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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dash.valid.Locus;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRace;
import org.junit.jupiter.api.Test;

// Regression test for a real user-reported bug: analyze-gl-strings against stanfordExamplesFixed.txt
// produced relative-frequency values > 100 in summary.xml (391 of 10173, all exactly
// 100.00000000000001). Root cause, confirmed independently in Python before fixing here: for any
// double x, (x * 100.0) / x is NOT guaranteed to round-trip to exactly 100.0 -- the multiply and
// divide are two separately-rounded IEEE-754 operations, so this happens for a meaningful fraction
// of possible x values (~6.5% in a random sample), even in the simplest case (a race with only one
// candidate haplotype pair, so that pair's own frequency *is* totalFreq). Not a logic bug -- a
// relativeFrequency can never legitimately exceed the total it's a share of -- just floating-point
// noise on the order of 1 ULP that DetectedLinkageFindings#setLinkedPairs now clamps away.
public class DetectedLinkageFindingsTest {

	// Found via a random search in Python: (x * 100.0) / x == 100.00000000000001 for this x,
	// reproducing the exact artifact seen in the user's real output.
	private static final double FREQUENCY_THAT_ROUNDS_OVER_100_PRE_FIX = 0.43276707357738264;

	@Test
	public void testRelativeFrequencyIsClampedTo100ForASingleCandidatePair() {
		MultiLocusHaplotype hap1 = haplotype("HLA-B*07:04", "HLA-C*07:02", withFrequency(1.0));
		MultiLocusHaplotype hap2 = haplotype("HLA-B*44:03", "HLA-C*12:03", withFrequency(FREQUENCY_THAT_ROUNDS_OVER_100_PRE_FIX));

		HaplotypePair pair = new HaplotypePair(hap1, hap2);
		assertTrue(pair.isByRace());

		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
		linkedPairs.add(pair);

		DetectedLinkageFindings findings = new DetectedLinkageFindings();
		findings.setLinkedPairs(linkedPairs);

		RelativeFrequencyByRace relativeFrequencyByRace = pair.getFrequencies().iterator().next();

		// Pre-fix, this pair's own (and only) contribution to its race's total came back as
		// 100.00000000000001 -- reproduced independently above and confirmed to still reproduce
		// without the Math.min(100.0, ...) clamp in DetectedLinkageFindings#setLinkedPairs.
		assertEquals(100.0, relativeFrequencyByRace.getRelativeFrequency(),
				"a single candidate pair is its race's entire total, so its relative frequency must be exactly 100.0, "
				+ "not a hair over it from floating-point rounding");
	}

	private static MultiLocusHaplotype haplotype(String bAllele, String cAllele, List<FrequencyByRace> frequenciesByRace) {
		ConcurrentHashMap<Locus, List<String>> alleleMap = new ConcurrentHashMap<Locus, List<String>>();
		alleleMap.put(Locus.HLA_B, list(bAllele));
		alleleMap.put(Locus.HLA_C, list(cAllele));

		MultiLocusHaplotype haplotype = new MultiLocusHaplotype(alleleMap, new HashMap<Locus, Integer>(), false);

		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		hlaElementMap.put(Locus.HLA_B, list(bAllele));
		hlaElementMap.put(Locus.HLA_C, list(cAllele));

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
}
