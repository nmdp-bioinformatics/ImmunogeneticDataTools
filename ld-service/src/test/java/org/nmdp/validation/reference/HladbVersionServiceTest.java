package org.nmdp.validation.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

// Runs the service against a local HTTP server standing in for NMDP's release list and GitHub, so
// the usable-set logic (listed AND file exists) is tested deterministically, offline.
public class HladbVersionServiceTest {

    private static final String RELEASES = "3.65.0 2026-07\n3.20.0 2015-04\n3.17.0 2014-07\n3.2.0 2010-10\n";

    private HttpServer server;
    private String releasesBody;
    private int releasesStatus;
    // file tag (version without periods) -> HTTP status to answer HEAD with
    private final Map<String, Integer> fileStatus = new ConcurrentHashMap<>();
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        releasesBody = RELEASES;
        releasesStatus = 200;
        fileStatus.put("3650", 200);
        fileStatus.put("3200", 200);
        fileStatus.put("3170", 404);
        fileStatus.put("320", 404);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/releases", exchange -> {
            requests.incrementAndGet();
            byte[] body = releasesBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(releasesStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/file/", exchange -> {
            requests.incrementAndGet();
            String tag = exchange.getRequestURI().getPath().substring("/file/".length());
            exchange.sendResponseHeaders(fileStatus.getOrDefault(tag, 404), -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HladbVersionService service(Duration ttl) {
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        return new HladbVersionService(base + "/releases", base + "/file/%s", ttl);
    }

    @Test
    public void listsOnlyReleasesThatAreListedAndHaveTheFile() throws Exception {
        List<HladbVersionService.Release> usable = service(Duration.ofHours(1)).getUsableReleases();

        assertEquals(List.of(
                new HladbVersionService.Release("3.65.0", "2026-07"),
                new HladbVersionService.Release("3.20.0", "2015-04")), usable);
    }

    @Test
    public void secondCallIsServedFromTheCache() throws Exception {
        HladbVersionService service = service(Duration.ofHours(1));
        service.getUsableReleases();
        int afterFirst = requests.get();

        service.getUsableReleases();

        assertEquals(afterFirst, requests.get());
    }

    @Test
    public void anUnexpectedStatusFailsTheRefreshInsteadOfShrinkingTheList() {
        fileStatus.put("3200", 503);

        assertThrows(IOException.class, () -> service(Duration.ofHours(1)).getUsableReleases());
    }

    @Test
    public void staleListIsServedWhenARefreshFails() throws Exception {
        HladbVersionService service = service(Duration.ZERO);
        List<HladbVersionService.Release> first = service.getUsableReleases();

        releasesStatus = 500;

        assertEquals(first, service.getUsableReleases());
    }

    @Test
    public void noListAndNoCacheIsAnError() {
        releasesStatus = 500;

        assertThrows(IOException.class, () -> service(Duration.ofHours(1)).getUsableReleases());
    }

    @Test
    public void parsesTheReleaseListFormat() {
        assertEquals(List.of(new HladbVersionService.Release("3.65.0", "2026-07"),
                new HladbVersionService.Release("3.64.0", "")),
                HladbVersionService.parse("3.65.0 2026-07\n\nnot a release\n3.64.0\n"));
    }
}
