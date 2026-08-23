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
package org.dash.valid.gl.haplo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dash.valid.Locus;
import org.junit.jupiter.api.Test;

// Regression tests for the equals()/hashCode() contract fixed in #13/#121: Haplotype originally
// overrode equals() with three independent asymmetries (order-sensitive List.equals() on a value
// built from unordered HashMap.values(); an asymmetric containsAll allele check instead of mutual
// containment; a linkage null-check that only looked at *this* side), and neither Haplotype nor
// HaplotypePair overrode hashCode() at all despite overriding equals() -- so HashSet<MultiLocusHaplotype>/
// HashSet<HaplotypePair> usages silently fell back to JVM-launch-scoped identity hashCode, breaking
// deduplication in a way that varied run to run on identical input. None of that had a direct unit
// test before now -- confirmed via run-twice-diff on real data at the time, not via the test suite.
public class HaplotypeEqualsHashCodeTest {

	private static MultiLocusHaplotype haplotype(String bAllele, String cAllele, int bInstance, int cInstance) {
		ConcurrentHashMap<Locus, List<String>> alleleMap = new ConcurrentHashMap<Locus, List<String>>();
		alleleMap.put(Locus.HLA_B, list(bAllele));
		alleleMap.put(Locus.HLA_C, list(cAllele));

		HashMap<Locus, Integer> instanceMap = new HashMap<Locus, Integer>();
		instanceMap.put(Locus.HLA_B, bInstance);
		instanceMap.put(Locus.HLA_C, cInstance);

		return new MultiLocusHaplotype(alleleMap, instanceMap, false);
	}

	private static List<String> list(String allele) {
		List<String> alleles = new ArrayList<String>();
		alleles.add(allele);
		return alleles;
	}

	@Test
	public void testEqualsIsReflexiveAndSymmetric() {
		MultiLocusHaplotype a = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);
		MultiLocusHaplotype b = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);

		assertTrue(a.equals(a), "equals() must be reflexive");
		assertTrue(a.equals(b), "two haplotypes with the same allele content must be equal");
		assertTrue(b.equals(a), "equals() must be symmetric");
	}

	@Test
	public void testEqualHaplotypesHaveEqualHashCode() {
		MultiLocusHaplotype a = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);
		MultiLocusHaplotype b = haplotype("HLA-B*07:04", "HLA-C*07:02", 1, 1);

		// Instance numbers differ (would have fed the old, now-removed asymmetric checks),
		// but allele content -- what getHaplotypeString() is built from -- is identical.
		assertTrue(a.equals(b));
		assertEquals(a.hashCode(), b.hashCode(),
				"equal haplotypes must report the same hashCode() -- the exact contract #121 fixed");
	}

	@Test
	public void testDifferentAllelesAreNotEqual() {
		MultiLocusHaplotype a = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);
		MultiLocusHaplotype b = haplotype("HLA-B*44:03", "HLA-C*12:03", 0, 0);

		assertFalse(a.equals(b));
		assertFalse(b.equals(a));
	}

	@Test
	public void testHashSetDeduplicatesEqualHaplotypes() {
		// The practical consequence of the original bug: content-equal haplotypes built as
		// separate objects (as constructPossibleHaplotypes()'s cartesian product does) must
		// collapse to one entry in a HashSet, deterministically, not depending on JVM-launch
		// identity hashCode.
		Set<MultiLocusHaplotype> haplotypes = new HashSet<MultiLocusHaplotype>();
		haplotypes.add(haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0));
		haplotypes.add(haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0));
		haplotypes.add(haplotype("HLA-B*44:03", "HLA-C*12:03", 1, 1));

		assertEquals(2, haplotypes.size(), "HashSet should have deduplicated the two equal haplotypes");
	}

	@Test
	public void testHaplotypePairEqualsIsOrderIndependent() {
		MultiLocusHaplotype a1 = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);
		MultiLocusHaplotype b1 = haplotype("HLA-B*44:03", "HLA-C*12:03", 1, 1);
		HaplotypePair pair1 = new HaplotypePair(a1, b1);

		MultiLocusHaplotype a2 = haplotype("HLA-B*44:03", "HLA-C*12:03", 1, 1);
		MultiLocusHaplotype b2 = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);
		HaplotypePair pair2 = new HaplotypePair(a2, b2);

		assertTrue(pair1.equals(pair2), "HaplotypePair equals() accepts either (hap1,hap2) ordering");
		assertEquals(pair1.hashCode(), pair2.hashCode(),
				"HaplotypePair hashCode() must also be order-independent (sum, not concatenation) -- was missing entirely before #121");
	}

	@Test
	public void testHashSetDeduplicatesEqualHaplotypePairs() {
		MultiLocusHaplotype a1 = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);
		MultiLocusHaplotype b1 = haplotype("HLA-B*44:03", "HLA-C*12:03", 1, 1);

		MultiLocusHaplotype a2 = haplotype("HLA-B*07:04", "HLA-C*07:02", 0, 0);
		MultiLocusHaplotype b2 = haplotype("HLA-B*44:03", "HLA-C*12:03", 1, 1);

		Set<HaplotypePair> pairs = new HashSet<HaplotypePair>();
		pairs.add(new HaplotypePair(a1, b1));
		pairs.add(new HaplotypePair(b2, a2));

		assertEquals(1, pairs.size(), "HashSet should have deduplicated the two equal (reversed-order) pairs");
	}
}
