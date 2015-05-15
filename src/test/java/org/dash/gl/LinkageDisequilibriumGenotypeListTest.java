package org.dash.gl;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.BCHaplotype;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.junit.Test;

public class LinkageDisequilibriumGenotypeListTest extends TestCase {
	private static final String B0702 = "HLA-B*07:02";
	private static final String B4402 = "HLA-B*44:02";
	private static final String C0702 = "HLA-C*07:02";
	private static final String C1203 = "HLA-C*12:03";
	private static final String TEST_BC_PAIRS = B0702 + GLStringConstants.GENE_COPY_DELIMITER + B4402 + GLStringConstants.GENE_DELIMITER + 
												C0702 + GLStringConstants.GENE_COPY_DELIMITER + C1203;

	@Test
	public void testHaplotypePairs() {	
		LinkageDisequilibriumGenotypeList genotypeList = new LinkageDisequilibriumGenotypeList("HaplotypePairs", TEST_BC_PAIRS);
		
		Set<BCHaplotype> possibleHaplotypes = genotypeList.getPossibleBCHaplotypes();
		
		Set<HaplotypePair> linkedPairs = new HashSet<HaplotypePair>();
		
		for (Haplotype haplotype1 : possibleHaplotypes) {	
			for (Haplotype haplotype2 : possibleHaplotypes) {
				if ((!genotypeList.checkHomozygous(Locus.HLA_B) && 
					GLStringUtilities.checkFromSameHaplotype(Locus.HLA_B, haplotype1, haplotype2)) ||
					(!genotypeList.checkHomozygous(Locus.HLA_C) &&
					GLStringUtilities.checkFromSameHaplotype(Locus.HLA_C, haplotype1, haplotype2))) {
						continue;
				}
				linkedPairs.add(new HaplotypePair(haplotype1, haplotype2));
			}
		}
		
		for (HaplotypePair pair : linkedPairs) {
			Haplotype haplotype1 = pair.getHaplotype1();
			assertTrue(haplotype1 instanceof BCHaplotype);
			
			Haplotype haplotype2 = pair.getHaplotype2();
			assertTrue(haplotype2 instanceof BCHaplotype);
			
			assertTrue((((BCHaplotype) haplotype1).getAlleles(Locus.HLA_B).contains(B0702) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_B).contains(B4402)) ||
					((BCHaplotype) haplotype1).getAlleles(Locus.HLA_B).contains(B4402) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_B).contains(B0702));
			
			assertTrue((((BCHaplotype) haplotype1).getAlleles(Locus.HLA_C).contains(C0702) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_C).contains(C1203)) ||
					((BCHaplotype) haplotype1).getAlleles(Locus.HLA_C).contains(C1203) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_C).contains(C0702));
			
			assertFalse((((BCHaplotype) haplotype1).getAlleles(Locus.HLA_B).contains(B0702) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_B).contains(B0702)));
			
			assertFalse((((BCHaplotype) haplotype1).getAlleles(Locus.HLA_B).contains(B4402) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_B).contains(B4402)));
			
			assertFalse((((BCHaplotype) haplotype1).getAlleles(Locus.HLA_C).contains(C0702) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_C).contains(C0702)));
			
			assertFalse((((BCHaplotype) haplotype1).getAlleles(Locus.HLA_C).contains(C1203) && ((BCHaplotype) haplotype2).getAlleles(Locus.HLA_C).contains(C1203)));
		}
	}
}
