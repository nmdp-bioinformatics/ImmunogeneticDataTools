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
package org.dash.valid.ars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import org.dash.valid.gl.GLStringConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AntigenRecognitionSiteCacheTest {

	@TempDir
	Path tempCacheDir;

	@BeforeEach
	public void pointCacheAtTempDir() {
		System.setProperty(AntigenRecognitionSiteCache.ARS_CACHE_DIR_PROPERTY, tempCacheDir.toString());
	}

	@AfterEach
	public void clearOverride() {
		System.clearProperty(AntigenRecognitionSiteCache.ARS_CACHE_DIR_PROPERTY);
	}

	@Test
	public void testMissingCacheFileIsAPlainMiss() {
		assertNull(AntigenRecognitionSiteCache.read("3.65.0"));
	}

	@Test
	public void testWriteThenReadRoundTripsForAPinnedVersion() {
		HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
		arsMap.put("HLA-A*01:01g", new HashSet<String>(java.util.Arrays.asList("HLA-A*01:01", "HLA-A*01:02")));
		arsMap.put("HLA-B*07:02g", new HashSet<String>(java.util.Arrays.asList("HLA-B*07:02")));

		AntigenRecognitionSiteCache.write("3.65.0", arsMap);
		HashMap<String, HashSet<String>> read = AntigenRecognitionSiteCache.read("3.65.0");

		assertEquals(arsMap, read);
	}

	@Test
	public void testEmptyAlleleSetRoundTrips() {
		// A gGroup with no gGroupAllele children -- the edge case where a naive "".split(",")
		// would reconstruct a bogus single empty-string allele instead of a truly empty set.
		HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
		arsMap.put("HLA-A*99:99g", new HashSet<String>());

		AntigenRecognitionSiteCache.write("3.65.0", arsMap);
		HashMap<String, HashSet<String>> read = AntigenRecognitionSiteCache.read("3.65.0");

		assertEquals(arsMap, read);
		assertTrue(read.get("HLA-A*99:99g").isEmpty());
	}

	@Test
	public void testDifferentHladbVersionsDoNotCollide() {
		HashMap<String, HashSet<String>> arsMapA = new HashMap<String, HashSet<String>>();
		arsMapA.put("HLA-A*01:01g", new HashSet<String>(java.util.Arrays.asList("HLA-A*01:01")));

		HashMap<String, HashSet<String>> arsMapB = new HashMap<String, HashSet<String>>();
		arsMapB.put("HLA-B*07:02g", new HashSet<String>(java.util.Arrays.asList("HLA-B*07:02")));

		AntigenRecognitionSiteCache.write("3.65.0", arsMapA);
		AntigenRecognitionSiteCache.write("3.60.0", arsMapB);

		assertEquals(arsMapA, AntigenRecognitionSiteCache.read("3.65.0"));
		assertEquals(arsMapB, AntigenRecognitionSiteCache.read("3.60.0"));
	}

	@Test
	public void testPinnedVersionNeverExpires() throws IOException {
		HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
		arsMap.put("HLA-A*01:01g", new HashSet<String>(java.util.Arrays.asList("HLA-A*01:01")));

		writeRawCacheFile("3.65.0", arsMap, System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(365));

		// A year-old cache entry for a *pinned* version must still be honored -- the data can
		// never have changed since IMGT/HLA published that specific release.
		assertEquals(arsMap, AntigenRecognitionSiteCache.read("3.65.0"));
	}

	@Test
	public void testLatestExpiresAfterTtl() throws IOException {
		HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
		arsMap.put("HLA-A*01:01g", new HashSet<String>(java.util.Arrays.asList("HLA-A*01:01")));

		writeRawCacheFile(GLStringConstants.LATEST_HLADB, arsMap, System.currentTimeMillis() - java.util.concurrent.TimeUnit.HOURS.toMillis(25));

		// Latest is a moving branch, not a pinned release -- a 25-hour-old entry must be treated
		// as stale, not served forever.
		assertNull(AntigenRecognitionSiteCache.read(GLStringConstants.LATEST_HLADB));
	}

	@Test
	public void testLatestWithinTtlIsStillHonored() throws IOException {
		HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
		arsMap.put("HLA-A*01:01g", new HashSet<String>(java.util.Arrays.asList("HLA-A*01:01")));

		writeRawCacheFile(GLStringConstants.LATEST_HLADB, arsMap, System.currentTimeMillis() - java.util.concurrent.TimeUnit.HOURS.toMillis(1));

		assertEquals(arsMap, AntigenRecognitionSiteCache.read(GLStringConstants.LATEST_HLADB));
	}

	@Test
	public void testCorruptCacheFileIsATreatedAsAMiss() throws IOException {
		File file = AntigenRecognitionSiteCache.cacheFile("3.65.0");
		file.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write("not a cache file at all\njust garbage\n");
		}

		assertNull(AntigenRecognitionSiteCache.read("3.65.0"));
	}

	@Test
	public void testWriteIsBestEffortAndDoesNotThrowWhenDirectoryCannotBeCreated() throws IOException {
		// Point the "cache directory" at a path that's actually a regular file -- Files#createDirectories
		// will fail, and write() must swallow that rather than propagating it, since a cache
		// write failing must never affect the correctness of a result the caller already has.
		File notADirectory = tempCacheDir.resolve("actually-a-file").toFile();
		try (FileWriter writer = new FileWriter(notADirectory)) {
			writer.write("occupying this path");
		}
		System.setProperty(AntigenRecognitionSiteCache.ARS_CACHE_DIR_PROPERTY, notADirectory.getAbsolutePath());

		HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
		arsMap.put("HLA-A*01:01g", new HashSet<String>());

		AntigenRecognitionSiteCache.write("3.65.0", arsMap);
		// No exception reaching here is the actual assertion.
	}

	private void writeRawCacheFile(String hladb, HashMap<String, HashSet<String>> arsMap, long cachedAtMillis) throws IOException {
		File file = AntigenRecognitionSiteCache.cacheFile(hladb);
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			writer.write("#cachedAt=" + cachedAtMillis + "\n");
			for (java.util.Map.Entry<String, HashSet<String>> entry : arsMap.entrySet()) {
				writer.write(entry.getKey() + "\t" + String.join(",", entry.getValue()) + "\n");
			}
		}
	}
}
