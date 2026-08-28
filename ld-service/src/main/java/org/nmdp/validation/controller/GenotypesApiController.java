package org.nmdp.validation.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Sample;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.race.RelativeFrequencyByRace;
import org.dash.valid.report.CommonWellDocumentedWriter;
import org.dash.valid.report.DetectedFindingsWriter;
import org.dash.valid.report.DetectedLinkageFindings;
import org.dash.valid.report.HaplotypePairWriter;
import org.dash.valid.report.LinkageDisequilibriumWriter;
import org.nmdp.validation.job.Job;
import org.nmdp.validation.job.JobRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.api.GenotypesApi;
import io.swagger.model.FindingData;
import io.swagger.model.Genotype;
import io.swagger.model.Genotypes;
import io.swagger.model.HaplotypePairData;
import io.swagger.model.JobReference;
import io.swagger.model.JobStatus;
import io.swagger.model.SampleData;
import io.swagger.model.Samples;

@Controller
public class GenotypesApiController implements GenotypesApi {

    // ld-validation's hladb/frequency-set selection is process-wide (System properties plus
    // HLAFrequenciesLoader/AntigenRecognitionSiteLoader's own cached singletons), not threaded
    // through per call. Phase 8 added per-request configurability to this API; without
    // serializing requests, two concurrent submissions asking for different hladb/frequency
    // values (or one named-set request racing one custom-frequency-file request) could each get
    // a mix of the other's config. Now that /genotypes/file's actual work runs on JobRegistry's
    // background thread rather than the calling HTTP thread, this lock is what's actually
    // serializing analysis with /genotypes' own (still-synchronous, still HTTP-thread) work --
    // without it those two could now genuinely run concurrently. ld-service is scoped as a
    // personal/local tool (Phase 8 audience decision), so serializing analysis -- one request's
    // worth of config-and-analyze at a time -- is an honest, low-risk fix; it is not a general
    // concurrent-request service.
    private static final Object ANALYSIS_LOCK = new Object();

    // Which named frequencySet is currently loaded into HLAFrequenciesLoader, or CUSTOM_FILES if
    // the last request used frequencyFiles instead. Measured against real reference data (Phase
    // 8): parsing a single ~90MB/927K-row standard-format frequency file alone -- before any
    // genotype analysis even starts -- takes minutes, and this project's own converted NMDP
    // datasets run well past that (some multi-locus combinations are 700MB-1GB+). Resetting
    // HLAFrequenciesLoader on every request regardless of whether the config actually changed
    // (the first version of this fix) would force that cost on every single request, even
    // repeated ones with identical config -- a real, self-inflicted regression a long-running
    // service should not have, since the CLI never pays this cost more than once per invocation
    // anyway. Only reset when the requested named set actually differs from what's already
    // loaded; this state lives here, not inside HLAFrequenciesLoader itself, specifically so it
    // doesn't reintroduce the automatic-reload-on-change behavior that was tried and reverted
    // there (see HLAFrequenciesLoader#getInstance()).
    private static final String CUSTOM_FILES = " CUSTOM_FILES ";
    private static String currentFrequencySet;

    private final JobRegistry jobRegistry;

    public GenotypesApiController(JobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

    @Override
    public ResponseEntity<Samples> submitGenotypes(@Parameter(description = "Genotypes", required = true) @Valid @RequestBody Genotypes genotypes) {
        synchronized (ANALYSIS_LOCK) {
            configure(genotypes.getHladbVersion(), genotypes.getFrequencySet());

            Samples samples = new Samples();
            for (Genotype genotype : genotypes.getGenotype()) {
                LinkageDisequilibriumGenotypeList linkedGLString = GLStringUtilities.inflateGenotypeList(genotype.getId(), genotype.getGlString(), null);
                Sample sample = LinkageDisequilibriumAnalyzer.detectLinkages(linkedGLString);
                samples.addSampleItem(populateSwaggerObject(sample));
            }

            return ResponseEntity.ok(samples);
        }
    }

    // Phase 8 (async): returns immediately with a jobId instead of blocking for the result --
    // real frequency reference data can take minutes just to parse, well past any reasonable
    // HTTP timeout, and the CLI's own per-genotype progress logging never reached an HTTP caller
    // at all. The actual work (configure + parse + analyze) runs on JobRegistry's single
    // background thread; poll GET /genotypes/jobs/{jobId} for progress and the eventual result.
    //
    // frequencyFiles get a cheap, synchronous sanity check here, before the job is even created,
    // specifically so an obviously-wrong upload (wrong file entirely, not the standard format)
    // still gets an immediate 400 instead of only surfacing as a FAILED job a few seconds later.
    // It only inspects the first few lines, not the whole file -- real errors deeper in a large
    // file still only surface as a FAILED job; catching everything synchronously would mean
    // fully parsing the file before responding, defeating the entire point of this endpoint.
    //
    // Every MultipartFile is copied to a temp file WE own, synchronously, right here, before
    // returning -- found the hard way (a real submission failing with NoSuchFileException deep
    // in the background job): MultipartFile's content is only guaranteed to exist for this HTTP
    // request's lifetime. The servlet container (Tomcat) deletes its backing temp file once the
    // request completes, which now happens almost immediately since we just return 202 -- well
    // before the background job, on a separate thread, ever gets to read it. The unit tests
    // never caught this because MockMultipartFile holds its content in memory indefinitely,
    // immune to that real request-scoped cleanup.
    @Override
    public ResponseEntity<JobReference> submitGenotypesFile(MultipartFile file, String hladbVersion, String frequencySet,
            List<MultipartFile> frequencyFiles, MultipartFile allelesFile) {
        if (frequencyFiles != null) {
            for (MultipartFile frequencyFile : frequencyFiles) {
                validateFrequencyFileLooksReasonable(frequencyFile);
            }
        }

        List<File> ownedTempFiles = new ArrayList<>();
        File ownedInputFile = toTempFile(file, ownedTempFiles);
        String originalFilename = file.getOriginalFilename();

        Set<File> ownedFrequencyFiles = null;
        File ownedAllelesFile = null;
        if (frequencyFiles != null && !frequencyFiles.isEmpty()) {
            ownedFrequencyFiles = new HashSet<>();
            for (MultipartFile frequencyFile : frequencyFiles) {
                ownedFrequencyFiles.add(toTempFile(frequencyFile, ownedTempFiles));
            }
            if (allelesFile != null && !allelesFile.isEmpty()) {
                ownedAllelesFile = toTempFile(allelesFile, ownedTempFiles);
            }
        }

        Set<File> jobFrequencyFiles = ownedFrequencyFiles;
        File jobAllelesFile = ownedAllelesFile;
        Job job = jobRegistry.submit(j -> {
            try {
                return runGenotypesFileJob(j, ownedInputFile, originalFilename, hladbVersion, frequencySet, jobFrequencyFiles, jobAllelesFile);
            }
            finally {
                for (File tempFile : ownedTempFiles) {
                    tempFile.delete();
                }
            }
        });

        JobReference reference = new JobReference();
        reference.setJobId(job.getId());
        return ResponseEntity.accepted().body(reference);
    }

    @Override
    public ResponseEntity<JobStatus> getGenotypesJob(String jobId) {
        Job job = jobRegistry.get(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No job with id " + jobId);
        }

        return ResponseEntity.ok(toJobStatus(job));
    }

    // Runs on JobRegistry's background worker thread, not the HTTP thread that accepted the
    // upload -- everything it touches (inputFile, frequencyFiles, allelesFile) is a temp file
    // submitGenotypesFile already copied the original uploads into, synchronously, before this
    // ever runs (see the comment there for why that copy is necessary). Held under the same
    // ANALYSIS_LOCK submitGenotypes uses, since both paths mutate the same process-wide
    // hladb/frequency-set configuration.
    private List<Sample> runGenotypesFileJob(Job job, File inputFile, String originalFilename, String hladbVersion, String frequencySet,
            Set<File> frequencyFiles, File allelesFile) throws IOException {
        synchronized (ANALYSIS_LOCK) {
            job.setPhase(Job.Phase.LOADING_REFERENCE_DATA);
            configureWithOptionalCustomFrequencies(hladbVersion, frequencySet, frequencyFiles, allelesFile);

            job.setPhase(Job.Phase.ANALYZING_GENOTYPES);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
                String name = originalFilename != null ? originalFilename : "upload";
                // Reuses the exact same parsing + analysis path analyze-gl-strings' CLI uses for
                // its own batch input files, rather than reimplementing the tab-delimited
                // "id<TAB>glString" format here.
                return LinkageDisequilibriumAnalyzer.analyzeGLStringFile(name, reader, job::updateProgress);
            }
        }
    }

    private JobStatus toJobStatus(Job job) {
        JobStatus status = new JobStatus();
        status.setJobId(job.getId());
        status.setPhase(JobStatus.PhaseEnum.valueOf(job.getPhase().name()));

        if (job.getPhase() == Job.Phase.ANALYZING_GENOTYPES) {
            status.setProcessed(job.getProcessed());
            status.setTotal(job.getTotal());
            status.setPercent(job.getTotal() > 0 ? (job.getProcessed() * 100) / job.getTotal() : 0);
        }

        if (job.getPhase() == Job.Phase.DONE) {
            Samples samples = new Samples();
            for (Sample sample : job.getResult()) {
                samples.addSampleItem(populateSwaggerObject(sample));
            }
            status.setResult(samples);
        }

        if (job.getPhase() == Job.Phase.FAILED) {
            status.setError(job.getError());
        }

        return status;
    }

    // Best-effort, not a real parse: just enough to catch "this is the wrong file entirely"
    // before committing to a background job for it. Checks the first few non-blank lines split
    // into the standard format's minimum 3 comma-separated columns (race,haplotype,frequency)
    // with a numeric frequency column -- matching what
    // HLAFrequenciesLoader#loadStandardReferenceData actually expects. A file that passes this
    // but has a malformed line further in still only fails later, as a FAILED job.
    private void validateFrequencyFileLooksReasonable(MultipartFile frequencyFile) {
        String name = frequencyFile.getOriginalFilename() != null ? frequencyFile.getOriginalFilename() : "upload";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(frequencyFile.getInputStream(), StandardCharsets.UTF_8))) {
            int checked = 0;
            String line;
            while (checked < 5 && (line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(GLStringConstants.COMMA);
                if (columns.length < 3) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "frequencyFiles upload '" + name + "' doesn't look like the standard format "
                                    + "(expected at least 3 comma-separated columns: race,haplotype,frequency) at line " + (checked + 1));
                }

                try {
                    Double.parseDouble(columns[2]);
                }
                catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "frequencyFiles upload '" + name + "' doesn't look like the standard format "
                                    + "(expected a numeric frequency in the third column) at line " + (checked + 1));
                }

                checked++;
            }
        }
        catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Couldn't read frequencyFiles upload '" + name + "'", e);
        }
    }

    // Sets the same System properties AnalyzeGLStrings' CLI sets from its -v/-f flags, so
    // per-request selection here reaches the detection engine the identical way the CLI's
    // already does. hladbVersion genuinely takes effect per request (AntigenRecognitionSiteLoader
    // and CommonWellDocumentedLoader both reload when it changes, comparing internally). Must be
    // called from inside the ANALYSIS_LOCK critical section.
    private void configure(String hladbVersion, String frequencySet) {
        System.setProperty(GLStringConstants.HLADB_PROPERTY, hladbVersion != null ? hladbVersion : GLStringConstants.LATEST_HLADB);
        configureNamedFrequencySet(frequencySet);
    }

    // frequencySet's own reload-on-change comparison, done here rather than inside
    // HLAFrequenciesLoader (see CUSTOM_FILES/currentFrequencySet above for why): only calls
    // reset() when the resolved value actually differs from what's already loaded, so repeated
    // requests against an unchanged (possibly very large) frequency set reuse the already-parsed
    // data instead of re-parsing it from scratch every single time.
    private void configureNamedFrequencySet(String frequencySet) {
        String resolvedFrequencySet = frequencySet != null ? frequencySet : Frequencies.NMDP_2007_STD.getShortName();
        System.setProperty(Frequencies.FREQUENCIES_PROPERTY, resolvedFrequencySet);

        if (!resolvedFrequencySet.equals(currentFrequencySet)) {
            HLAFrequenciesLoader.reset();
            currentFrequencySet = resolvedFrequencySet;
        }
    }

    // The /genotypes/file variant: when the caller uploads their own frequencyFiles (any
    // source, in the "standard format" normalize-frequency-file produces -- the same files the
    // CLI's -q/-l flags take), those take precedence over frequencySet, mirroring
    // AnalyzeGLStrings' own precedence. frequencyFiles/allelesFile are already temp files
    // submitGenotypesFile owns (copied from the original uploads before the job was submitted --
    // see there for why); this method doesn't create or clean up any files itself.
    //
    // Runs inside a background job (see runGenotypesFileJob), so a malformed upload that slips
    // past validateFrequencyFileLooksReasonable's cheap check just propagates as a plain
    // exception -- JobRegistry's own catch-all records it as the job's error, there's no HTTP
    // response to shape here anymore. (HLAFrequenciesLoader itself used to System.exit(-1) on a
    // bad file instead of throwing at all -- fixed as part of Phase 8, since that would have
    // taken the whole service down over one bad request regardless of sync vs async.)
    private void configureWithOptionalCustomFrequencies(String hladbVersion, String frequencySet,
            Set<File> frequencyFiles, File allelesFile) {
        System.setProperty(GLStringConstants.HLADB_PROPERTY, hladbVersion != null ? hladbVersion : GLStringConstants.LATEST_HLADB);

        if (frequencyFiles == null || frequencyFiles.isEmpty()) {
            configureNamedFrequencySet(frequencySet);
            return;
        }

        System.setProperty(Frequencies.FREQUENCIES_PROPERTY, frequencySet != null ? frequencySet : "Inputted");

        // Always reconstructs unconditionally regardless of what's currently cached -- unlike
        // the named-set path, there's no cheap way to tell whether a fresh upload is "the same
        // as last time" without hashing its content, and every genuine upload legitimately
        // needs to load, so no reset()-avoidance is possible or attempted here.
        HLAFrequenciesLoader.getInstance(frequencyFiles, allelesFile);
        // A subsequent named-set request must not think its target set is already loaded just
        // because it once was, before this custom-file request replaced it.
        currentFrequencySet = CUSTOM_FILES;
    }

    private File toTempFile(MultipartFile multipartFile, List<File> tempFiles) {
        try {
            File tempFile = File.createTempFile("ld-service-freq-", "-" + safeName(multipartFile.getOriginalFilename()));
            tempFiles.add(tempFile);
            multipartFile.transferTo(tempFile);
            return tempFile;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String safeName(String originalFilename) {
        return originalFilename != null ? originalFilename.replaceAll("[^A-Za-z0-9._-]", "_") : "upload";
    }

    public SampleData populateSwaggerObject(Sample sample) {
        SampleData sampleData = new SampleData();
        sampleData.setId(sample.getId());
        sampleData.setGlString(sample.getGlString());
        sampleData.setProcessedGlString(sample.getProcessedGlString());

        DetectedLinkageFindings findings = sample.getFindings();

        for (HaplotypePair pair : findings.getLinkedPairs()) {
            HaplotypePairData pairData = new HaplotypePairData();
            pairData.setHaplotype1(pair.getHaplotypes().get(0).getHaplotypeString());
            pairData.setHaplotype2(pair.getHaplotypes().get(1).getHaplotypeString());
            for (RelativeFrequencyByRace freqByRace : pair.getFrequencies()) {
                FindingData finding = new FindingData();
                finding.setRace(freqByRace.getRace());
                finding.setFrequency(new BigDecimal(freqByRace.getFrequency()));
                finding.setRelativeFrequency(new BigDecimal(freqByRace.getRelativeFrequency()));
                finding.setHaplotype1Frequency(new BigDecimal(freqByRace.getHap1Frequency()));
                finding.setHaplotype2Frequency(new BigDecimal(freqByRace.getHap2Frequency()));
                pairData.addFindingItem(finding);
            }
            sampleData.addHaplotypePairItem(pairData);
        }

        // Phase 8: the anomaly/warning findings below used to be dropped entirely here -- only
        // getLinkedPairs() (the clean, no-anomaly case) ever made it into the response, despite
        // the module README claiming this endpoint "returns the same detected-linkage findings
        // analyze-gl-strings would produce." Reuses the exact same formatter classes the CLI
        // writes its *.log/*.csv report files with, so the text is identical, not a
        // re-derived approximation.
        sampleData.setHasAnomalies(findings.hasAnomalies());
        sampleData.setWarnings(findings.getWarnings());
        sampleData.setLinkageReport(LinkageDisequilibriumWriter.formatDetectedLinkages(findings));
        sampleData.setHaplotypePairReport(HaplotypePairWriter.formatDetectedLinkages(findings));
        sampleData.setNonCommonWellDocumentedReport(CommonWellDocumentedWriter.formatCommonWellDocumented(findings));
        sampleData.setDetectedFindingsReport(DetectedFindingsWriter.formatDetectedFindings(findings));

        return sampleData;
    }
}
