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
package org.nmdp.validation.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Issue #58. This replaces FormatConversionTest, whose entire body was commented out (it never
// actually exercised NormalizeFrequencyFile, or anything else) -- effectively zero real coverage
// for this tool before now.
//
// NormalizeFrequencyFile's input parsing (HLAFrequenciesLoader#loadNMDPLinkageReferenceData)
// peeks the file's actual magic bytes and only accepts real POI Workbook content (OLE2 or
// OOXML) -- there's no plain-text/CSV input path to fake, and no bundled .xlsx fixture exists
// anywhere in this repo to reuse. So this builds a minimal real .xlsx in the "combined
// haplotype" layout (the 2026 nine-locus release's own format: a header row of
// "Haplotype", <population>, "TotalFreq", then one data row) directly with POI, which is
// already a project dependency.
public class NormalizeFrequencyFileTest {

	@Test
	public void testNormalizeCombinedHaplotypeXlsxToStandardCsv(@TempDir Path tempDir) throws Exception {
		// The "~"-joined name is load-bearing: NormalizeFrequencyFile derives column order from
		// it (deriveLocusPositions), the same convention this project's own real reference file
		// releases use.
		File inputFile = tempDir.resolve("A~C~B.xlsx").toFile();
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet();

			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Haplotype");
			header.createCell(1).setCellValue("CAU");
			header.createCell(2).setCellValue("TotalFreq");

			// No "HLA-" prefix here -- NormalizeFrequencyFile/HLAFrequenciesLoader add that
			// themselves when reconstructing each locus's allele string.
			Row data = sheet.createRow(1);
			data.createCell(0).setCellValue("A*01:01~C*07:01~B*08:01");
			data.createCell(1).setCellValue(0.05);
			data.createCell(2).setCellValue(0.05);

			try (FileOutputStream out = new FileOutputStream(inputFile)) {
				workbook.write(out);
			}
		}

		File outputFile = tempDir.resolve("A~C~B.std.csv").toFile();

		NormalizeFrequencyFile normalizer = new NormalizeFrequencyFile(inputFile, null, outputFile);
		Integer exitCode = normalizer.call();
		assertEquals(0, exitCode);

		List<String> lines = Files.readAllLines(outputFile.toPath());
		assertEquals(1, lines.size(), lines.toString());

		String[] columns = lines.get(0).split(",");
		assertEquals("CAU", columns[0]);
		assertEquals("HLA-A*01:01~HLA-C*07:01~HLA-B*08:01", columns[1]);
		assertEquals("0.05", columns[2]);
		assertEquals("1", columns[3], "the only row for this population should rank 1st");
	}

	// -f single: an individual-locus file. Same real-.xlsx requirement as above --
	// loadIndividualLocusFrequency() peeks magic bytes too. Header column 0 is the locus's own
	// short name; each data row's column 0 is a fully-qualified allele (containing "*", so it's
	// used as-is with just the "HLA-" prefix added, per loadIndividualLocusFrequencyStreaming's
	// own branch for that case).
	@Test
	public void testNormalizeSingleLocusFrequencies(@TempDir Path tempDir) throws Exception {
		File inputFile = tempDir.resolve("individual-locus.xlsx").toFile();
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet();

			sheet.createRow(0).createCell(0).setCellValue("A");
			sheet.createRow(1).createCell(0).setCellValue("A*01:01");
			sheet.createRow(2).createCell(0).setCellValue("A*02:01");

			try (FileOutputStream out = new FileOutputStream(inputFile)) {
				workbook.write(out);
			}
		}

		File outputFile = tempDir.resolve("individual-locus-out.txt").toFile();

		NormalizeFrequencyFile normalizer = new NormalizeFrequencyFile(inputFile, NormalizeFrequencyFile.SINGLE, outputFile);
		Integer exitCode = normalizer.call();
		assertEquals(0, exitCode);

		List<String> lines = Files.readAllLines(outputFile.toPath());
		assertEquals(2, lines.size(), lines.toString());
		assertTrue(lines.contains("HLA-A*01:01"), lines.toString());
		assertTrue(lines.contains("HLA-A*02:01"), lines.toString());
	}
}
