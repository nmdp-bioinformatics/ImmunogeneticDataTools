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
package org.dash.valid.freq;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

// Nmdp2026FrequencyCache against a local HTTP server and a one-file manifest: no network
// access and none of the real (licensed) NMDP data is needed.
public class Nmdp2026FrequencyCacheTest {
	private static final String FILE = "A~B.xlsx";
	private static final byte[] CONTENT = "pretend this is an xlsx".getBytes(StandardCharsets.UTF_8);

	@TempDir
	Path cacheDir;

	private HttpServer server;
	private final AtomicInteger requests = new AtomicInteger();
	private volatile int status = 200;
	private volatile byte[] body = CONTENT;
	private URI base;

	@BeforeEach
	public void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/files/", exchange -> {
			requests.incrementAndGet();
			byte[] payload = status == 200 ? body : new byte[0];
			exchange.sendResponseHeaders(status, payload.length == 0 ? -1 : payload.length);
			if (payload.length > 0) {
				exchange.getResponseBody().write(payload);
			}
			exchange.close();
		});
		server.start();
		base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/files/");
	}

	@AfterEach
	public void stopServer() {
		server.stop(0);
	}

	private static String md5(byte[] bytes) throws Exception {
		StringBuilder sb = new StringBuilder();
		for (byte b : MessageDigest.getInstance("MD5").digest(bytes)) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private Nmdp2026FrequencyCache cache(boolean accepted) throws Exception {
		Map<String, Object[]> manifest = new HashMap<String, Object[]>();
		manifest.put(FILE, new Object[] { (long) CONTENT.length, md5(CONTENT) });
		return new Nmdp2026FrequencyCache(cacheDir, base, manifest, accepted);
	}

	private long filesInCache() throws Exception {
		try (Stream<Path> files = Files.list(cacheDir)) {
			return files.count();
		}
	}

	@Test
	public void testDownloadsVerifiesAndThenServesFromCache() throws Exception {
		Nmdp2026FrequencyCache cache = cache(true);

		Path first = cache.ensure(FILE);
		assertArrayEquals(CONTENT, Files.readAllBytes(first));
		assertEquals(1, requests.get());

		Path second = cache.ensure(FILE);
		assertEquals(first, second);
		assertEquals(1, requests.get(), "a cache hit must not touch the network");
		assertEquals(1, filesInCache(), "no temp file should be left behind");
	}

	@Test
	public void testReportsDownloadProgressFromZeroToTheExpectedSize() throws Exception {
		java.util.List<long[]> seen = new java.util.concurrent.CopyOnWriteArrayList<>();
		Nmdp2026FrequencyCache.setDownloadListener((name, done, total) -> {
			assertEquals(FILE, name);
			seen.add(new long[] {done, total});
		});
		try {
			cache(true).ensure(FILE);
			assertEquals(0, seen.get(0)[0]);
			assertEquals(CONTENT.length, seen.get(seen.size() - 1)[0]);
			assertEquals(CONTENT.length, seen.get(seen.size() - 1)[1]);

			seen.clear();
			cache(true).ensure(FILE);
			assertTrue(seen.isEmpty(), "a cache hit downloads nothing, so reports nothing");
		}
		finally {
			Nmdp2026FrequencyCache.setDownloadListener(null);
		}
	}

	@Test
	public void testCacheHitNeedsNoLicenseAcknowledgement() throws Exception {
		cache(true).ensure(FILE);

		try (java.io.InputStream in = cache(false).open(FILE)) {
			assertArrayEquals(CONTENT, in.readAllBytes());
		}
		assertEquals(1, requests.get());
	}

	@Test
	public void testRefusesToDownloadWithoutLicenseAcknowledgement() throws Exception {
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> cache(false).ensure(FILE));

		assertEquals(0, requests.get(), "nothing may be requested before the terms are acknowledged");
		assertEquals(0, filesInCache());
		assertTrue(e.getMessage().contains("CC BY-NC-ND 4.0"));
		assertTrue(e.getMessage().contains("--accept-license"));
		assertTrue(e.getMessage().contains(Nmdp2026FrequencyCache.DOI));
	}

	@Test
	public void testRejectsAndDiscardsAFileThatFailsTheChecksum() throws Exception {
		body = "something else entirely".getBytes(StandardCharsets.UTF_8);

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> cache(true).ensure(FILE));

		assertTrue(e.getMessage().contains("does not match"));
		assertEquals(0, filesInCache(), "neither the bad file nor its temp file may remain");
	}

	@Test
	public void testRejectsASameSizeFileWithTheWrongChecksum() throws Exception {
		body = new byte[CONTENT.length];

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> cache(true).ensure(FILE));

		assertTrue(e.getMessage().contains("does not match"));
		assertEquals(0, filesInCache());
	}

	@Test
	public void testStopsDownloadingOnceTheExpectedSizeIsExceeded() throws Exception {
		body = new byte[CONTENT.length * 1000];

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> cache(true).ensure(FILE));

		assertTrue(e.getMessage().contains("larger than the expected"));
		assertEquals(0, filesInCache());
	}

	@Test
	public void testACachedSameSizeFileWithTheWrongChecksumIsRejected() throws Exception {
		Files.write(cacheDir.resolve(FILE), new byte[CONTENT.length]);

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> cache(true).ensure(FILE));

		assertTrue(e.getMessage().contains("Delete it"));
		assertEquals(0, requests.get(), "a bad cached file is reported, not silently replaced");
	}

	@Test
	public void testAWrongSizeCachedFileIsReplacedByADownloadOnceTheTermsAreAccepted() throws Exception {
		Files.write(cacheDir.resolve(FILE), new byte[] { 1, 2, 3 });

		assertThrows(IllegalStateException.class, () -> cache(false).ensure(FILE));
		assertEquals(0, requests.get());

		assertArrayEquals(CONTENT, Files.readAllBytes(cache(true).ensure(FILE)));
		assertEquals(1, requests.get());
	}

	@Test
	public void testHttpErrorIsAnIllegalStateExceptionWithGuidance() throws Exception {
		status = 404;

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> cache(true).ensure(FILE));

		assertTrue(e.getMessage().contains("HTTP 404"));
		assertTrue(e.getMessage().contains("place it, unmodified"));
		assertEquals(0, filesInCache());
	}

	@Test
	public void testOnlyPinnedFileNamesAreAccepted() throws Exception {
		Nmdp2026FrequencyCache cache = cache(true);

		assertThrows(IllegalArgumentException.class, () -> cache.ensure("../escape.xlsx"));
		assertThrows(IllegalArgumentException.class, () -> cache.ensure("unknown.xlsx"));
		assertEquals(0, requests.get());
	}

	@Test
	public void testRealManifestPinsTheReleaseFiles() {
		assertTrue(Nmdp2026FrequencyCache.isKnownFile("A~C~B~DRBX~DRB1~DQA1~DQB1~DPA1~DPB1.xlsx"));
		assertTrue(Nmdp2026FrequencyCache.isKnownFile("A~C~B~DRB1~DQB1.xlsx"));
		assertTrue(Nmdp2026FrequencyCache.isKnownFile("DRBX.xlsx"));
		assertFalse(Nmdp2026FrequencyCache.isKnownFile("C~B.xlsx"), "the release has no C~B file");
	}

	@Test
	public void testCacheDirectoryHonorsTheOverrideProperty() {
		String previous = System.getProperty(Nmdp2026FrequencyCache.CACHE_PROPERTY);
		try {
			System.setProperty(Nmdp2026FrequencyCache.CACHE_PROPERTY, cacheDir.toString());
			assertEquals(cacheDir, Nmdp2026FrequencyCache.defaultCacheDirectory());
		}
		finally {
			if (previous == null) {
				System.clearProperty(Nmdp2026FrequencyCache.CACHE_PROPERTY);
			}
			else {
				System.setProperty(Nmdp2026FrequencyCache.CACHE_PROPERTY, previous);
			}
		}
	}

	@Test
	public void testDefaultCacheDirectoryIsNotTheWorkingDirectory() {
		String previous = System.getProperty(Nmdp2026FrequencyCache.CACHE_PROPERTY);
		try {
			System.clearProperty(Nmdp2026FrequencyCache.CACHE_PROPERTY);
			Path dir = Nmdp2026FrequencyCache.defaultCacheDirectory().toAbsolutePath();
			assertFalse(dir.startsWith(Path.of("").toAbsolutePath()), "must not default to somewhere a git add could sweep up");
			assertTrue(dir.endsWith(Path.of("immunogenetic-data-tools", "nmdp-2026")));
		}
		finally {
			if (previous != null) {
				System.setProperty(Nmdp2026FrequencyCache.CACHE_PROPERTY, previous);
			}
		}
	}
}
