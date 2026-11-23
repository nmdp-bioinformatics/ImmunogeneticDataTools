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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Fetches, on demand, the unmodified NMDP "Classical HLA Allele and Haplotype Frequency
 * Estimates in US Populations" files (Zenodo record 17966993, version 1.0.0) and keeps them in
 * a local cache directory so {@link HLAFrequenciesLoader} can read them with its existing xlsx
 * reader.
 *
 * <p>Nothing from this dataset is bundled with, or redistributed by, this software. The data
 * is owned by NMDP and published under CC BY-NC-ND 4.0 (non-commercial, no derivatives): this
 * class only downloads the original file, byte for byte, for the user who asked for the
 * "nmdp-2026" frequency set, never writes a converted copy, and refuses to download until the
 * user has acknowledged the terms (see {@link #LICENSE_PROPERTY}).</p>
 */
public class Nmdp2026FrequencyCache {
	public static final String LICENSE_PROPERTY = "org.dash.accept.license";
	public static final String CACHE_PROPERTY = "org.dash.frequencies.cache";
	public static final String LICENSE_ACCEPTANCE_VALUE = "nmdp-2026";

	public static final String DOI = "10.5281/zenodo.17966993";
	public static final String RECORD_URL = "https://zenodo.org/records/17966993";

	static final URI DEFAULT_FILES_BASE = URI.create(RECORD_URL + "/files/");

	private static final Logger LOGGER = Logger.getLogger(Nmdp2026FrequencyCache.class.getName());

	// File name -> {size in bytes, MD5} as published by the Zenodo record's files API
	// (https://zenodo.org/api/records/17966993). Pinning these means a changed or corrupted
	// download is rejected, and only these names can ever be fetched or written.
	private static final Map<String, Object[]> MANIFEST;
	static {
		Map<String, Object[]> m = new HashMap<String, Object[]>();
		m.put("A.xlsx", new Object[] {158144L, "de680dbea80561bea1b6de751464dba4"});
		m.put("B.xlsx", new Object[] {195660L, "38c4d9896aae254f64443749c93945c0"});
		m.put("C.xlsx", new Object[] {148285L, "21744343178a9e30a4ecf73a256ca013"});
		m.put("DRB1.xlsx", new Object[] {78810L, "18353c500ac828230a8d85c0bf00dd0e"});
		m.put("DRBX.xlsx", new Object[] {23332L, "96e27e383ea04c7ecc182f61a30c08ad"});
		m.put("DQA1.xlsx", new Object[] {17247L, "e5b2f304724f36fcd4c2c68b5341274d"});
		m.put("DQB1.xlsx", new Object[] {38207L, "b2fd44526c813c752b0bd3bea0dfaf89"});
		m.put("DPA1.xlsx", new Object[] {18887L, "4ca13b3a2764ca691f930c2d75e1563c"});
		m.put("DPB1.xlsx", new Object[] {59653L, "773af14d20b29de335bb1ddd32da6016"});
		m.put("DRB1~DQB1.xlsx", new Object[] {240885L, "3a79e1f7ee5df40a37255ded3e647b0f"});
		m.put("DRBX~DRB1.xlsx", new Object[] {209499L, "8eb777390e836c5453c57977f571d458"});
		m.put("DRBX~DQB1.xlsx", new Object[] {133772L, "e9791795199e8ddcecd7ef7fd8667385"});
		m.put("DQA1~DQB1.xlsx", new Object[] {217855L, "e3c4b19ce96fa2ce398ad3e85b53a070"});
		m.put("DPA1~DPB1.xlsx", new Object[] {245796L, "f7fe08233eef6c3f4680e04d60e52e41"});
		m.put("DRBX~DRB1~DQA1~DQB1.xlsx", new Object[] {1542886L, "70ca00c3d3a10c9cbc71a15b6308cb3d"});
		m.put("DRBX~DRB1~DQA1~DQB1~DPA1~DPB1.xlsx", new Object[] {5352922L, "d0cc715a6bce802f56b8ab37e73e7dc9"});
		m.put("A~C~B.xlsx", new Object[] {6549690L, "d29da0e8390d5fbcfa20169cfcb9a048"});
		m.put("A~B~DRB1.xlsx", new Object[] {20917012L, "c629a9049b08d6cf251101cdd2f7409e"});
		m.put("A~B~DRB1~DQB1.xlsx", new Object[] {29824918L, "428e1c405e15009381f6c100756239a7"});
		m.put("C~B~DRB1~DQB1.xlsx", new Object[] {12019019L, "d35946a8efddf975cda6931d6cafe733"});
		m.put("A~C~B~DRB1.xlsx", new Object[] {41166725L, "9fa36d3725b99afca2db4dd3a56cce12"});
		m.put("A~C~B~DRB1~DQB1.xlsx", new Object[] {53372531L, "f0a9a5af01e02ad383a8ca948d1f45fe"});
		m.put("A~C~B~DRBX~DRB1~DQB1.xlsx", new Object[] {59563811L, "8055b9df6a54e57f5e304b4a3c614706"});
		m.put("A~C~B~DRBX~DRB1~DQB1~DPB1.xlsx", new Object[] {157651635L, "202266069a741b5d8f6b8982f5473f0d"});
		m.put("A~C~B~DRBX~DRB1~DQA1~DQB1~DPA1~DPB1.xlsx", new Object[] {180856641L, "4698f92515f4592dddb5e16756cb1ba4"});
		MANIFEST = Collections.unmodifiableMap(m);
	}

	/**
	 * A download is needed but the CC BY-NC-ND terms have not been acknowledged. A distinct type
	 * (still an {@link IllegalStateException}) so a service can answer 403 instead of a generic
	 * failure.
	 */
	public static class LicenseNotAcceptedException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		public LicenseNotAcceptedException(String message) {
			super(message);
		}
	}

	/** Told how far along a download is, so a service can show progress. */
	public interface DownloadListener {
		/** Called at the start (0 bytes), about every 1% or 250 ms while streaming, and at the end. */
		void onProgress(String fileName, long bytesDownloaded, long totalBytes);
	}

	// Process-wide rather than per instance: HLAFrequenciesLoader creates its own cache, and ld-service
	// runs one analysis at a time, so whoever is loading sets this for the duration and clears it.
	private static volatile DownloadListener downloadListener;

	public static void setDownloadListener(DownloadListener listener) {
		downloadListener = listener;
	}

	private final Path cacheDirectory;
	private final URI filesBase;
	private final Map<String, Object[]> manifest;
	private final boolean licenseAccepted;

	public Nmdp2026FrequencyCache() {
		this(defaultCacheDirectory(), DEFAULT_FILES_BASE, MANIFEST,
				LICENSE_ACCEPTANCE_VALUE.equals(System.getProperty(LICENSE_PROPERTY)));
	}

	// Package-private so tests can point the downloader at a local server with a tiny manifest.
	Nmdp2026FrequencyCache(Path cacheDirectory, URI filesBase, Map<String, Object[]> manifest, boolean licenseAccepted) {
		this.cacheDirectory = cacheDirectory;
		this.filesBase = filesBase;
		this.manifest = manifest;
		this.licenseAccepted = licenseAccepted;
	}

	public static boolean isKnownFile(String fileName) {
		return MANIFEST.containsKey(fileName);
	}

	public Path getCacheDirectory() {
		return cacheDirectory;
	}

	/**
	 * The cache location: the {@link #CACHE_PROPERTY} system property if set, otherwise the
	 * platform's per-user cache directory. Deliberately never the working directory, so a
	 * large licensed file can't be swept up by an accidental "git add".
	 */
	static Path defaultCacheDirectory() {
		String override = System.getProperty(CACHE_PROPERTY);
		if (override != null && !override.isEmpty()) {
			return Paths.get(override);
		}

		String home = System.getProperty("user.home");
		String os = System.getProperty("os.name", "").toLowerCase();
		Path base;
		if (os.contains("mac")) {
			base = Paths.get(home, "Library", "Caches");
		}
		else if (os.contains("win") && System.getenv("LOCALAPPDATA") != null) {
			base = Paths.get(System.getenv("LOCALAPPDATA"));
		}
		else {
			String xdg = System.getenv("XDG_CACHE_HOME");
			base = (xdg != null && !xdg.isEmpty()) ? Paths.get(xdg) : Paths.get(home, ".cache");
		}

		return base.resolve("immunogenetic-data-tools").resolve("nmdp-2026");
	}

	/**
	 * Opens a cached copy of one of the pinned files, downloading it first if it is not
	 * already cached.
	 *
	 * @throws IllegalArgumentException if the name is not one of the pinned files
	 * @throws IllegalStateException if a download is needed and the license notice has not
	 *         been acknowledged, or the download fails verification
	 */
	public InputStream open(String fileName) throws IOException {
		return Files.newInputStream(ensure(fileName));
	}

	public Path ensure(String fileName) throws IOException {
		Object[] expected = manifest.get(fileName);
		if (expected == null) {
			throw new IllegalArgumentException("Not a file in the NMDP 2026 frequency release: " + fileName);
		}

		long expectedSize = (Long) expected[0];
		String expectedMd5 = (String) expected[1];
		Path target = cacheDirectory.resolve(fileName);

		boolean present = Files.isRegularFile(target);
		if (present && Files.size(target) == expectedSize) {
			// Re-verified on every use (~0.5s for the largest file): the README lets users place
			// files by hand, and a same-size corrupt or modified file must not reach the parser.
			String actual = md5Of(target);
			if (actual.equals(expectedMd5)) {
				return target;
			}
			throw new IllegalStateException("The cached " + target + " does not match the NMDP 2026 release (MD5 " + actual
					+ " vs " + expectedMd5 + "). Delete it so it can be downloaded again, or replace it with the unmodified file from "
					+ RECORD_URL);
		}

		if (!licenseAccepted) {
			throw new LicenseNotAcceptedException(licenseNotice(fileName, present));
		}

		try {
			Files.createDirectories(cacheDirectory);
			download(fileName, target, expectedSize, expectedMd5);
		}
		catch (IOException e) {
			// Unchecked on purpose: HLAFrequenciesLoader.init(Frequencies) turns any IOException
			// into System.exit(-1), which a failed download (offline, Zenodo down) shouldn't do
			// to a long-running service; the message already says what to do instead.
			throw new IllegalStateException(e.getMessage(), e);
		}

		return target;
	}

	private void download(String fileName, Path target, long expectedSize, String expectedMd5) throws IOException {
		URI uri = URI.create(filesBase.toString() + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20") + "?download=1");
		LOGGER.info("Downloading " + fileName + " (" + expectedSize + " bytes) from " + filesBase + " to " + cacheDirectory);

		Path temp = Files.createTempFile(cacheDirectory, fileName + ".", ".part");
		try {
			// Timeouts so a stalled connection fails instead of hanging the CLI or a service
			// thread forever (the request timeout covers the wait for response headers).
			HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
					.connectTimeout(Duration.ofSeconds(30)).build();
			HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).GET().build();

			HttpResponse<InputStream> response;
			try {
				response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while downloading " + fileName, e);
			}

			if (response.statusCode() != 200) {
				response.body().close();
				throw new IOException("Could not download " + fileName + " from " + RECORD_URL + ": HTTP " + response.statusCode()
						+ ". If you are offline, download the file from " + RECORD_URL + " yourself and place it, unmodified, in "
						+ cacheDirectory);
			}

			DownloadListener listener = downloadListener;
			if (listener != null) {
				listener.onProgress(fileName, 0, expectedSize);
			}
			long lastReportedAt = System.nanoTime();
			long lastReportedSize = 0;
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			long size = 0;
			try (InputStream in = response.body();
					java.io.OutputStream out = Files.newOutputStream(temp)) {
				byte[] buffer = new byte[64 * 1024];
				int read;
				while ((read = in.read(buffer)) != -1) {
					size += read;
					long now = System.nanoTime();
					if (listener != null && (size - lastReportedSize >= expectedSize / 100 || now - lastReportedAt >= 250_000_000L)) {
						listener.onProgress(fileName, size, expectedSize);
						lastReportedSize = size;
						lastReportedAt = now;
					}
					if (size > expectedSize) {
						throw new IllegalStateException("Downloaded " + fileName + " is larger than the expected " + expectedSize
								+ " bytes; stopped and discarded it.");
					}
					md5.update(buffer, 0, read);
					out.write(buffer, 0, read);
				}
			}

			String actualMd5 = toHex(md5.digest());
			if (size != expectedSize || !actualMd5.equals(expectedMd5)) {
				throw new IllegalStateException("Downloaded " + fileName + " does not match the expected release (size " + size
						+ " vs " + expectedSize + ", MD5 " + actualMd5 + " vs " + expectedMd5 + "). It was discarded.");
			}

			Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			if (listener != null) {
				listener.onProgress(fileName, expectedSize, expectedSize);
			}
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 is not available in this JVM", e);
		}
		finally {
			Files.deleteIfExists(temp);
		}
	}

	private static String md5Of(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] buffer = new byte[64 * 1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				md5.update(buffer, 0, read);
			}
			return toHex(md5.digest());
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 is not available in this JVM", e);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	static String licenseNotice(String fileName, boolean presentButWrong) {
		return "The \"nmdp-2026\" frequency set needs " + fileName
				+ (presentButWrong ? ", and the cached copy is not the expected size.\n" : ", which is not cached yet.\n")
				+ "These frequencies are NMDP's \"Classical HLA Allele and Haplotype Frequency Estimates in US Populations\""
				+ " (doi:" + DOI + "), owned by NMDP and licensed CC BY-NC-ND 4.0: non-commercial use only, no derivatives, attribution required."
				+ " They are not distributed with this software; they are downloaded unmodified from " + RECORD_URL + " for you.\n"
				+ "Read the terms there. To accept them and download, re-run with --accept-license"
				+ " (or -D" + LICENSE_PROPERTY + "=" + LICENSE_ACCEPTANCE_VALUE + ").\n"
				+ "Alternatively, download the file yourself and place it, unmodified, in the cache directory.";
	}
}
