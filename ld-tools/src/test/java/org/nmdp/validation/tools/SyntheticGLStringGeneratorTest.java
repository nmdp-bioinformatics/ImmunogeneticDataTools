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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Issue #58: the third ld-tools CLI tool had no test coverage at all before this.
//
// Reuses the same bundled five-locus standard-format frequency file HLAFrequenciesLoaderTest
// already relies on -- SyntheticGLStringGenerator's own LOCI constant is fixed to exactly
// Locus.FIVE_LOCUS, so this is real reference data, not a fixture built just for this test.
// Exercises the real generator end to end (a real AntigenRecognitionSiteLoader fetch/cache,
// same as AnalyzeGLStringsTest and friends already do), same rationale as
// syntheticExamples.README.txt documents for how the committed fixture itself was produced.
public class SyntheticGLStringGeneratorTest {

	@Test
	public void testGeneratesSampleCountLinesInTheExpectedFormat(@TempDir Path tempDir) throws Exception {
		URI uri = SyntheticGLStringGeneratorTest.class.getClassLoader()
				.getResource("frequencies/std/NMDP_2007_FiveLocus_Freqs.csv").toURI();
		File frequencyFile = new File(uri);
		File outputFile = tempDir.resolve("synthetic-out.txt").toFile();

		// Same rates the CLI itself defaults to (see SyntheticGLStringGenerator's --help), not
		// picked for this test -- real generation behavior, not a stripped-down special case.
		SyntheticGLStringGenerator generator = new SyntheticGLStringGenerator(
				frequencyFile, outputFile, /* sampleCount */ 10, /* seed */ 42L,
				/* ambiguityRate */ 0.35, /* thresholdExceedingRate */ 0.06, /* genotypeTieRate */ 0.15);

		Integer exitCode = generator.call();
		assertEquals(0, exitCode);

		List<String> lines = Files.readAllLines(outputFile.toPath());
		assertEquals(10, lines.size(), "call() writes exactly sampleCount lines regardless of scenario mix -- "
				+ "a threshold-exceeding sample is still written, just meant to be rejected later by "
				+ "the analyzer, not skipped here");

		for (int i = 0; i < lines.size(); i++) {
			String[] parts = lines.get(i).split("\t");
			assertEquals(2, parts.length, lines.get(i));
			assertEquals("synthetic-" + String.format("%03d", i + 1), parts[0]);

			String glString = parts[1];
			String[] lociSegments = glString.split("\\^");
			assertEquals(5, lociSegments.length, "one segment per Locus.FIVE_LOCUS entry: " + glString);
			assertTrue(glString.contains("HLA-A*"), glString);
			assertTrue(glString.contains("HLA-C*"), glString);
			assertTrue(glString.contains("HLA-B*"), glString);
			assertTrue(glString.contains("HLA-DRB1*"), glString);
			assertTrue(glString.contains("HLA-DQB1*"), glString);
		}
	}

	@Test
	public void testSameSeedProducesIdenticalOutput(@TempDir Path tempDir) throws Exception {
		URI uri = SyntheticGLStringGeneratorTest.class.getClassLoader()
				.getResource("frequencies/std/NMDP_2007_FiveLocus_Freqs.csv").toURI();
		File frequencyFile = new File(uri);

		File firstRun = tempDir.resolve("first.txt").toFile();
		File secondRun = tempDir.resolve("second.txt").toFile();

		new SyntheticGLStringGenerator(frequencyFile, firstRun, 5, 7L, 0.35, 0.06, 0.15).call();
		new SyntheticGLStringGenerator(frequencyFile, secondRun, 5, 7L, 0.35, 0.06, 0.15).call();

		assertEquals(Files.readString(firstRun.toPath()), Files.readString(secondRun.toPath()),
				"the --seed argument exists specifically for reproducible output");
	}
}
