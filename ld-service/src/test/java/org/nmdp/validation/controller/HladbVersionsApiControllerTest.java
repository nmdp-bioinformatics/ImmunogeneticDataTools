package org.nmdp.validation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nmdp.validation.reference.HladbVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HladbVersionsApiController.class)
public class HladbVersionsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HladbVersionService service;

    @Test
    public void returnsTheUsableReleasesNewestFirst() throws Exception {
        when(service.getUsableReleases()).thenReturn(List.of(
                new HladbVersionService.Release("3.65.0", "2026-07"),
                new HladbVersionService.Release("3.20.0", "")));

        mockMvc.perform(get("/hladb-versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions[0].version").value("3.65.0"))
                .andExpect(jsonPath("$.versions[0].released").value("2026-07"))
                .andExpect(jsonPath("$.versions[1].version").value("3.20.0"))
                .andExpect(jsonPath("$.versions[1].released").doesNotExist());
    }

    @Test
    public void reportsBadGatewayWhenTheListCannotBeRetrieved() throws Exception {
        when(service.getUsableReleases()).thenThrow(new IOException("offline"));

        mockMvc.perform(get("/hladb-versions")).andExpect(status().isBadGateway());
    }

    @Test
    public void blankMissingAndAnyCaseLatestAllMeanLatest() {
        assertEquals("Latest", GenotypesApiController.resolveHladb(null));
        assertEquals("Latest", GenotypesApiController.resolveHladb(""));
        assertEquals("Latest", GenotypesApiController.resolveHladb("  "));
        assertEquals("Latest", GenotypesApiController.resolveHladb("latest"));
        assertEquals("Latest", GenotypesApiController.resolveHladb(" LATEST "));
        assertEquals("3.20.0", GenotypesApiController.resolveHladb(" 3.20.0 "));
    }
}
