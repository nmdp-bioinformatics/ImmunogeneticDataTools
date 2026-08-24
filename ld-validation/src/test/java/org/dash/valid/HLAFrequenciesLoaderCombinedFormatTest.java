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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.junit.jupiter.api.Test;

// Regression tests for the two reference-file layouts HLAFrequenciesLoader.
// loadNMDPLinkageReferenceData(InputStream, Locus[]) and loadIndividualLocusFrequency(InputStream)
// now read via StreamingXlsxRows (SAX) rather than the DOM WorkbookFactory.create()/XSSFWorkbook
// previously used. That switch was needed because the DOM approach couldn't practically read the
// NMDP nine-locus reference release's larger files (up to ~170MB compressed): even a mid-sized
// 57MB file ran 15+ minutes and past 2GB RSS without finishing the parse. Existing coverage
// (HLAFrequenciesLoaderTest.testLoadNMDPLinkageReferenceData) already exercises the streaming
// reader against a real bundled legacy-layout file; these tests instead build small workbooks in
// memory via POI so both layouts' parsing logic -- including the new-format rank computation and
// zero/blank-frequency handling neither had a direct test for before -- are pinned down with known
// expected values, without needing to check a large, non-redistributable fixture into the repo.
public class HLAFrequenciesLoaderCombinedFormatTest {

	private static InputStream workbookToStream(XSSFWorkbook workbook) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		workbook.write(out);
		workbook.close();
		return new ByteArrayInputStream(out.toByteArray());
	}

	private static void setCell(Row row, int col, String value) {
		row.createCell(col).setCellValue(value);
	}

	private static void setCell(Row row, int col, double value) {
		row.createCell(col).setCellValue(value);
	}

	// Builds a combined-haplotype-layout workbook (see HLAFrequenciesLoader's
	// COMBINED_HAPLOTYPE_HEADER): one "Haplotype" column, one frequency column per named
	// population, then "TotalFreq". A null frequency leaves that cell entirely absent from the
	// row, matching how the real NMDP nine-locus release's files omit zero-frequency cells
	// rather than writing literal 0s.
	private static InputStream buildCombinedHaplotypeWorkbook(String[] populations, String[] haplotypes, Double[][] frequencies) throws Exception {
		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Sheet1");

		Row header = sheet.createRow(0);
		setCell(header, 0, "Haplotype");
		for (int p = 0; p < populations.length; p++) {
			setCell(header, p + 1, populations[p]);
		}
		setCell(header, populations.length + 1, "TotalFreq");

		for (int h = 0; h < haplotypes.length; h++) {
			Row row = sheet.createRow(h + 1);
			setCell(row, 0, haplotypes[h]);
			double total = 0;
			for (int p = 0; p < populations.length; p++) {
				Double freq = frequencies[h][p];
				if (freq != null) {
					setCell(row, p + 1, freq.doubleValue());
					total += freq.doubleValue();
				}
			}
			setCell(row, populations.length + 1, total);
		}

		return workbookToStream(workbook);
	}

	@Test
	public void testCombinedHaplotypeFormatParsesLociAndFrequencies() throws Exception {
		InputStream inStream = buildCombinedHaplotypeWorkbook(
				new String[] { "AAFA", "CAU" },
				new String[] {
						"A*01:01~C*07:01~B*08:01",
						"A*02:01~C*05:01~B*44:02"
				},
				new Double[][] {
						{ 0.05, 0.02 },
						{ 0.10, 0.09 }
				});

		List<DisequilibriumElement> elements = HLAFrequenciesLoader.loadNMDPLinkageReferenceData(
				inStream, new Locus[] { Locus.HLA_A, Locus.HLA_C, Locus.HLA_B });

		assertEquals(2, elements.size());

		DisequilibriumElement first = elements.get(0);
		assertTrue(first.getHlaElement(Locus.HLA_A).contains("HLA-A*01:01"));
		assertTrue(first.getHlaElement(Locus.HLA_C).contains("HLA-C*07:01"));
		assertTrue(first.getHlaElement(Locus.HLA_B).contains("HLA-B*08:01"));

		DisequilibriumElement second = elements.get(1);
		assertTrue(second.getHlaElement(Locus.HLA_A).contains("HLA-A*02:01"));
	}

	@Test
	public void testCombinedHaplotypeFormatComputesRankPerPopulation() throws Exception {
		// AAFA frequencies (desc): h2 (0.10) > h1 (0.05) > h3 (0.02) -> ranks 1, 2, 3
		// CAU  frequencies (desc): h3 (0.20) > h2 (0.09) > h1 (0.02) -> ranks 1, 2, 3
		InputStream inStream = buildCombinedHaplotypeWorkbook(
				new String[] { "AAFA", "CAU" },
				new String[] {
						"A*01:01~C*07:01~B*08:01",
						"A*02:01~C*05:01~B*44:02",
						"A*03:01~C*07:02~B*07:02"
				},
				new Double[][] {
						{ 0.05, 0.02 },
						{ 0.10, 0.09 },
						{ 0.02, 0.20 }
				});

		List<DisequilibriumElement> elements = HLAFrequenciesLoader.loadNMDPLinkageReferenceData(
				inStream, new Locus[] { Locus.HLA_A, Locus.HLA_C, Locus.HLA_B });

		assertEquals(3, elements.size());

		assertRank(elements.get(0), "AAFA", "2");
		assertRank(elements.get(1), "AAFA", "1");
		assertRank(elements.get(2), "AAFA", "3");

		assertRank(elements.get(0), "CAU", "3");
		assertRank(elements.get(1), "CAU", "2");
		assertRank(elements.get(2), "CAU", "1");
	}

	@Test
	public void testCombinedHaplotypeFormatOmitsBlankFrequencyCells() throws Exception {
		// h2 has no AAFA frequency at all (the cell is entirely absent from the row, not a
		// literal 0) -- it must not appear in h2's frequenciesByRace for AAFA, while h1 and h3
		// (which do have an AAFA frequency) still do.
		InputStream inStream = buildCombinedHaplotypeWorkbook(
				new String[] { "AAFA" },
				new String[] {
						"A*01:01~C*07:01~B*08:01",
						"A*02:01~C*05:01~B*44:02",
						"A*03:01~C*07:02~B*07:02"
				},
				new Double[][] {
						{ 0.05 },
						{ null },
						{ 0.02 }
				});

		List<DisequilibriumElement> elements = HLAFrequenciesLoader.loadNMDPLinkageReferenceData(
				inStream, new Locus[] { Locus.HLA_A, Locus.HLA_C, Locus.HLA_B });

		assertTrue(hasFrequencyForRace(elements.get(0), "AAFA"));
		assertTrue(!hasFrequencyForRace(elements.get(1), "AAFA"));
		assertTrue(hasFrequencyForRace(elements.get(2), "AAFA"));

		// The two haplotypes that do have an AAFA frequency are still ranked 1 and 2 between
		// themselves; the blank row simply isn't part of that ranking at all.
		assertRank(elements.get(0), "AAFA", "1");
		assertRank(elements.get(2), "AAFA", "2");
	}

	@Test
	public void testLoadIndividualLocusFrequencyCombinedFormat() throws Exception {
		InputStream inStream = buildCombinedHaplotypeWorkbook(
				new String[] { "AAFA" },
				new String[] { "A*02:01", "A*24:02" },
				new Double[][] { { 0.5 }, { 0.3 } });

		List<String> alleles = HLAFrequenciesLoader.loadIndividualLocusFrequency(inStream);

		assertEquals(2, alleles.size());
		assertTrue(alleles.get(0).equals("HLA-A*02:01"));
		assertTrue(alleles.get(1).equals("HLA-A*24:02"));
	}

	private static void assertRank(DisequilibriumElement element, String race, String expectedRank) {
		for (FrequencyByRace frequencyByRace : ((DisequilibriumElementByRace) element).getFrequenciesByRace()) {
			if (race.equals(frequencyByRace.getRace())) {
				assertEquals(expectedRank, frequencyByRace.getRank());
				return;
			}
		}
		throw new AssertionError("No frequency found for race " + race);
	}

	private static boolean hasFrequencyForRace(DisequilibriumElement element, String race) {
		for (FrequencyByRace frequencyByRace : ((DisequilibriumElementByRace) element).getFrequenciesByRace()) {
			if (race.equals(frequencyByRace.getRace())) {
				return true;
			}
		}
		return false;
	}
}
