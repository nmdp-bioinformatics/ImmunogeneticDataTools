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
package org.dash.valid.cwd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.dash.valid.gl.GLStringConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommonWellDocumentedCacheTest {

	@TempDir
	Path tempCacheDir;

	@BeforeEach
	public void pointCacheAtTempDir() {
		System.setProperty(CommonWellDocumentedCache.CWD_CACHE_DIR_PROPERTY, tempCacheDir.toString());
	}

	@AfterEach
	public void clearOverride() {
		System.clearProperty(CommonWellDocumentedCache.CWD_CACHE_DIR_PROPERTY);
	}

	@Test
	public void testMissingCacheFileIsAPlainMiss() {
		assertNull(CommonWellDocumentedCache.read("3.65.0"));
	}

	@Test
	public void testWriteThenReadRoundTripsForAPinnedVersion() {
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
		accessionMap.put("HLA-A*01:01", new ArrayList<String>(Arrays.asList("HLA00001", "HLA02169")));
		accessionMap.put("HLA-B*07:02", new ArrayList<String>(Arrays.asList("HLA00654")));

		CommonWellDocumentedCache.write("3.65.0", accessionMap);
		HashMap<String, List<String>> read = CommonWellDocumentedCache.read("3.65.0");

		assertEquals(accessionMap, read);
	}

	@Test
	public void testOrderAndDuplicatesArePreserved() {
		// Unlike the ARS cache's HashSet-valued map, accessions are a List -- order and
		// duplicates are both meaningful and must round-trip exactly.
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
		accessionMap.put("HLA-A*01:01", new ArrayList<String>(Arrays.asList("HLA00003", "HLA00001", "HLA00001")));

		CommonWellDocumentedCache.write("3.65.0", accessionMap);
		HashMap<String, List<String>> read = CommonWellDocumentedCache.read("3.65.0");

		assertEquals(Arrays.asList("HLA00003", "HLA00001", "HLA00001"), read.get("HLA-A*01:01"));
	}

	@Test
	public void testEmptyAccessionListRoundTrips() {
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
		accessionMap.put("HLA-A*99:99", new ArrayList<String>());

		CommonWellDocumentedCache.write("3.65.0", accessionMap);
		HashMap<String, List<String>> read = CommonWellDocumentedCache.read("3.65.0");

		assertEquals(accessionMap, read);
		assertTrue(read.get("HLA-A*99:99").isEmpty());
	}

	@Test
	public void testNullKeyedEntryIsSkippedWithoutBreakingTheRestOfTheWrite() {
		// Real-world regression: GLStringUtilities#convertToProteinLevel (whose result becomes
		// the map key in loadFromIMGT) can return null for a malformed/short allele name --
		// confirmed against actual hla.xml.zip data, not a hypothetical. Before this was handled,
		// a null key threw inside the write loop, was swallowed by write()'s own catch, and
		// silently left an orphaned, header-only .tmp file behind on *every* write -- meaning the
		// cache was never actually populated at all, ever, for real data.
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
		accessionMap.put("HLA-A*01:01", new ArrayList<String>(Arrays.asList("HLA00001")));
		accessionMap.put(null, new ArrayList<String>(Arrays.asList("HLA99999")));
		accessionMap.put("HLA-B*07:02", new ArrayList<String>(Arrays.asList("HLA00654")));

		CommonWellDocumentedCache.write("3.65.0", accessionMap);
		HashMap<String, List<String>> read = CommonWellDocumentedCache.read("3.65.0");

		assertEquals(Arrays.asList("HLA00001"), read.get("HLA-A*01:01"));
		assertEquals(Arrays.asList("HLA00654"), read.get("HLA-B*07:02"));
		assertEquals(2, read.size(), "the null-keyed entry should be dropped, not silently discard the whole write");

		File[] leftoverTempFiles = tempCacheDir.toFile().listFiles((dir, name) -> name.endsWith(".tmp"));
		assertTrue(leftoverTempFiles == null || leftoverTempFiles.length == 0, "a successful write must not leave an orphaned .tmp file behind");
	}

	@Test
	public void testPinnedVersionNeverExpires() throws IOException {
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
		accessionMap.put("HLA-A*01:01", new ArrayList<String>(Arrays.asList("HLA00001")));

		writeRawCacheFile("3.65.0", accessionMap, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365));

		assertEquals(accessionMap, CommonWellDocumentedCache.read("3.65.0"));
	}

	@Test
	public void testLatestExpiresAfterTtl() throws IOException {
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
		accessionMap.put("HLA-A*01:01", new ArrayList<String>(Arrays.asList("HLA00001")));

		writeRawCacheFile(GLStringConstants.LATEST_HLADB, accessionMap, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25));

		assertNull(CommonWellDocumentedCache.read(GLStringConstants.LATEST_HLADB));
	}

	@Test
	public void testLatestWithinTtlIsStillHonored() throws IOException {
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
		accessionMap.put("HLA-A*01:01", new ArrayList<String>(Arrays.asList("HLA00001")));

		writeRawCacheFile(GLStringConstants.LATEST_HLADB, accessionMap, System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

		assertEquals(accessionMap, CommonWellDocumentedCache.read(GLStringConstants.LATEST_HLADB));
	}

	@Test
	public void testCorruptCacheFileIsTreatedAsAMiss() throws IOException {
		File file = CommonWellDocumentedCache.cacheFile("3.65.0");
		file.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write("not a cache file at all\njust garbage\n");
		}

		assertNull(CommonWellDocumentedCache.read("3.65.0"));
	}

	private void writeRawCacheFile(String hladb, HashMap<String, List<String>> accessionMap, long cachedAtMillis) throws IOException {
		File file = CommonWellDocumentedCache.cacheFile(hladb);
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			writer.write("#cachedAt=" + cachedAtMillis + "\n");
			for (Map.Entry<String, List<String>> entry : accessionMap.entrySet()) {
				writer.write(entry.getKey() + "\t" + String.join(",", entry.getValue()) + "\n");
			}
		}
	}
}
