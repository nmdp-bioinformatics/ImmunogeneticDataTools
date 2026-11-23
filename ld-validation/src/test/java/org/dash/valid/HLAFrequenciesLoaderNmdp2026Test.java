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
package org.dash.valid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.junit.jupiter.api.Test;

// The pieces of the "nmdp-2026" frequency set that don't need the real (licensed, downloaded)
// release files: name lookup, default linkage, and deriving column order from a file name.
public class HLAFrequenciesLoaderNmdp2026Test {

	@Test
	public void testNmdp2026IsALookupableFrequencySet() {
		assertEquals(Frequencies.NMDP_2026, Frequencies.lookup("nmdp-2026"));
		assertEquals("nmdp-2026", Frequencies.NMDP_2026.getShortName());
	}

	@Test
	public void testDeriveLocusPositionsFollowsTheFileName() {
		assertArrayEquals(new Locus[] { Locus.HLA_A, Locus.HLA_C, Locus.HLA_B },
				HLAFrequenciesLoader.deriveLocusPositions("A~C~B.xlsx"));
		assertArrayEquals(new Locus[] { Locus.HLA_DPA1, Locus.HLA_DPB1 },
				HLAFrequenciesLoader.deriveLocusPositions("DPA1~DPB1.xlsx"));
	}

	// Locus.lookup("DRBX") is its own Locus value, but every reference-data locus set keys on
	// HLA_DRB345, so a DRBX column must land under that key or the six- and nine-locus files
	// would never match their Linkages.
	@Test
	public void testDrbxColumnsAreKeyedAsDrb345() {
		assertArrayEquals(new Locus[] { Locus.HLA_A, Locus.HLA_C, Locus.HLA_B, Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQB1 },
				HLAFrequenciesLoader.deriveLocusPositions("A~C~B~DRBX~DRB1~DQB1.xlsx"));
		assertArrayEquals(new Locus[] { Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQA1, Locus.HLA_DQB1, Locus.HLA_DPA1, Locus.HLA_DPB1 },
				HLAFrequenciesLoader.deriveLocusPositions("DRBX~DRB1~DQA1~DQB1~DPA1~DPB1.xlsx"));
	}

	// A failed nmdp-2026 load (here: terms not acknowledged, empty cache) must throw every
	// time, not leave a half-built singleton that later calls silently use with no data.
	@Test
	public void testFailedNmdp2026LoadDoesNotLeaveAnEmptySingleton(@org.junit.jupiter.api.io.TempDir java.nio.file.Path emptyCache) {
		String freq = System.getProperty(Frequencies.FREQUENCIES_PROPERTY);
		String cache = System.getProperty(org.dash.valid.freq.Nmdp2026FrequencyCache.CACHE_PROPERTY);
		String license = System.getProperty(org.dash.valid.freq.Nmdp2026FrequencyCache.LICENSE_PROPERTY);
		try {
			HLAFrequenciesLoader.reset();
			System.setProperty(Frequencies.FREQUENCIES_PROPERTY, "nmdp-2026");
			System.setProperty(org.dash.valid.freq.Nmdp2026FrequencyCache.CACHE_PROPERTY, emptyCache.toString());
			System.clearProperty(org.dash.valid.freq.Nmdp2026FrequencyCache.LICENSE_PROPERTY);

			assertThrows(IllegalStateException.class, HLAFrequenciesLoader::getInstance);
			assertThrows(IllegalStateException.class, HLAFrequenciesLoader::getInstance);
		}
		finally {
			HLAFrequenciesLoader.reset();
			restore(Frequencies.FREQUENCIES_PROPERTY, freq);
			restore(org.dash.valid.freq.Nmdp2026FrequencyCache.CACHE_PROPERTY, cache);
			restore(org.dash.valid.freq.Nmdp2026FrequencyCache.LICENSE_PROPERTY, license);
		}
	}

	private static void restore(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		}
		else {
			System.setProperty(key, value);
		}
	}

	@Test
	public void testUnrecognizedLocusTokenIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> HLAFrequenciesLoader.deriveLocusPositions("A~Q9.xlsx"));
	}

	@Test
	public void testDrbxHaplotypesLoadUnderTheDrb345Key() throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Sheet1");
		Row header = sheet.createRow(0);
		header.createCell(0).setCellValue("Haplotype");
		header.createCell(1).setCellValue("CAU");
		header.createCell(2).setCellValue("TotalFreq");
		Row row = sheet.createRow(1);
		row.createCell(0).setCellValue("DRB3*01:01~DRB1*03:01");
		row.createCell(1).setCellValue(0.1);
		row.createCell(2).setCellValue(0.1);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();

		List<DisequilibriumElement> elements = HLAFrequenciesLoader.loadNMDPLinkageReferenceData(
				new ByteArrayInputStream(out.toByteArray()), HLAFrequenciesLoader.deriveLocusPositions("DRBX~DRB1.xlsx"));

		assertEquals(1, elements.size());
		assertTrue(elements.get(0).getHlaElement(Locus.HLA_DRB345).contains("HLA-DRB3*01:01"));
		assertTrue(elements.get(0).getHlaElement(Locus.HLA_DRB1).contains("HLA-DRB1*03:01"));
	}
}
