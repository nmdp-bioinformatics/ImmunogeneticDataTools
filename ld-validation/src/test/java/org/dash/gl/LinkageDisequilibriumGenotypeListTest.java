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
package org.dash.gl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.dash.valid.Locus;
import org.dash.valid.gl.AmbiguousGenotypeException;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.junit.jupiter.api.Test;

public class LinkageDisequilibriumGenotypeListTest {
	private static final String B0704 = "HLA-B*07:04";
	private static final String B4403 = "HLA-B*44:03";
	private static final String C0702 = "HLA-C*07:02";
	private static final String C1203 = "HLA-C*12:03";
	private static final String TEST_BC_PAIRS = B0704 + GLStringConstants.GENE_COPY_DELIMITER + B4403 + GLStringConstants.GENE_DELIMITER + 
												C0702 + GLStringConstants.GENE_COPY_DELIMITER + C1203;

	@Test
	public void testHaplotypePairs() throws IOException {	
		LinkageDisequilibriumGenotypeList genotypeList = new LinkageDisequilibriumGenotypeList("HaplotypePairs", TEST_BC_PAIRS);
		
		Set<Locus> loci = new HashSet<Locus>();
		loci.add(Locus.HLA_B);
		loci.add(Locus.HLA_C);
		
		Set<MultiLocusHaplotype> possibleHaplotypes = genotypeList.constructPossibleHaplotypes(loci);
						
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
		
		for (Haplotype haplotype1 : possibleHaplotypes) {	
			for (Haplotype haplotype2 : possibleHaplotypes) {
				if ((!genotypeList.hasHomozygous(Locus.HLA_B) &&
						haplotype1.getAlleles(Locus.HLA_B).containsAll(haplotype2.getAlleles(Locus.HLA_B))) ||
						(!genotypeList.hasHomozygous(Locus.HLA_C) &&
						haplotype1.getAlleles(Locus.HLA_C).containsAll(haplotype2.getAlleles(Locus.HLA_C)))) {
					continue;
				}

				HaplotypePair haplotypePair = new HaplotypePair(haplotype1, haplotype2);
				linkedPairs.add(haplotypePair);
			}
		}
		
		for (HaplotypePair pair : linkedPairs) {
			Haplotype haplotype1 = pair.getHaplotypes().get(0);
			assertTrue(haplotype1 instanceof MultiLocusHaplotype);
			
			Haplotype haplotype2 = pair.getHaplotypes().get(1);
			assertTrue(haplotype2 instanceof MultiLocusHaplotype);
			
			assertTrue((haplotype1.getAlleles(Locus.HLA_B).contains(B0704) && haplotype2.getAlleles(Locus.HLA_B).contains(B4403)) ||
					(haplotype1.getAlleles(Locus.HLA_B).contains(B4403) && haplotype2.getAlleles(Locus.HLA_B).contains(B0704)));
			
			assertTrue((haplotype1.getAlleles(Locus.HLA_C).contains(C0702) && haplotype2.getAlleles(Locus.HLA_C).contains(C1203)) ||
					(haplotype1.getAlleles(Locus.HLA_C).contains(C1203) && haplotype2.getAlleles(Locus.HLA_C).contains(C0702)));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_B).contains(B0704) && haplotype2.getAlleles(Locus.HLA_B).contains(B0704));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_B).contains(B4403) && haplotype2.getAlleles(Locus.HLA_B).contains(B4403));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_C).contains(C0702) && haplotype2.getAlleles(Locus.HLA_C).contains(C0702));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_C).contains(C1203) && haplotype2.getAlleles(Locus.HLA_C).contains(C1203));
		}
	}

	// Regression tests for #120: constructPossibleHaplotypes() cartesian-products every
	// ambiguous allele across every locus, which can reach into the billions (and effectively
	// hang) for a pathologically ambiguous genotype -- checkAmbiguityThresholds() is supposed
	// to reject those up front instead. Neither the rejection itself nor its below-threshold
	// non-interference had a direct test before now; the fix was previously verified only by
	// timing a real 32,757-character line from a user-supplied file, not by the test suite.
	@Test
	public void testAmbiguityThresholdThrows() throws ReflectiveOperationException {
		// LinkagesLoader is a process-wide singleton whose getInstance() is a no-op once
		// already initialized, and other tests in this same forked JVM may initialize it
		// first with a Linkages set that doesn't cover HLA_B. Reset it so this test's
		// assumption (some loaded linkage set includes HLA_B) actually holds regardless of
		// what ran before it.
		java.lang.reflect.Field instanceField = org.dash.valid.LinkagesLoader.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, null);
		org.dash.valid.LinkagesLoader.getInstance(org.dash.valid.Linkages.lookup(org.dash.valid.Locus.C_B_LOCI));

		StringBuilder alleleAmbiguities = new StringBuilder("HLA-B*07:01");
		for (int i = 2; i <= 21; i++) {
			alleleAmbiguities.append(GLStringConstants.ALLELE_AMBIGUITY_DELIMITER)
					.append(String.format("HLA-B*07:%02d", i));
		}
		String glString = "HLA-A*01:01" + GLStringConstants.GENE_DELIMITER + alleleAmbiguities;

		// The constructor itself eagerly calls constructPossibleHaplotypes() for whichever
		// Linkages set(s) are currently loaded, so with 21 ambiguous alleles at HLA_B,
		// construction itself is expected to throw -- this is also a more realistic test of
		// the actual rejection path (GLStringUtilities' batch-file processing catches exactly
		// this exception per-line).
		assertThrows(AmbiguousGenotypeException.class,
				() -> new LinkageDisequilibriumGenotypeList("AmbiguityThreshold", glString),
				"Expected constructing a genotype with 21 ambiguous alleles at one locus to be rejected");
	}

	@Test
	public void testBelowAmbiguityThresholdDoesNotThrow() {
		LinkageDisequilibriumGenotypeList genotypeList = new LinkageDisequilibriumGenotypeList("BelowThreshold", TEST_BC_PAIRS);

		Set<Locus> loci = new HashSet<Locus>();
		loci.add(Locus.HLA_B);
		loci.add(Locus.HLA_C);

		// Well under the threshold -- asserts the check isn't over-eager (e.g. counting
		// something other than actual allele ambiguity count) and doesn't regress the
		// ordinary, non-pathological case.
		Set<MultiLocusHaplotype> possibleHaplotypes = genotypeList.constructPossibleHaplotypes(loci);
		assertTrue(possibleHaplotypes.size() > 0);
	}
}
