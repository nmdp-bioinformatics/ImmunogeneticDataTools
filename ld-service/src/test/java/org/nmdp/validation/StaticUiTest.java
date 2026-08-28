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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }
}
