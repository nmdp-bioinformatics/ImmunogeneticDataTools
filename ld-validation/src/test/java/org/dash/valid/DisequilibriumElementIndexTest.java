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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.dash.valid.ars.AntigenRecognitionSiteLoader;
import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.junit.jupiter.api.Test;

// DisequilibriumElementIndex replaces a manual List#contains()/indexOf() linear scan (see
// HLALinkageDisequilibrium) with an index, specifically so it can be fast against a reference
// list with 900K+ rows -- but DisequilibriumElement#equals() itself does fuzzy matching (colon-
// field prefix truncation OR ARS/G-group membership per locus, not exact string comparison), so
// the risk here isn't "did the copy-and-scan style change" but "does the index still find every
// match the old brute-force scan would have, and nothing else." Every test below compares the
// index's result against bruteForceMatches() -- a deliberately trivial, obviously-correct linear
// filter using the same, unmodified equals() -- rather than asserting expected values by hand,
// so what's actually being verified is "the index agrees with the ground truth," not "the index
// agrees with what I assumed."
public class DisequilibriumElementIndexTest {

	@Test
	public void testExactMatch() {
		DisequilibriumElement reference = referenceRow(Locus.HLA_A, "HLA-A*01:01:01:01");
		DisequilibriumElement query = queryElement(Locus.HLA_A, "HLA-A*01:01:01:01");

		assertIndexAgreesWithBruteForce(Arrays.asList(reference), query);
		assertTrue(DisequilibriumElementIndex.forElements(Arrays.asList(reference)).findMatches(query).contains(reference));
	}

	@Test
	public void testFieldTruncationMatchWhenQueryIsShorterThanReference() {
		// Query has fewer fields than the reference row -- fieldLevelComparison truncates to the
		// query's own (shorter) length, so this must still match.
		DisequilibriumElement reference = referenceRow(Locus.HLA_A, "HLA-A*01:01:01:01");
		DisequilibriumElement query = queryElement(Locus.HLA_A, "HLA-A*01:01");

		assertIndexAgreesWithBruteForce(Arrays.asList(reference), query);
		assertTrue(DisequilibriumElementIndex.forElements(Arrays.asList(reference)).findMatches(query).contains(reference));
	}

	@Test
	public void testFieldTruncationMatchWhenQueryIsLongerThanReference() {
		// Reverse of the above -- reference row has fewer fields, so the comparison truncates
		// down to *its* length instead.
		DisequilibriumElement reference = referenceRow(Locus.HLA_A, "HLA-A*01:01");
		DisequilibriumElement query = queryElement(Locus.HLA_A, "HLA-A*01:01:05:01");

		assertIndexAgreesWithBruteForce(Arrays.asList(reference), query);
		assertTrue(DisequilibriumElementIndex.forElements(Arrays.asList(reference)).findMatches(query).contains(reference));
	}

	@Test
	public void testGenuineNonMatch() {
		// No field-level overlap at all -- must not match, proving the index doesn't over-match.
		DisequilibriumElement reference = referenceRow(Locus.HLA_A, "HLA-A*02:01");
		DisequilibriumElement query = queryElement(Locus.HLA_A, "HLA-A*01:01");

		assertIndexAgreesWithBruteForce(Arrays.asList(reference), query);
		assertTrue(DisequilibriumElementIndex.forElements(Arrays.asList(reference)).findMatches(query).isEmpty());
	}

	@Test
	public void testMultiLocusRequiresAllLociToMatch() {
		// Matches at HLA_B but not HLA_C -- the pair must not be reported as an overall match,
		// same as equals()'s per-locus AND.
		HashMap<Locus, List<String>> refMap = new HashMap<Locus, List<String>>();
		refMap.put(Locus.HLA_B, list("HLA-B*07:02"));
		refMap.put(Locus.HLA_C, list("HLA-C*07:02"));
		DisequilibriumElement reference = new BaseDisequilibriumElement(refMap, "1", "note");

		HashMap<Locus, List<String>> queryMap = new HashMap<Locus, List<String>>();
		queryMap.put(Locus.HLA_B, list("HLA-B*07:02"));
		queryMap.put(Locus.HLA_C, list("HLA-C*04:01"));
		DisequilibriumElement query = new CoreDisequilibriumElement(queryMap, haplotype());

		assertIndexAgreesWithBruteForce(Arrays.asList(reference), query);
		assertTrue(DisequilibriumElementIndex.forElements(Arrays.asList(reference)).findMatches(query).isEmpty());
	}

	@Test
	public void testMultipleTiedMatchesAreAllFound() {
		// Two distinct reference rows both matching the same (shorter) query -- findLinkedPairs'
		// tie-break logic depends on finding *all* of them, not just the first encountered.
		DisequilibriumElement reference1 = referenceRow(Locus.HLA_A, "HLA-A*01:01:01:01");
		DisequilibriumElement reference2 = referenceRow(Locus.HLA_A, "HLA-A*01:01:01:02");
		DisequilibriumElement query = queryElement(Locus.HLA_A, "HLA-A*01:01");

		List<DisequilibriumElement> elements = Arrays.asList(reference1, reference2);
		assertIndexAgreesWithBruteForce(elements, query);

		List<DisequilibriumElement> matches = DisequilibriumElementIndex.forElements(elements).findMatches(query);
		assertTrue(matches.contains(reference1));
		assertTrue(matches.contains(reference2));
		assertEquals(2, matches.size());
	}

	@Test
	public void testDrb345HomozygousPlaceholderMatch() {
		// A DRB345-homozygous query (no DRB3/4/5 gene at all) must match a reference row whose
		// DRB345 slot is the "-" placeholder, even though the strings share no field-level prefix
		// and aren't ARS-related -- the special case in equals() this exercises.
		HashMap<Locus, List<String>> refMap = new HashMap<Locus, List<String>>();
		refMap.put(Locus.HLA_DRB345, list(org.dash.valid.gl.GLStringConstants.DASH));
		DisequilibriumElement reference = new BaseDisequilibriumElement(refMap, "1", "note");

		HashMap<Locus, List<String>> queryMap = new HashMap<Locus, List<String>>();
		queryMap.put(Locus.HLA_DRB345, list("HLA-DRB3*01:01:02:01"));
		MultiLocusHaplotype homozygousHaplotype = haplotype();
		homozygousHaplotype.setDRB345Homozygous(true);
		DisequilibriumElement query = new CoreDisequilibriumElement(queryMap, homozygousHaplotype);

		assertIndexAgreesWithBruteForce(Arrays.asList(reference), query);
		assertTrue(DisequilibriumElementIndex.forElements(Arrays.asList(reference)).findMatches(query).contains(reference));
	}

	@Test
	public void testArsOnlyMatchAgreesWithBruteForce() throws IOException, InvalidFormatException {
		// Finds a real example, from the actually-loaded ARS reference data, of a query allele
		// that matches a reference allele *only* via ARS/G-group membership -- i.e. one with no
		// field-level prefix overlap at all -- rather than hand-picking a specific allele pair
		// that could quietly go stale as IMGT/HLA data changes. Skips (rather than fails) if none
		// is found, since that would say more about data/network availability in this environment
		// than about the index itself.
		HashMap<String, HashSet<String>> arsMap = AntigenRecognitionSiteLoader.getInstance().getArsMap();

		String referenceAllele = null;
		String queryAllele = null;
		outer:
		for (Map.Entry<String, HashSet<String>> entry : arsMap.entrySet()) {
			for (String code : entry.getValue()) {
				if (!org.dash.valid.gl.GLStringUtilities.fieldLevelComparison(code, entry.getKey())) {
					referenceAllele = entry.getKey();
					queryAllele = code;
					break outer;
				}
			}
		}

		assumeTrue(referenceAllele != null, "Could not find a real ARS-only example pair in the loaded reference data");

		DisequilibriumElement reference = referenceRow(Locus.HLA_A, referenceAllele);
		DisequilibriumElement query = queryElement(Locus.HLA_A, queryAllele);

		assertIndexAgreesWithBruteForce(Arrays.asList(reference), query);
		assertTrue(DisequilibriumElementIndex.forElements(Arrays.asList(reference)).findMatches(query).contains(reference),
				"query " + queryAllele + " should have matched " + referenceAllele + " via ARS membership");
	}

	@Test
	public void testAgainstALargerMixedReferenceList() {
		// A broader sanity check with more rows and more variety (matching, non-matching, and
		// differing field counts at the same locus) than the single-row cases above, still
		// verified against the same brute-force oracle rather than hand-asserted expectations.
		List<DisequilibriumElement> elements = new ArrayList<DisequilibriumElement>();
		elements.add(referenceRow(Locus.HLA_A, "HLA-A*01:01:01:01"));
		elements.add(referenceRow(Locus.HLA_A, "HLA-A*02:01:01:01"));
		elements.add(referenceRow(Locus.HLA_A, "HLA-A*03:01"));
		elements.add(referenceRow(Locus.HLA_A, "HLA-A*01:02"));
		elements.add(referenceRow(Locus.HLA_A, "HLA-A*11:01:01:01"));

		assertIndexAgreesWithBruteForce(elements, queryElement(Locus.HLA_A, "HLA-A*01:01"));
		assertIndexAgreesWithBruteForce(elements, queryElement(Locus.HLA_A, "HLA-A*03:01:01:01"));
		assertIndexAgreesWithBruteForce(elements, queryElement(Locus.HLA_A, "HLA-A*24:02"));
	}

	private static void assertIndexAgreesWithBruteForce(List<DisequilibriumElement> elements, DisequilibriumElement query) {
		List<DisequilibriumElement> indexed = DisequilibriumElementIndex.forElements(elements).findMatches(query);
		List<DisequilibriumElement> bruteForce = bruteForceMatches(elements, query);

		assertEquals(bruteForce, indexed,
				"indexed lookup must find exactly the same matches, in the same order, as a plain linear equals() scan");
	}

	// The oracle: exactly what the old code was doing conceptually (find every element the query
	// equals(), in list order) -- obviously correct by inspection, since it's just equals() called
	// directly against every candidate with no indexing involved at all.
	private static List<DisequilibriumElement> bruteForceMatches(List<DisequilibriumElement> elements, DisequilibriumElement query) {
		List<DisequilibriumElement> matches = new ArrayList<DisequilibriumElement>();
		for (DisequilibriumElement element : elements) {
			if (query.equals(element)) {
				matches.add(element);
			}
		}
		return matches;
	}

	private static DisequilibriumElement referenceRow(Locus locus, String allele) {
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		hlaElementMap.put(locus, list(allele));
		return new BaseDisequilibriumElement(hlaElementMap, "1", "note");
	}

	private static DisequilibriumElement queryElement(Locus locus, String allele) {
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		hlaElementMap.put(locus, list(allele));
		return new CoreDisequilibriumElement(hlaElementMap, haplotype());
	}

	private static MultiLocusHaplotype haplotype() {
		return new MultiLocusHaplotype(new java.util.concurrent.ConcurrentHashMap<Locus, List<String>>(), new HashMap<Locus, Integer>(), false);
	}

	private static List<String> list(String allele) {
		List<String> alleles = new ArrayList<String>();
		alleles.add(allele);
		return alleles;
	}
}
