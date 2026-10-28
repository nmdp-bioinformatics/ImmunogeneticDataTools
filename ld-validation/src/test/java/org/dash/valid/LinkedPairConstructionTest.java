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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Pins down which HaplotypePairs HLALinkageDisequilibrium#findLinkedPairs builds from a small, hand-made
// two-locus (C~B) reference file, so the expected pairs are known by construction rather than compared
// against another implementation. The pairing loop's rule: two linked haplotypes form a pair when, at
// every locus, they come from different copies of the genotype -- or the genotype is homozygous there,
// in which case the two copies are indistinguishable and the instance check is skipped. Before this test
// nothing asserted an expected pair at all (the pair-producing tests only checked sizes > 0 or inspected
// their own re-implementation of the predicate), so removing the homozygous check altogether passed every
// test in the module.
public class LinkedPairConstructionTest {

	@TempDir
	Path tempDir;

	@BeforeEach
	@AfterEach
	public void resetLoaders() {
		LinkagesLoader.reset();
		HLAFrequenciesLoader.reset();
	}

	private Set<HaplotypePair> linkedPairs(String frequencyRows, String glString) throws IOException {
		Path frequencyFile = tempDir.resolve("cbFreqs.csv");
		Files.write(frequencyFile, frequencyRows.getBytes(StandardCharsets.UTF_8));

		LinkagesLoader.reset();
		HLAFrequenciesLoader.reset();
		HLAFrequenciesLoader.getInstance(Collections.singleton(frequencyFile.toFile()), (File) null);

		Sample sample = HLALinkageDisequilibrium.hasLinkageDisequilibrium(new LinkageDisequilibriumGenotypeList("test", glString));

		return sample.getFindings().getLinkedPairs();
	}

	private static String pairString(HaplotypePair pair) {
		List<org.dash.valid.gl.haplo.Haplotype> haplotypes = pair.getHaplotypes();
		return haplotypes.get(0).getHaplotypeString() + " | " + haplotypes.get(1).getHaplotypeString();
	}

	private static final String HET_C = "HLA-C*01:02:01+HLA-C*07:01:01:01";
	private static final String HET_B = "HLA-B*08:01:01:01+HLA-B*07:02:01";
	private static final String HOM_B = "HLA-B*08:01:01:01+HLA-B*08:01:01:01";
	private static final String HOM_C = "HLA-C*01:02:01+HLA-C*01:02:01";

	// Both haplotypes get the same race: HaplotypePairs whose two haplotypes share no race are dropped
	// by DetectedLinkageFindings#setLinkedPairs, which would hide the pair this test is looking for.
	private static final String C1_B1 = "HIS,HLA-C*01:02~HLA-B*08:01g,1.0E-4\n";
	private static final String C2_B2 = "HIS,HLA-C*07:01~HLA-B*07:02,6.0E-5\n";
	private static final String C1_B2 = "HIS,HLA-C*01:02~HLA-B*07:02,1.0E-4\n";
	private static final String C2_B1 = "HIS,HLA-C*07:01~HLA-B*08:01g,6.0E-5\n";
	private static final String C2_B1_SAME_B = "HIS,HLA-C*07:01~HLA-B*08:01g,6.0E-5\n";

	private void assertPairs(Set<HaplotypePair> pairs, String... expected) {
		List<String> actual = new ArrayList<String>();
		for (HaplotypePair pair : pairs) {
			actual.add(pairString(pair));
		}
		Collections.sort(actual);

		assertEquals(Arrays.asList(expected), actual);
	}

	@Test
	public void testHeterozygousGenotypeWithBothHaplotypesLinkedGivesOnePair() throws IOException {
		assertPairs(linkedPairs(C1_B1 + C2_B2, HET_C + "^" + HET_B),
				"HLA-C*01:02~HLA-B*08:01g | HLA-C*07:01~HLA-B*07:02");
	}

	@Test
	public void testOnlyOneHaplotypeLinkedGivesNoPair() throws IOException {
		assertPairs(linkedPairs(C1_B1, HET_C + "^" + HET_B));
	}

	// C1~B2 is a possible haplotype of this genotype, but its complement C2~B1 is not in the reference.
	@Test
	public void testOnlyAPhasingsFirstHaplotypeLinkedGivesNoPair() throws IOException {
		assertPairs(linkedPairs(C1_B2, HET_C + "^" + HET_B));
	}

	// Unphased: the genotype supports both C1~B1 + C2~B2 and C1~B2 + C2~B1; the reference decides which.
	@Test
	public void testPairFollowsTheReferencePhasing() throws IOException {
		assertPairs(linkedPairs(C1_B2 + C2_B1, HET_C + "^" + HET_B),
				"HLA-C*01:02~HLA-B*07:02 | HLA-C*07:01~HLA-B*08:01g");
	}

	// Homozygous B: both copies carry B*08:01, so the two haplotypes cannot be told apart by which B copy
	// they came from -- the pair has to be allowed without a different-copy check at B.
	@Test
	public void testHomozygousLocusStillPairsHaplotypesFromDifferentCopiesAtTheOtherLocus() throws IOException {
		assertPairs(linkedPairs(C1_B1 + C2_B1_SAME_B, HET_C + "^" + HOM_B),
				"HLA-C*01:02~HLA-B*08:01g | HLA-C*07:01~HLA-B*08:01g");
	}

	@Test
	public void testHomozygousLocusWithOnlyOneLinkedHaplotypeGivesNoPair() throws IOException {
		assertPairs(linkedPairs(C1_B1, HET_C + "^" + HOM_B));
	}

	@Test
	public void testHomozygousFirstLocusPairsTheTwoPhasingsOfTheOtherLocus() throws IOException {
		assertPairs(linkedPairs(C1_B1 + C1_B2, HOM_C + "^" + HET_B),
				"HLA-C*01:02~HLA-B*07:02 | HLA-C*01:02~HLA-B*08:01g");
	}

	@Test
	public void testHomozygousAtEveryLocusPairsTheHaplotypeWithItself() throws IOException {
		assertPairs(linkedPairs(C1_B1, HOM_C + "^" + HOM_B),
				"HLA-C*01:02~HLA-B*08:01g | HLA-C*01:02~HLA-B*08:01g");
	}
}
