package org.nmdp.validation.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.Valid;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Sample;
import org.dash.valid.freq.Frequencies;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.api.GenotypesApi;
import io.swagger.model.FindingData;
import io.swagger.model.Genotype;
import io.swagger.model.Genotypes;
import io.swagger.model.HaplotypePairData;
import io.swagger.model.SampleData;
import io.swagger.model.Samples;

@Controller
public class GenotypesApiController implements GenotypesApi {

    // ld-validation's hladb/frequency-set selection is process-wide (System properties, read
    // deep inside the detection engine and its reference-data loaders -- see
    // AntigenRecognitionSiteLoader/HLAFrequenciesLoader), not threaded through per call. Phase 8
    // added per-request configurability to this API; without serializing requests, two
    // concurrent submissions asking for different hladb/frequency values could race and each
    // get a mix of the other's config. ld-service is scoped as a personal/local tool (Phase 8
    // audience decision), so serializing analysis -- one request's worth of config-and-analyze
    // at a time -- is an honest, low-risk fix; it is not a general concurrent-request service.
    private static final Object ANALYSIS_LOCK = new Object();

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

    @Override
    public ResponseEntity<Samples> submitGenotypesFile(MultipartFile file, String hladbVersion, String frequencySet) {
        synchronized (ANALYSIS_LOCK) {
            configure(hladbVersion, frequencySet);

            List<Sample> sampleList;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
                // Reuses the exact same parsing + analysis path analyze-gl-strings' CLI uses for
                // its own batch input files, rather than reimplementing the tab-delimited
                // "id<TAB>glString" format here.
                sampleList = LinkageDisequilibriumAnalyzer.analyzeGLStringFile(name, reader);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            Samples samples = new Samples();
            for (Sample sample : sampleList) {
                samples.addSampleItem(populateSwaggerObject(sample));
            }

            return ResponseEntity.ok(samples);
        }
    }

    // Sets the same System properties AnalyzeGLStrings' CLI sets from its -v/-f flags, so
    // per-request selection here reaches the detection engine the identical way the CLI's
    // already does. hladbVersion genuinely takes effect per request (AntigenRecognitionSiteLoader
    // and CommonWellDocumentedLoader both reload when it changes); frequencySet only reliably
    // takes effect on the first analysis this process performs -- HLAFrequenciesLoader does not
    // reload on change, deliberately left as-is (see its own getInstance() for why). Must be
    // called from inside the ANALYSIS_LOCK critical section.
    private void configure(String hladbVersion, String frequencySet) {
        System.setProperty(GLStringConstants.HLADB_PROPERTY, hladbVersion != null ? hladbVersion : GLStringConstants.LATEST_HLADB);
        System.setProperty(Frequencies.FREQUENCIES_PROPERTY, frequencySet != null ? frequencySet : Frequencies.NMDP_2007_STD.getShortName());
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
