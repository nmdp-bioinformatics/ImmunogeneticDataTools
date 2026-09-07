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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

// Phase 8b: the UI is static content (src/main/resources/static) served by Spring Boot's
// default resource handling, not anything GenotypesApiController is involved in -- there's no
// controller code to unit test here, just "did the build actually wire this up correctly."
// A real embedded server, not @WebMvcTest, since that's what actually exercises Spring Boot's
// static resource handler.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StaticUiTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void indexPageIsServedAtRoot() {
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("HLAHapV"), response.getBody());
    }

    @Test
    public void cssAndJsAssetsAreServed() {
        ResponseEntity<String> css = restTemplate.getForEntity("http://localhost:" + port + "/css/app.css", String.class);
        assertEquals(HttpStatus.OK, css.getStatusCode());

        ResponseEntity<String> js = restTemplate.getForEntity("http://localhost:" + port + "/js/app.js", String.class);
        assertEquals(HttpStatus.OK, js.getStatusCode());
        assertTrue(js.getBody().contains("/genotypes/file"), "app.js should reference the async job endpoint it actually calls");
        assertTrue(js.getBody().contains("/actuator/info"), "app.js should reference the version endpoint it reads for the footer");
    }

    // Issue #89, applied to the UI: real end-to-end coverage of the wiring, not just "the
    // dependency is on the classpath" -- exercises the actual build-info Maven goal's output
    // (target/classes/META-INF/build-info.properties) flowing through Actuator's real
    // BuildInfoContributor and web exposure config, exactly what the browser footer depends on.
    @Test
    public void actuatorInfoEndpointReportsTheBuildVersion() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/actuator/info", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode build = new ObjectMapper().readTree(response.getBody()).path("build");
        assertEquals("ld-service", build.path("name").asText());
        assertFalse(build.path("version").asText().isEmpty(), "build.version should be populated, not just present");
    }
}
