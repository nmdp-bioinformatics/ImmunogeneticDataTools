package org.nmdp.validation.controller;

import java.io.IOException;

import org.nmdp.validation.reference.HladbVersionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.api.HladbVersionsApi;
import io.swagger.model.HladbVersion;
import io.swagger.model.HladbVersions;

@Controller
public class HladbVersionsApiController implements HladbVersionsApi {

    private final HladbVersionService hladbVersionService;

    public HladbVersionsApiController(HladbVersionService hladbVersionService) {
        this.hladbVersionService = hladbVersionService;
    }

    @Override
    public ResponseEntity<HladbVersions> getHladbVersions() {
        try {
            HladbVersions body = new HladbVersions();
            for (HladbVersionService.Release release : hladbVersionService.getUsableReleases()) {
                HladbVersion version = new HladbVersion();
                version.setVersion(release.version());
                version.setReleased(release.released().isEmpty() ? null : release.released());
                body.addVersionsItem(version);
            }
            return ResponseEntity.ok(body);
        }
        catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not retrieve the list of IMGT/HLA releases: " + e.getMessage(), e);
        }
    }
}
