/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.nmdp.validation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.dash.valid.freq.HLAFrequenciesLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.nmdp.validation.job.JobRegistry;

// ld-service's first tests (Phase 8a) -- until now this module had zero test coverage at all.
// Uses the same inline GL String fixture LinkageDisequilibriumAnalyzerTest#testLinkageReportingInlineGLString
// already exercises directly against ld-validation, driven this time through the real HTTP
// controller layer, since that's what's actually new/at-risk here.
//
// @Import(JobRegistry.class): @WebMvcTest only auto-registers web-layer beans; JobRegistry is a
// plain @Component the controller now depends on for /genotypes/file. Imported (not mocked) so
// the async job tests below exercise the real background-thread execution, not a stub.
@WebMvcTest(GenotypesApiController.class)
@Import(JobRegistry.class)
public class GenotypesApiControllerTest {

    private static final String INLINE_GL_STRING = "HLA-A*11:01:01+HLA-A*24:02:01:01/HLA-A*24:02:01:02L/HLA-A*24:02:01:03^HLA-B*18:01:01:01/HLA-B*18:01:01:02/HLA-B*18:51+HLA-B*53:01:01^HLA-C*04:01:01:01/HLA-C*04:01:01:02/HLA-C*04:01:01:03/HLA-C*04:01:01:04/HLA-C*04:01:01:05/HLA-C*04:20/HLA-C*04:117+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34^HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05+HLA-DPA1*02:01:01^HLA-DPB1*02:01:02+HLA-DPB1*09:01^HLA-DQA1*01:02:01:01/HLA-DQA1*01:02:01:02/HLA-DQA1*01:02:01:03/HLA-DQA1*01:02:01:04/HLA-DQA1*01:11+HLA-DQA1*03:01:01^HLA-DQB1*03:05:01+HLA-DQB1*06:09^HLA-DRB1*11:04:01+HLA-DRB1*13:02:01^HLA-DRB3*02:02:01:01/HLA-DRB3*02:02:01:02+HLA-DRB3*03:01:01";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Submits a multipart request to /genotypes/file, then polls GET /genotypes/jobs/{jobId}
    // (Phase 8's async job API) until it reaches a terminal phase, returning that final
    // JobStatus body. The fixtures this test class uses are tiny (a handful of genotypes
    // against a small reference file), so real completion happens well within this loop's
    // bound -- this is not simulating slowness, just accommodating the fact that the work now
    // genuinely happens on a separate thread instead of inline on the calling one.
    private JsonNode submitFileJobAndAwaitTerminal(MockMultipartFile... files) throws Exception {
        var requestBuilder = multipart("/genotypes/file");
        for (MockMultipartFile file : files) {
            requestBuilder = requestBuilder.file(file);
        }

        MvcResult submitResult = mockMvc.perform(requestBuilder)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.jobId").exists())
            .andReturn();
        String jobId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("jobId").asText();

        for (int i = 0; i < 200; i++) {
            MvcResult statusResult = mockMvc.perform(get("/genotypes/jobs/" + jobId)).andExpect(status().isOk()).andReturn();
            JsonNode status = objectMapper.readTree(statusResult.getResponse().getContentAsString());
            String phase = status.get("phase").asText();
            if (phase.equals("DONE") || phase.equals("FAILED")) {
                return status;
            }
            Thread.sleep(25);
        }

        fail("Job did not reach a terminal phase in time");
        return null;
    }

    // Phase 8a's first gap: the anomaly/warning findings used to be dropped entirely from the
    // JSON response -- only getLinkedPairs() ever made it out. Doesn't assert which way
    // hasAnomalies() lands for this genotype (that's ld-validation's own test suite's job);
    // asserts only that the fields actually reach the response now, which is the regression
    // this test exists to catch.
    @Test
    public void submitGenotypesIncludesAnomalyAndWarningFindings() throws Exception {
        String body = "{\"genotype\":[{\"id\":\"fullyQualified\",\"glString\":\"" + INLINE_GL_STRING + "\"}]}";

        mockMvc.perform(post("/genotypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sample", hasSize(1)))
            .andExpect(jsonPath("$.sample[0].id").value("fullyQualified"))
            .andExpect(jsonPath("$.sample[0].hasAnomalies").exists())
            .andExpect(jsonPath("$.sample[0].warnings").exists())
            .andExpect(jsonPath("$.sample[0].linkageReport").exists())
            .andExpect(jsonPath("$.sample[0].haplotypePairReport").exists())
            .andExpect(jsonPath("$.sample[0].detectedFindingsReport").exists());
    }

    // Phase 8a's second gap: hladbVersion/frequencySet were never exposed via the API at all
    // (System properties set once at CLI startup, invisible to REST callers). Confirms the
    // request-level values actually reach the detection engine by checking for their literal
    // echo in the report text (DetectedLinkageFindings embeds both in every formatted report).
    // Uses "nmdp-2007" (a real bundled dataset), not the plain "nmdp" enum value -- that one
    // has no bundled reference file at all and throws FileNotFoundException on first use in a
    // process (a separate, pre-existing bug found while scoping this change, left unfixed here;
    // see HLAFrequenciesLoader#getInstance()).
    @Test
    public void submitGenotypesAppliesPerRequestHladbAndFrequencySet() throws Exception {
        String body = "{\"genotype\":[{\"id\":\"fullyQualified\",\"glString\":\"" + INLINE_GL_STRING
                + "\"}],\"hladbVersion\":\"3.19.0\",\"frequencySet\":\"nmdp-2007\"}";

        mockMvc.perform(post("/genotypes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sample[0].linkageReport", containsString("HLA DB Version: 3.19.0")))
            .andExpect(jsonPath("$.sample[0].linkageReport", containsString("Frequencies:  nmdp-2007")));
    }

    // Regression test for a real perf bug found while measuring actual large-frequency-file
    // costs against this project's own converted NMDP data (some run 8+ minutes just to parse,
    // before any genotype analysis even starts): the first version of the per-request
    // frequencySet fix called HLAFrequenciesLoader.reset() unconditionally on every single
    // request, discarding the already-parsed data even when nothing had changed. Confirms two
    // requests with the same frequencySet reuse the same HLAFrequenciesLoader instance (no
    // reparse), and that a genuine change to a different frequencySet does get a fresh one.
    @Test
    public void submitGenotypesOnlyReloadsFrequencyDataWhenFrequencySetActuallyChanges() throws Exception {
        String bodyNmdp2007 = "{\"genotype\":[{\"id\":\"s\",\"glString\":\"" + INLINE_GL_STRING
                + "\"}],\"frequencySet\":\"nmdp-2007\"}";
        String bodyNmdp2007Std = "{\"genotype\":[{\"id\":\"s\",\"glString\":\"" + INLINE_GL_STRING
                + "\"}],\"frequencySet\":\"nmdp-2007-std\"}";

        mockMvc.perform(post("/genotypes").contentType(MediaType.APPLICATION_JSON).content(bodyNmdp2007))
            .andExpect(status().isOk());
        HLAFrequenciesLoader afterFirst = HLAFrequenciesLoader.getInstance();

        mockMvc.perform(post("/genotypes").contentType(MediaType.APPLICATION_JSON).content(bodyNmdp2007))
            .andExpect(status().isOk());
        HLAFrequenciesLoader afterRepeat = HLAFrequenciesLoader.getInstance();
        assertSame(afterFirst, afterRepeat, "Same frequencySet on a repeat request should reuse the already-parsed data, not reparse it");

        mockMvc.perform(post("/genotypes").contentType(MediaType.APPLICATION_JSON).content(bodyNmdp2007Std))
            .andExpect(status().isOk());
        HLAFrequenciesLoader afterChange = HLAFrequenciesLoader.getInstance();
        assertNotSame(afterRepeat, afterChange, "A genuinely different frequencySet should still get freshly loaded data");
    }

    // Phase 8a's third gap: the API previously had no way to submit a batch file at all, unlike
    // analyze-gl-strings' actual primary use case. Reuses the same tab-delimited
    // "id<TAB>glString"-per-line format the CLI reads (confirmed against
    // ld-validation/src/test/resources/syntheticExamples.txt).
    //
    // Phase 8 (async): this endpoint now returns 202+jobId immediately rather than the result
    // directly -- see submitFileJobAndAwaitTerminal.
    @Test
    public void submitGenotypesFileParsesTabDelimitedBatchFile() throws Exception {
        String fileContent = "sample-1\t" + INLINE_GL_STRING + "\n"
                + "sample-2\t" + INLINE_GL_STRING + "\n";
        MockMultipartFile file = new MockMultipartFile("file", "batch.txt", "text/plain",
                fileContent.getBytes(StandardCharsets.UTF_8));

        JsonNode finalStatus = submitFileJobAndAwaitTerminal(file);

        assertEquals("DONE", finalStatus.get("phase").asText(), finalStatus.toString());
        assertEquals(2, finalStatus.get("result").get("sample").size());
        assertEquals("sample-1", finalStatus.get("result").get("sample").get(0).get("id").asText());
        assertEquals("sample-2", finalStatus.get("result").get("sample").get(1).get("id").asText());
    }

    // The actual point of Phase 8: let the end user upload their own frequency reference data
    // (NMDP's or anyone else's, once converted to the "standard format" normalize-frequency-file
    // produces) instead of being limited to the handful of bundled named sets. Confirms the
    // upload genuinely reaches the detection engine -- not just that the request is accepted --
    // by checking the "Inputted" sentinel AnalyzeGLStrings' CLI itself uses for this same case
    // shows up in the report text.
    @Test
    public void submitGenotypesFileAcceptsCustomFrequencyFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "batch.txt", "text/plain",
                ("sample-1\t" + INLINE_GL_STRING + "\n").getBytes(StandardCharsets.UTF_8));
        MockMultipartFile freqFile = new MockMultipartFile("frequencyFiles", "miniFiveLocusFreqs.csv", "text/csv",
                getClass().getClassLoader().getResourceAsStream("miniFiveLocusFreqs.csv").readAllBytes());

        JsonNode finalStatus = submitFileJobAndAwaitTerminal(file, freqFile);

        assertEquals("DONE", finalStatus.get("phase").asText(), finalStatus.toString());
        String linkageReport = finalStatus.get("result").get("sample").get(0).get("linkageReport").asText();
        assertTrue(linkageReport.contains("Frequencies:  Inputted"), linkageReport);
    }

    // Phase 8: HLAFrequenciesLoader used to System.exit(-1) the entire process on a bad
    // frequency file (fine for the CLI, catastrophic for a long-running service -- see
    // HLAFrequenciesLoader#init(Set,File)). This particular garbage upload is obviously wrong
    // (no commas at all), so it's still caught by the cheap synchronous pre-check and rejected
    // with an immediate 400 -- it never becomes a job at all. See
    // GenotypesApiController#validateFrequencyFileLooksReasonable for what that check does and
    // doesn't catch.
    @Test
    public void submitGenotypesFileRejectsObviouslyMalformedFrequencyFileSynchronously() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "batch.txt", "text/plain",
                ("sample-1\t" + INLINE_GL_STRING + "\n").getBytes(StandardCharsets.UTF_8));
        MockMultipartFile badFreqFile = new MockMultipartFile("frequencyFiles", "garbage.csv", "text/csv",
                "this is not a valid frequency file at all\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/genotypes/file").file(file).file(badFreqFile))
            .andExpect(status().isBadRequest());
    }

    // A file that passes the cheap synchronous pre-check (right shape for its first few lines)
    // but is malformed further in only fails once the background job actually gets to that
    // line -- surfacing as a FAILED job, not a 400. This is the real, documented tradeoff of
    // making this endpoint async (see JobStatus's FAILED phase description in the spec).
    @Test
    public void submitGenotypesFileFailsAsJobWhenFrequencyFileIsMalformedPastThePreCheck() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "batch.txt", "text/plain",
                ("sample-1\t" + INLINE_GL_STRING + "\n").getBytes(StandardCharsets.UTF_8));
        String badContentPastPreCheck = "HIS,HLA-A*01:01g~HLA-C*01:02~HLA-B*08:01g~HLA-DRB1*04:02~HLA-DQB1*03:02,1.2E-4\n".repeat(5)
                + "this line is fine for the first 5 rows the pre-check reads, but not this one\n";
        MockMultipartFile badFreqFile = new MockMultipartFile("frequencyFiles", "sneaky.csv", "text/csv",
                badContentPastPreCheck.getBytes(StandardCharsets.UTF_8));

        JsonNode finalStatus = submitFileJobAndAwaitTerminal(file, badFreqFile);

        assertEquals("FAILED", finalStatus.get("phase").asText(), finalStatus.toString());
        assertNotNull(finalStatus.get("error").asText());
    }

    // GET /genotypes/jobs/{jobId} for an id that never existed (or was evicted).
    @Test
    public void getGenotypesJobReturns404ForUnknownJobId() throws Exception {
        mockMvc.perform(get("/genotypes/jobs/does-not-exist"))
            .andExpect(status().isNotFound());
    }
}
