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
package org.nmdp.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

// Regression test for a real bug found via manual testing (not caught by
// GenotypesApiControllerTest's @WebMvcTest suite): MultipartFile's content is only guaranteed
// to exist for the HTTP request's lifetime -- the servlet container (Tomcat) deletes its
// backing temp file once the request completes, which for this async endpoint happens almost
// immediately (it returns 202 right away, well before the background job -- on a separate
// thread -- gets around to reading the upload). @WebMvcTest's MockMvc never goes through a real
// embedded servlet container, so it can't exercise this lifecycle at all; MockMultipartFile
// also holds its content in memory indefinitely regardless. Only a genuine @SpringBootTest with
// a real embedded Tomcat, driven over real HTTP, can catch this -- which is exactly what this
// class is for.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GenotypesFileJobIntegrationTest {

    private static final String INLINE_GL_STRING = "HLA-A*11:01:01+HLA-A*24:02:01:01/HLA-A*24:02:01:02L/HLA-A*24:02:01:03^HLA-B*18:01:01:01/HLA-B*18:01:01:02/HLA-B*18:51+HLA-B*53:01:01^HLA-C*04:01:01:01/HLA-C*04:01:01:02/HLA-C*04:01:01:03/HLA-C*04:01:01:04/HLA-C*04:01:01:05/HLA-C*04:20/HLA-C*04:117+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34^HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05+HLA-DPA1*02:01:01^HLA-DPB1*02:01:02+HLA-DPB1*09:01^HLA-DQA1*01:02:01:01/HLA-DQA1*01:02:01:02/HLA-DQA1*01:02:01:03/HLA-DQA1*01:02:01:04/HLA-DQA1*01:11+HLA-DQA1*03:01:01^HLA-DQB1*03:05:01+HLA-DQB1*06:09^HLA-DRB1*11:04:01+HLA-DRB1*13:02:01^HLA-DRB3*02:02:01:01/HLA-DRB3*02:02:01:02+HLA-DRB3*03:01:01";

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private File uploadFile;

    @AfterEach
    void cleanup() {
        if (uploadFile != null) {
            uploadFile.delete();
        }
    }

    @Test
    public void submitGenotypesFileSurvivesRealServletContainerMultipartCleanup() throws Exception {
        uploadFile = Files.createTempFile("genotypes-integration-test-", ".txt").toFile();
        Files.writeString(uploadFile.toPath(), "sample-1\t" + INLINE_GL_STRING + "\n", StandardCharsets.UTF_8);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(uploadFile));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> submitResponse = restTemplate.postForEntity(
                url("/genotypes/file"), new HttpEntity<>(body, headers), Map.class);
        assertEquals(HttpStatus.ACCEPTED, submitResponse.getStatusCode());
        String jobId = (String) submitResponse.getBody().get("jobId");
        assertNotNull(jobId);

        Map<String, Object> finalStatus = null;
        for (int i = 0; i < 200; i++) {
            ResponseEntity<Map> statusResponse = restTemplate.getForEntity(url("/genotypes/jobs/" + jobId), Map.class);
            String phase = (String) statusResponse.getBody().get("phase");
            if ("DONE".equals(phase) || "FAILED".equals(phase)) {
                finalStatus = statusResponse.getBody();
                break;
            }
            Thread.sleep(25);
        }

        assertNotNull(finalStatus, "Job did not reach a terminal phase in time");
        assertEquals("DONE", finalStatus.get("phase"), "Job failed: " + finalStatus.get("error"));
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
