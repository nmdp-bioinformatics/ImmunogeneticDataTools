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
package org.dash.valid.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dash.valid.Locus;
import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRace;
import org.junit.jupiter.api.Test;

// Two groups of tests. The first two tests are a regression test for a real user-reported bug: analyze-gl-strings against stanfordExamplesFixed.txt
// produced relative-frequency values > 100 in summary.xml (391 of 10173, all exactly
// 100.00000000000001). Root cause, confirmed independently in Python before fixing here: for any
// double x, (x * 100.0) / x is NOT guaranteed to round-trip to exactly 100.0 -- the multiply and
// divide are two separately-rounded IEEE-754 operations, so this happens for a meaningful fraction
// of possible x values (~6.5% each direction in a random sample), even in the simplest case (a race
// with only one candidate haplotype pair, so that pair's own frequency *is* totalFreq). Not a logic
// bug -- a single candidate's relative frequency against its own total must be exactly 100.0 -- just
// floating-point noise of about 1 ULP, landing on either side of 100.0 with similar likelihood.
// DetectedLinkageFindings#setLinkedPairs now divides before multiplying by 100.0 instead of after,
// which (confirmed the same way) lands on exactly 100.0 every time for this case, in addition to the
// Math.min(100.0, ...) backstop for the general (non-degenerate) case. The rest of the class pins
// the relative-frequency normalisation across candidate pairs and getMinimumDifference.
public class DetectedLinkageFindingsTest {

	// Found via random searches in Python: (x * 100.0) / x landed just over 100.0 for one value and
	// just under for the other, reproducing both directions of the artifact seen in the user's real
	// output -- 100.00000000000001 was the exact value found in summary.xml; the under-100 value is
	// the same phenomenon in the other direction, not itself something seen in that file.
	private static final double FREQUENCY_THAT_ROUNDED_OVER_100_PRE_FIX = 0.43276707357738264;
	private static final double FREQUENCY_THAT_ROUNDED_UNDER_100_PRE_FIX = 0.7637746213388679;

	// Tolerance for the multi-candidate shares, which are not exactly representable.
	private static final double DELTA = 1e-9;

	@Test
	public void testRelativeFrequencyIsExactly100ForASingleCandidatePairThatPreFixRoundedOver() {
		assertEquals(100.0, relativeFrequencyForSingleCandidatePair(FREQUENCY_THAT_ROUNDED_OVER_100_PRE_FIX));
	}

	@Test
	public void testRelativeFrequencyIsExactly100ForASingleCandidatePairThatPreFixRoundedUnder() {
		assertEquals(100.0, relativeFrequencyForSingleCandidatePair(FREQUENCY_THAT_ROUNDED_UNDER_100_PRE_FIX));
	}

	private static double relativeFrequencyForSingleCandidatePair(double frequency) {
		MultiLocusHaplotype hap1 = haplotype("HLA-B*07:04", "HLA-C*07:02", withFrequency(1.0));
		MultiLocusHaplotype hap2 = haplotype("HLA-B*44:03", "HLA-C*12:03", withFrequency(frequency));

		HaplotypePair pair = new HaplotypePair(hap1, hap2);
		assertTrue(pair.isByRace());

		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
		linkedPairs.add(pair);

		DetectedLinkageFindings findings = new DetectedLinkageFindings();
		findings.setLinkedPairs(linkedPairs);

		RelativeFrequencyByRace relativeFrequencyByRace = pair.getFrequencies().iterator().next();
		return relativeFrequencyByRace.getRelativeFrequency();
	}

	private static MultiLocusHaplotype haplotype(String bAllele, String cAllele, List<FrequencyByRace> frequenciesByRace) {
		Map<Locus, String> alleles = new LinkedHashMap<Locus, String>();
		alleles.put(Locus.HLA_B, bAllele);
		alleles.put(Locus.HLA_C, cAllele);

		return haplotype(alleles, frequenciesByRace);
	}

	private static MultiLocusHaplotype haplotype(Map<Locus, String> alleles, List<FrequencyByRace> frequenciesByRace) {
		ConcurrentHashMap<Locus, List<String>> alleleMap = new ConcurrentHashMap<Locus, List<String>>();
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		for (Map.Entry<Locus, String> allele : alleles.entrySet()) {
			alleleMap.put(allele.getKey(), list(allele.getValue()));
			hlaElementMap.put(allele.getKey(), list(allele.getValue()));
		}

		MultiLocusHaplotype haplotype = new MultiLocusHaplotype(alleleMap, new HashMap<Locus, Integer>(), false);

		DisequilibriumElementByRace disequilibriumElementByRace = new DisequilibriumElementByRace(hlaElementMap, frequenciesByRace);
		haplotype.setLinkage(new DetectedDisequilibriumElement(disequilibriumElementByRace));

		return haplotype;
	}

	private static List<FrequencyByRace> withFrequency(double frequency) {
		List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
		frequenciesByRace.add(new FrequencyByRace(frequency, "1", "CAU"));
		return frequenciesByRace;
	}

	private static List<String> list(String allele) {
		List<String> alleles = new ArrayList<String>();
		alleles.add(allele);
		return alleles;
	}

	// ---- Relative-frequency normalisation across candidate pairs ----
	//
	// A pair's relative frequency is its share (in percent) of the summed frequency of every
	// candidate pair found for the same locus combination, computed separately for each race.
	// Before these tests nothing asserted a relative frequency other than the single-candidate
	// 100.0 above, so forcing the value to 100 for every pair passed the whole module, and
	// getMinimumDifference was never called.

	// Builds a pair on the given loci whose frequency in each listed race is exactly the given
	// value: hap1 carries 1.0 in every race, so the pair frequency (hap1 * hap2) is hap2's value.
	// Alleles are made unique by id so that no two pairs in a test are equal.
	private static HaplotypePair pairByRace(int id, Object... raceAndFrequency) {
		return pairOnLoci(id, false, raceAndFrequency);
	}

	private static HaplotypePair threeLocusPairByRace(int id, Object... raceAndFrequency) {
		return pairOnLoci(id, true, raceAndFrequency);
	}

	private static HaplotypePair pairOnLoci(int id, boolean threeLoci, Object... raceAndFrequency) {
		List<FrequencyByRace> hap1Frequencies = new ArrayList<FrequencyByRace>();
		List<FrequencyByRace> hap2Frequencies = new ArrayList<FrequencyByRace>();
		for (int i = 0; i < raceAndFrequency.length; i += 2) {
			hap1Frequencies.add(new FrequencyByRace(1.0, "1", (String) raceAndFrequency[i]));
			hap2Frequencies.add(new FrequencyByRace((Double) raceAndFrequency[i + 1], "1", (String) raceAndFrequency[i]));
		}

		return new HaplotypePair(haplotype(id, 1, threeLoci, hap1Frequencies), haplotype(id, 2, threeLoci, hap2Frequencies));
	}

	private static MultiLocusHaplotype haplotype(int id, int copy, boolean threeLoci, List<FrequencyByRace> frequenciesByRace) {
		Map<Locus, String> alleles = new LinkedHashMap<Locus, String>();
		alleles.put(Locus.HLA_B, "HLA-B*" + id + ":0" + copy);
		alleles.put(Locus.HLA_C, "HLA-C*" + id + ":0" + copy);
		if (threeLoci) {
			alleles.put(Locus.HLA_A, "HLA-A*" + id + ":0" + copy);
		}

		return haplotype(alleles, frequenciesByRace);
	}

	private static DetectedLinkageFindings findingsFor(HaplotypePair... pairs) {
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
		for (HaplotypePair pair : pairs) {
			linkedPairs.add(pair);
		}

		DetectedLinkageFindings findings = new DetectedLinkageFindings();
		findings.setLinkedPairs(linkedPairs);

		return findings;
	}

	private static double relativeFrequency(HaplotypePair pair, String race) {
		for (RelativeFrequencyByRace relativeFrequency : pair.getFrequencies()) {
			if (race.equals(relativeFrequency.getRace())) {
				return relativeFrequency.getRelativeFrequency();
			}
		}

		throw new AssertionError("pair has no frequency for race " + race);
	}

	@Test
	public void testRelativeFrequencyIsEachPairsShareOfTheRacesTotal() {
		HaplotypePair likelier = pairByRace(1, "CAU", 0.75);
		HaplotypePair lessLikely = pairByRace(2, "CAU", 0.25);

		findingsFor(likelier, lessLikely);

		assertEquals(75.0, relativeFrequency(likelier, "CAU"), DELTA);
		assertEquals(25.0, relativeFrequency(lessLikely, "CAU"), DELTA);
	}

	// Real reference frequencies are tiny, so the total of a race is nowhere near 1.0.
	@Test
	public void testRelativeFrequencyIsDividedByTheRacesActualTotal() {
		HaplotypePair likelier = pairByRace(1, "CAU", 0.0003);
		HaplotypePair lessLikely = pairByRace(2, "CAU", 0.0001);

		findingsFor(likelier, lessLikely);

		assertEquals(75.0, relativeFrequency(likelier, "CAU"), DELTA);
		assertEquals(25.0, relativeFrequency(lessLikely, "CAU"), DELTA);
	}

	@Test
	public void testRelativeFrequencyIsNormalisedSeparatelyForEachRace() {
		HaplotypePair first = pairByRace(1, "CAU", 0.75, "AFA", 0.25);
		HaplotypePair second = pairByRace(2, "CAU", 0.25, "AFA", 0.75);

		findingsFor(first, second);

		assertEquals(75.0, relativeFrequency(first, "CAU"), DELTA);
		assertEquals(25.0, relativeFrequency(first, "AFA"), DELTA);
		assertEquals(25.0, relativeFrequency(second, "CAU"), DELTA);
		assertEquals(75.0, relativeFrequency(second, "AFA"), DELTA);
	}

	// "AFA" has only one candidate, so that candidate is the whole of the race even though the
	// other pair is more frequent in "CAU".
	@Test
	public void testRaceWithASingleCandidateIsOneHundredPercentEvenWhenOtherRacesHaveMore() {
		HaplotypePair both = pairByRace(1, "CAU", 0.75, "AFA", 0.01);
		HaplotypePair caucasianOnly = pairByRace(2, "CAU", 0.25);

		findingsFor(both, caucasianOnly);

		assertEquals(75.0, relativeFrequency(both, "CAU"), DELTA);
		assertEquals(100.0, relativeFrequency(both, "AFA"), DELTA);
		assertEquals(25.0, relativeFrequency(caucasianOnly, "CAU"), DELTA);
	}

	// A pair whose two haplotypes share no race has no frequency to be a share of: it is dropped from
	// the findings and the others are normalised as if it were not there. (It cannot leak into their
	// totals either way: HaplotypePair builds no per-race frequency for it, so there is nothing to add.)
	@Test
	public void testPairWithNoRaceInCommonIsDropped() {
		HaplotypePair likelier = pairByRace(1, "CAU", 0.75);
		HaplotypePair lessLikely = pairByRace(2, "CAU", 0.25);
		HaplotypePair noOverlap = new HaplotypePair(
				haplotype(3, 1, false, withFrequency(1.0)),
				haplotype(3, 2, false, Collections.singletonList(new FrequencyByRace(0.9, "1", "AFA"))));

		DetectedLinkageFindings findings = findingsFor(likelier, lessLikely, noOverlap);

		assertEquals(2, findings.getLinkedPairs().size());
		assertTrue(findings.getLinkedPairs().contains(likelier));
		assertTrue(findings.getLinkedPairs().contains(lessLikely));
		assertEquals(75.0, relativeFrequency(likelier, "CAU"), DELTA);
		assertEquals(25.0, relativeFrequency(lessLikely, "CAU"), DELTA);
	}

	// Totals are kept per locus combination: a C~B pair is not a competitor of an A~C~B pair.
	@Test
	public void testPairsOnDifferentLocusCombinationsAreNotNormalisedAgainstEachOther() {
		HaplotypePair twoLocus = pairByRace(1, "CAU", 0.75);
		HaplotypePair threeLocus = threeLocusPairByRace(2, "CAU", 0.25);

		DetectedLinkageFindings findings = findingsFor(twoLocus, threeLocus);

		assertEquals(100.0, relativeFrequency(twoLocus, "CAU"), DELTA);
		assertEquals(100.0, relativeFrequency(threeLocus, "CAU"), DELTA);
		assertEquals(100.0, findings.getMinimumDifference(Locus.C_B_LOCI), DELTA);
		assertEquals(100.0, findings.getMinimumDifference(Locus.A_C_B_LOCI), DELTA);
	}

	@Test
	public void testLinkedPairsAreOrderedWithTheMostLikelyFirst() {
		HaplotypePair least = pairByRace(1, "CAU", 0.1);
		HaplotypePair most = pairByRace(2, "CAU", 0.6);
		HaplotypePair middle = pairByRace(3, "CAU", 0.3);

		DetectedLinkageFindings findings = findingsFor(least, most, middle);

		List<HaplotypePair> ordered = new ArrayList<HaplotypePair>(findings.getLinkedPairs());
		assertEquals(3, ordered.size());
		assertSame(most, ordered.get(0));
		assertSame(middle, ordered.get(1));
		assertSame(least, ordered.get(2));

		assertTrue(findings.hasLinkedPairs(Locus.C_B_LOCI));
		assertSame(most, findings.getFirstPair(Locus.C_B_LOCI));
	}

	// ---- getMinimumDifference ----
	//
	// The smallest gap between two candidates' relative frequencies in a race, taken over the
	// races (reported in the detected-findings CSV/XLSX so a reader can tell how decisive the
	// top candidate is). A race with a single candidate has no gap, so it reports that
	// candidate's own relative frequency (100).

	@Test
	public void testMinimumDifferenceFindsTheClosestPairOfCandidates() {
		// 50, 40, 10 -> the closest pair is 40 and 50 (10), not the first two (10 and 40: 30).
		DetectedLinkageFindings findings = findingsFor(
				pairByRace(1, "CAU", 0.5), pairByRace(2, "CAU", 0.4), pairByRace(3, "CAU", 0.1));

		assertEquals(10.0, findings.getMinimumDifference(Locus.C_B_LOCI), DELTA);
	}

	// 5, 20, 25, 50 -> gaps of 15, 5 and 25: the smallest is neither the first nor the last gap.
	@Test
	public void testMinimumDifferenceIsTheSmallestGapWhenItIsInTheMiddle() {
		DetectedLinkageFindings findings = findingsFor(
				pairByRace(1, "CAU", 0.05), pairByRace(2, "CAU", 0.20), pairByRace(3, "CAU", 0.25), pairByRace(4, "CAU", 0.50));

		assertEquals(5.0, findings.getMinimumDifference(Locus.C_B_LOCI), DELTA);
	}

	@Test
	public void testMinimumDifferenceOfTwoEquallyLikelyCandidatesIsZero() {
		DetectedLinkageFindings findings = findingsFor(pairByRace(1, "CAU", 0.25), pairByRace(2, "CAU", 0.25));

		assertEquals(0.0, findings.getMinimumDifference(Locus.C_B_LOCI), DELTA);
	}

	@Test
	public void testMinimumDifferenceOfTwoCandidatesIsTheirGap() {
		DetectedLinkageFindings findings = findingsFor(pairByRace(1, "CAU", 0.75), pairByRace(2, "CAU", 0.25));

		assertEquals(50.0, findings.getMinimumDifference(Locus.C_B_LOCI), DELTA);
	}

	@Test
	public void testMinimumDifferenceOfASingleCandidateIsItsRelativeFrequency() {
		DetectedLinkageFindings findings = findingsFor(pairByRace(1, "CAU", 0.75));

		assertEquals(100.0, findings.getMinimumDifference(Locus.C_B_LOCI), DELTA);
	}

	// CAU has a gap of 50, AFA a gap of 20, and "ASN" a single candidate (100): the smallest wins.
	@Test
	public void testMinimumDifferenceIsTheSmallestAcrossRaces() {
		DetectedLinkageFindings findings = findingsFor(
				pairByRace(1, "CAU", 0.75, "AFA", 0.6, "ASN", 0.5),
				pairByRace(2, "CAU", 0.25, "AFA", 0.4));

		assertEquals(20.0, findings.getMinimumDifference(Locus.C_B_LOCI), DELTA);
	}

	@Test
	public void testMinimumDifferenceIsNullForALocusCombinationWithNoPairs() {
		DetectedLinkageFindings findings = findingsFor(pairByRace(1, "CAU", 0.75));

		assertNull(findings.getMinimumDifference(Locus.A_C_B_LOCI));
	}

	// ---- Reference data that is not stratified by race ----

	private static HaplotypePair pairWithoutRace(int id, String hap1Frequency, String hap2Frequency) {
		return new HaplotypePair(nonRaceHaplotype(id, 1, hap1Frequency), nonRaceHaplotype(id, 2, hap2Frequency));
	}

	private static MultiLocusHaplotype nonRaceHaplotype(int id, int copy, String frequency) {
		ConcurrentHashMap<Locus, List<String>> alleleMap = new ConcurrentHashMap<Locus, List<String>>();
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		for (Locus locus : new Locus[] { Locus.HLA_B, Locus.HLA_C }) {
			String allele = locus.getShortName() + "*" + id + ":0" + copy;
			alleleMap.put(locus, list(allele));
			hlaElementMap.put(locus, list(allele));
		}

		MultiLocusHaplotype haplotype = new MultiLocusHaplotype(alleleMap, new HashMap<Locus, Integer>(), false);
		haplotype.setLinkage(new DetectedDisequilibriumElement(new BaseDisequilibriumElement(hlaElementMap, frequency, "")));

		return haplotype;
	}

	// Without race there is nothing to normalise: the pairs are kept as given, flagged as linked, and
	// carry their own "freq1*freq2" string; there is no relative frequency, hence no minimum difference.
	@Test
	public void testPairsWithoutRaceAreKeptAndNotNormalised() {
		HaplotypePair pair = pairWithoutRace(1, "0.1", "0.2");
		assertFalse(pair.isByRace());

		DetectedLinkageFindings findings = findingsFor(pair);

		assertEquals(1, findings.getLinkedPairs().size());
		assertSame(pair, findings.getFirstPair(Locus.C_B_LOCI));
		assertTrue(findings.hasLinkedPairs(Locus.C_B_LOCI));
		assertEquals("0.1*0.2", pair.getFrequency());
		assertNull(findings.getMinimumDifference(Locus.C_B_LOCI));
	}
}
