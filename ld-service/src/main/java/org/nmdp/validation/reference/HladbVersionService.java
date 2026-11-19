package org.nmdp.validation.reference;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dash.valid.ars.AntigenRecognitionSiteLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The IMGT/HLA releases that can actually be used as an {@code hladbVersion}.
 *
 * NMDP's release list names every release back to 3.0.0, but the analysis needs the G-group file
 * (hla_ambigs.xml.zip) published for that release, and 18 of the listed ones don't have it. So the
 * usable set is the NMDP list intersected with "the file exists", checked once with cheap HEAD
 * requests and cached for a day (a new release can appear, an old one never changes).
 *
 * Only a definite "not found" marks a release unusable. Any other problem (timeout, server error)
 * fails the whole refresh instead, so a flaky network can't quietly shrink the list.
 */
@Service
public class HladbVersionService {

    public record Release(String version, String released) {
    }

    private static final Pattern LINE = Pattern.compile("^\\s*(\\d+\\.\\d+\\.\\d+)\\s*(\\S*)");
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration RETRY_AFTER_FAILURE = Duration.ofMinutes(5);
    private static final int CHECK_THREADS = 8;

    private final String releasesUrl;
    private final String fileUrlTemplate;
    private final Duration ttl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private List<Release> cached;
    private Instant cachedAt;

    // The real endpoints by default; overridable so tests can point at a local server. By default the
    // file URL comes from AntigenRecognitionSiteLoader#gGroupFileUrl, the same code the analysis uses
    // to download it, so this list can't drift from what the loader can actually load. A test
    // template takes the version with its periods removed (3.65.0 -> 3650).
    @Autowired
    public HladbVersionService(
            @Value("${ld.reference.releases-url:https://hml.nmdp.org/mac/api/imgtHlaReleases}") String releasesUrl,
            @Value("${ld.reference.file-url-template:}") String fileUrlTemplate) {
        this(releasesUrl, fileUrlTemplate == null || fileUrlTemplate.isBlank() ? null : fileUrlTemplate, DEFAULT_TTL);
    }

    HladbVersionService(String releasesUrl, String fileUrlTemplate, Duration ttl) {
        this.releasesUrl = releasesUrl;
        this.fileUrlTemplate = fileUrlTemplate;
        this.ttl = ttl;
    }

    /** Usable releases, newest first. Serves the previous list if a refresh fails and one exists. */
    public synchronized List<Release> getUsableReleases() throws IOException {
        if (cached != null && Instant.now().isBefore(cachedAt.plus(ttl))) {
            return cached;
        }

        try {
            cached = refresh();
            cachedAt = Instant.now();
        }
        catch (IOException | RuntimeException e) {
            if (cached == null) {
                throw e instanceof IOException io ? io : new IOException(e);
            }
            // Stale beats nothing: the list almost never changes. Back off before trying again, so an
            // outage doesn't turn every request into a full (slow) refresh attempt.
            cachedAt = Instant.now().minus(ttl).plus(RETRY_AFTER_FAILURE.compareTo(ttl) < 0 ? RETRY_AFTER_FAILURE : ttl);
        }

        return cached;
    }

    private List<Release> refresh() throws IOException {
        List<Release> listed = parse(get(releasesUrl));
        if (listed.isEmpty()) {
            throw new IOException("The release list was empty or in an unexpected format");
        }

        ExecutorService pool = Executors.newFixedThreadPool(CHECK_THREADS);
        try {
            List<Future<Boolean>> checks = new ArrayList<>();
            for (Release release : listed) {
                checks.add(pool.submit(() -> fileExists(release.version())));
            }

            List<Release> usable = new ArrayList<>();
            for (int i = 0; i < listed.size(); i++) {
                if (checks.get(i).get()) {
                    usable.add(listed.get(i));
                }
            }
            return usable;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking releases", e);
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IOException(cause.getMessage() != null ? cause.getMessage() : cause.toString(), e);
        }
        finally {
            pool.shutdownNow();
        }
    }

    static List<Release> parse(String text) {
        List<Release> releases = new ArrayList<>();
        for (String line : text.split("\\R")) {
            Matcher m = LINE.matcher(line);
            if (m.find()) {
                releases.add(new Release(m.group(1), m.group(2)));
            }
        }
        return releases;
    }

    private boolean fileExists(String version) throws IOException {
        String url = fileUrlTemplate == null
                ? AntigenRecognitionSiteLoader.gGroupFileUrl(version)
                : String.format(fileUrlTemplate, version.replace(".", ""));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(15))
                .build();
        try {
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 200) {
                return true;
            }
            if (status == 404) {
                return false;
            }
            throw new IOException("Unexpected HTTP " + status + " checking release " + version);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking release " + version, e);
        }
    }

    private String get(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " from " + url);
            }
            return response.body();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
    }
}
