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
package org.dash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.dash.valid.HLALinkageDisequilibrium;
import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
import org.dash.valid.Locus;
import org.dash.valid.Sample;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.report.DetectedDisequilibriumElement;
import org.dash.valid.report.DetectedLinkageFindings;
import org.junit.jupiter.api.Test;

public class LinkageDisequilibriumAnalyzerTest {	
	@Test
	public void testLinkageReportingExamples() {
		LinkagesLoader.getInstance(Linkages.lookup(Locus.C_B_LOCI));
		LinkageDisequilibriumAnalyzer.main(new String[] {"contrivedExamples.txt", "fullyQualifiedExample.txt", "strictExample.txt", "hml_1_0_2-example7-ngsFull.xml", "shorthandExamples.txt"});
	}
	
//	@Test
//	public void testLinkageReportingMugs() throws IOException {
//		List<LinkageDisequilibriumGenotypeList> glStrings = GLStringUtilities.readGLStringFile("fullyQualifiedExample.txt");
//		
//		for (LinkageDisequilibriumGenotypeList linkedGLString : glStrings) {
//			MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(linkedGLString.getGLString());
//			
//			assertNotNull(mug);
//			
//			Sample sample = LinkageDisequilibriumAnalyzer.detectLinkages(mug);
//			
//			assertNotNull(sample);
//		}
//	}
	
	// This genotype is fully phased via "~" chains with zero "^" characters (per the published
	// GL String spec, Milius et al., PMC3715123, that's legal). Before #120, parseGLString()
	// derived the locus once per outer "^"-delimited block instead of once per inner phased
	// segment, so an all-"~"-no-"^" genotype like this one silently collapsed every gene's
	// alleles onto whichever locus was named first (HLA-A here) -- no error, just every other
	// locus left empty. Only assertNotNull(sample) was checked before, which the bug still
	// passed; asserting each locus actually got its own allele(s) is what would have caught it.
	@Test
	public void testPhasedGenotypeList() throws IOException {
		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.NMDP.getShortName());
		String fullyQualified = GLStringUtilities.fullyQualifyGLString("HLA-A*24:02:01:01~HLA-C*04:01:01:06~HLA-B*35:02:01~HLA-DRB3*02:02:01:02~HLA-DRB1*11:01:01:01~HLA-DQA1*05:05:01:01/HLA-DQA1*05:05:01:02~HLA-DQB1*03:01:01:03~HLA-DPA1*01:03:01:01~HLA-DPB1*05:01:01+HLA-A*11:01:01:01~HLA-C*12:03:01:01~HLA-B*35:03:01~HLA-DRB3*02:02:01:01~HLA-DRB1*13:01:01:01/HLA-DRB1*13:01:01:02~HLA-DQA1*01:03:01:02~HLA-DQB1*06:03:01~HLA-DPA1*02:01:01:01~HLA-DPB1*13:01:01/HLA-DPB1*107:01");

		LinkageDisequilibriumGenotypeList glString = new LinkageDisequilibriumGenotypeList("SBCFMW0003", fullyQualified);

		// The bug's exact symptom: every one of these 7 non-first loci ended up with an empty
		// allele list, with everything piled onto HLA_A instead. (DRB3 normalizes to the
		// combined HLA_DRB345 pseudo-locus, so that's what's checked here, not HLA_DRB3 itself.)
		for (Locus locus : new Locus[] {Locus.HLA_C, Locus.HLA_B, Locus.HLA_DRB345, Locus.HLA_DRB1,
				Locus.HLA_DQA1, Locus.HLA_DQB1, Locus.HLA_DPA1, Locus.HLA_DPB1}) {
			assertFalse(glString.getAlleles(locus).isEmpty(),
					"Expected locus " + locus + " to have its own allele(s), not be left empty by mis-bucketed phasing");
		}

		// HLA_A itself should have exactly the 2 phased copies' worth of alleles (one entry per
		// "+"-separated copy), not every gene's alleles piled on top of it.
		assertEquals(2, glString.getAlleles(Locus.HLA_A).size(),
				"Expected HLA_A to have exactly 2 allele-ambiguity groups (one per phased copy), not every locus's alleles collapsed onto it");

		List<Haplotype> knownHaplotypes = GLStringUtilities.buildHaplotypes(glString);

		Sample sample = HLALinkageDisequilibrium.hasLinkageDisequilibrium(glString, knownHaplotypes);

		assertNotNull(sample);
	}
	
	@Test
	public void testLinkageReportingInlineGLString() throws IOException {
		String fullyQualified = GLStringUtilities.fullyQualifyGLString("HLA-A*11:01:01+HLA-A*24:02:01:01/HLA-A*24:02:01:02L/HLA-A*24:02:01:03^HLA-B*18:01:01:01/HLA-B*18:01:01:02/HLA-B*18:51+HLA-B*53:01:01^HLA-C*04:01:01:01/HLA-C*04:01:01:02/HLA-C*04:01:01:03/HLA-C*04:01:01:04/HLA-C*04:01:01:05/HLA-C*04:20/HLA-C*04:117+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34^HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05+HLA-DPA1*02:01:01^HLA-DPB1*02:01:02+HLA-DPB1*09:01^HLA-DQA1*01:02:01:01/HLA-DQA1*01:02:01:02/HLA-DQA1*01:02:01:03/HLA-DQA1*01:02:01:04/HLA-DQA1*01:11+HLA-DQA1*03:01:01^HLA-DQB1*03:05:01+HLA-DQB1*06:09^HLA-DRB1*11:04:01+HLA-DRB1*13:02:01^HLA-DRB3*02:02:01:01/HLA-DRB3*02:02:01:02+HLA-DRB3*03:01:01");

		LinkageDisequilibriumGenotypeList glString = new LinkageDisequilibriumGenotypeList("fullyQualified", fullyQualified);
		Sample sample = LinkageDisequilibriumAnalyzer.detectLinkages(glString);

		assertNotNull(sample);
	}

	// Regression test for the non-determinism found while verifying #124 (perf fix): multiple
	// raw allele candidates enumerated from this genotype's own "|" ambiguity at HLA-B
	// (52:01:01:01/52:01:01:02 vs 52:07, both under G-group B*52:01g) tie against the same
	// reference row for HLA-C*12:02~HLA-B*52:01g. findLinkedPairs()'s own tie-break (fixed in
	// #122) picks the deterministic winner correctly and consistently -- but a separate,
	// parallel structure (detectedDisequilibriumElements -> findings.linkages, a TreeSet keyed
	// by a comparator that treats any two matches of the same reference row as equal) relied on
	// DetectedDisequilibriumElement's default (identity-based) hashCode(), since it overrides
	// equals() without overriding hashCode() -- a genuine contract violation. That meant the
	// <linkage> report section could report a *different* fullValue for this same G-group than
	// the <haplo-pair> section reported, and which one varied across separate JVM launches
	// (confirmed via 8 repeated CLI runs on real data landing in one of two states). Both
	// sections must agree, deterministically, in every run -- this only checks one JVM's worth,
	// since default hashCode() is stable within a single process, but it does lock in the
	// invariant the bug actually violated.
	@Test
	public void testTiedGGroupCandidateReportedConsistently() throws IOException, ReflectiveOperationException {
		// LinkagesLoader/HLAFrequenciesLoader are process-wide singletons whose getInstance()
		// is a no-op once already initialized -- and other tests in this same forked JVM
		// initialize them first (this file's own class-level "TODO: Write tests" on
		// LinkagesLoader is a pre-existing gap, not introduced here). Reset both via
		// reflection so this test's expected reference data is actually the data in effect,
		// regardless of what ran before it.
		resetSingleton(LinkagesLoader.class);
		resetSingleton(org.dash.valid.freq.HLAFrequenciesLoader.class);

		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.NMDP_2007_STD.getShortName());
		LinkagesLoader.getInstance(Linkages.lookup(Locus.C_B_LOCI));

		String fullyQualified = GLStringUtilities.fullyQualifyGLString(
				"HLA-B*52:01:01:01/52:01:01:02+HLA-B*56:01:01/56:26|52:07+HLA-B*56:26^HLA-C*01:02:01+HLA-C*12:02:02");
		LinkageDisequilibriumGenotypeList glString = new LinkageDisequilibriumGenotypeList("tiedGGroup", fullyQualified);

		Sample sample = LinkageDisequilibriumAnalyzer.detectLinkages(glString);
		DetectedLinkageFindings findings = sample.getFindings();

		String tiedValue = "HLA-C*12:02~HLA-B*52:01g";
		// The two candidates the genotype's own ambiguity produces for this G-group; the
		// lexicographically smaller one is the deterministic winner (see #122).
		String expectedFullValue = "HLA-C*12:02:02~HLA-B*52:01:01:01/HLA-B*52:01:01:02";

		DetectedDisequilibriumElement matchingLinkage = null;
		for (DetectedDisequilibriumElement linkage : findings.getLinkages()) {
			if (tiedValue.equals(linkage.getHaplotype().getHaplotypeString())) {
				matchingLinkage = linkage;
				break;
			}
		}
		assertNotNull(matchingLinkage, "Expected a <linkage> entry for " + tiedValue);
		assertEquals(expectedFullValue, matchingLinkage.getHaplotype().getFullHaplotypeString(),
				"findings.getLinkages() reported an unexpected fullValue for the tied G-group");

		boolean foundMatchingPairHaplotype = false;
		for (HaplotypePair pair : findings.getLinkedPairs()) {
			for (Haplotype haplotype : pair.getHaplotypes()) {
				if (tiedValue.equals(haplotype.getHaplotypeString())) {
					assertEquals(expectedFullValue, haplotype.getFullHaplotypeString(),
							"findings.getLinkedPairs() disagreed with findings.getLinkages() on the tied G-group's fullValue");
					foundMatchingPairHaplotype = true;
				}
			}
		}
		assertTrue(foundMatchingPairHaplotype, "Expected a haplotype pair referencing " + tiedValue);
	}

	private static void resetSingleton(Class<?> clazz) throws ReflectiveOperationException {
		java.lang.reflect.Field instanceField = clazz.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, null);
	}
}
