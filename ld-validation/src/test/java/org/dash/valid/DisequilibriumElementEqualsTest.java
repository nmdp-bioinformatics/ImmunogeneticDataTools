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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.junit.jupiter.api.Test;

// DisequilibriumElement#equals() has one special case: when a genotype is homozygous at DRB3/4/5
// (so it carries one DRB345 allele, not two), the "no DRB345 gene" placeholder ("-" or "NNNN") in a
// reference row at the DRB345 locus is allowed to stand in for the missing allele. The condition
// was written as `isDRB345(locus) && (clauseA) || (clauseB)`, so && bound tighter and clauseB --
// the receiver's own haplotype is DRB345-homozygous and the argument holds a placeholder -- applied
// at EVERY locus, letting a placeholder at e.g. HLA-B match any B allele. DisequilibriumElementIndex
// (the production fast path) restricts the special case to DRB345 loci, which is the intended
// behaviour; DisequilibriumElementIndexTest uses equals() as its oracle, so it could not see this.
// The table below covers both directions, both placeholders, and DRB345 vs. non-DRB345 loci.
public class DisequilibriumElementEqualsTest {

	private static final String B_ALLELE = "HLA-B*07:02";
	private static final String DRB345_ALLELE = "HLA-DRB3*01:01";
	private static final String[] PLACEHOLDERS = {GLStringConstants.DASH, GLStringConstants.NNNN};

	private static List<String> list(String allele) {
		List<String> alleles = new ArrayList<String>();
		alleles.add(allele);
		return alleles;
	}

	// An element with an HLA-B and an HLA-DRB345 allele; a null haplotype stands for a reference
	// row (which carries none), otherwise the haplotype's drb345Homozygous flag is set as given.
	private static DisequilibriumElement element(String bAllele, String drb345Allele, Boolean drb345Homozygous) {
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		hlaElementMap.put(Locus.HLA_B, list(bAllele));
		hlaElementMap.put(Locus.HLA_DRB345, list(drb345Allele));

		DisequilibriumElement element = new BaseDisequilibriumElement(hlaElementMap, "0.001", "");

		if (drb345Homozygous != null) {
			ConcurrentHashMap<Locus, List<String>> alleleMap = new ConcurrentHashMap<Locus, List<String>>();
			alleleMap.put(Locus.HLA_B, list(bAllele));
			alleleMap.put(Locus.HLA_DRB345, list(drb345Allele));

			HashMap<Locus, Integer> instanceMap = new HashMap<Locus, Integer>();
			instanceMap.put(Locus.HLA_B, 0);
			instanceMap.put(Locus.HLA_DRB345, 0);

			element.setHaplotype(new MultiLocusHaplotype(alleleMap, instanceMap, drb345Homozygous));
		}

		return element;
	}

	@Test
	public void testSameAllelesMatch() {
		assertTrue(element(B_ALLELE, DRB345_ALLELE, null).equals(element(B_ALLELE, DRB345_ALLELE, null)));
	}

	@Test
	public void testDifferentBAlleleDoesNotMatch() {
		assertFalse(element(B_ALLELE, DRB345_ALLELE, null).equals(element("HLA-B*52:01", DRB345_ALLELE, null)));
	}

	@Test
	public void testPlaceholderInReceiverMatchesWhenArgumentIsDrb345Homozygous() {
		for (String placeholder : PLACEHOLDERS) {
			DisequilibriumElement referenceRow = element(B_ALLELE, placeholder, null);
			DisequilibriumElement query = element(B_ALLELE, DRB345_ALLELE, true);

			assertTrue(referenceRow.equals(query), "placeholder " + placeholder + " should stand in for a homozygous DRB345 query");
		}
	}

	@Test
	public void testPlaceholderInReceiverDoesNotMatchWhenArgumentIsNotDrb345Homozygous() {
		for (String placeholder : PLACEHOLDERS) {
			DisequilibriumElement referenceRow = element(B_ALLELE, placeholder, null);

			assertFalse(referenceRow.equals(element(B_ALLELE, DRB345_ALLELE, false)), "placeholder " + placeholder + " vs. heterozygous haplotype");
			assertFalse(referenceRow.equals(element(B_ALLELE, DRB345_ALLELE, null)), "placeholder " + placeholder + " vs. no haplotype");
		}
	}

	@Test
	public void testPlaceholderInArgumentMatchesWhenReceiverIsDrb345Homozygous() {
		for (String placeholder : PLACEHOLDERS) {
			DisequilibriumElement query = element(B_ALLELE, DRB345_ALLELE, true);
			DisequilibriumElement referenceRow = element(B_ALLELE, placeholder, null);

			assertTrue(query.equals(referenceRow), "placeholder " + placeholder + " should stand in for a homozygous DRB345 query");
		}
	}

	@Test
	public void testPlaceholderInArgumentDoesNotMatchWhenReceiverIsNotDrb345Homozygous() {
		for (String placeholder : PLACEHOLDERS) {
			DisequilibriumElement referenceRow = element(B_ALLELE, placeholder, null);

			assertFalse(element(B_ALLELE, DRB345_ALLELE, false).equals(referenceRow), "placeholder " + placeholder + " vs. heterozygous haplotype");
			assertFalse(element(B_ALLELE, DRB345_ALLELE, null).equals(referenceRow), "placeholder " + placeholder + " vs. no haplotype");
		}
	}

	// The bug: a DRB345-homozygous receiver matched a placeholder at a locus that is not DRB345.
	@Test
	public void testPlaceholderAtNonDrb345LocusNeverMatchesInArgument() {
		for (String placeholder : PLACEHOLDERS) {
			DisequilibriumElement query = element(B_ALLELE, DRB345_ALLELE, true);
			DisequilibriumElement referenceRow = element(placeholder, DRB345_ALLELE, null);

			assertFalse(query.equals(referenceRow), "placeholder " + placeholder + " at HLA-B must not match a B allele");
		}
	}

	@Test
	public void testPlaceholderAtNonDrb345LocusNeverMatchesInReceiver() {
		for (String placeholder : PLACEHOLDERS) {
			DisequilibriumElement referenceRow = element(placeholder, DRB345_ALLELE, null);
			DisequilibriumElement query = element(B_ALLELE, DRB345_ALLELE, true);

			assertFalse(referenceRow.equals(query), "placeholder " + placeholder + " at HLA-B must not match a B allele");
		}
	}
}
