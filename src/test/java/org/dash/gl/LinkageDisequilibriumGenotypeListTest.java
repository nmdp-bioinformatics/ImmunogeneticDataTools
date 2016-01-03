package org.dash.gl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.junit.Test;

public class LinkageDisequilibriumGenotypeListTest extends TestCase {
	private static final String B0702 = "HLA-B*07:02";
	private static final String B4402 = "HLA-B*44:02";
	private static final String C0702 = "HLA-C*07:02";
	private static final String C1203 = "HLA-C*12:03";
	private static final String TEST_BC_PAIRS = B0702 + GLStringConstants.GENE_COPY_DELIMITER + B4402 + GLStringConstants.GENE_DELIMITER + 
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
				if ((!genotypeList.checkHomozygous(Locus.HLA_B) &&
						haplotype1.getAlleles(Locus.HLA_B).containsAll(haplotype2.getAlleles(Locus.HLA_B))) ||
						(!genotypeList.checkHomozygous(Locus.HLA_C) &&
						haplotype1.getAlleles(Locus.HLA_C).containsAll(haplotype2.getAlleles(Locus.HLA_C)))) {
					continue;
				}

				HaplotypePair haplotypePair = new HaplotypePair(haplotype1, haplotype2);
				linkedPairs.add(haplotypePair);
			}
		}
		
		for (HaplotypePair pair : linkedPairs) {
			Haplotype haplotype1 = pair.getHaplotype1();
			assertTrue(haplotype1 instanceof MultiLocusHaplotype);
			
			Haplotype haplotype2 = pair.getHaplotype2();
			assertTrue(haplotype2 instanceof MultiLocusHaplotype);
			
			assertTrue((haplotype1.getAlleles(Locus.HLA_B).contains(B0702) && haplotype2.getAlleles(Locus.HLA_B).contains(B4402)) ||
					(haplotype1.getAlleles(Locus.HLA_B).contains(B4402) && haplotype2.getAlleles(Locus.HLA_B).contains(B0702)));
			
			assertTrue((haplotype1.getAlleles(Locus.HLA_C).contains(C0702) && haplotype2.getAlleles(Locus.HLA_C).contains(C1203)) ||
					(haplotype1.getAlleles(Locus.HLA_C).contains(C1203) && haplotype2.getAlleles(Locus.HLA_C).contains(C0702)));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_B).contains(B0702) && haplotype2.getAlleles(Locus.HLA_B).contains(B0702));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_B).contains(B4402) && haplotype2.getAlleles(Locus.HLA_B).contains(B4402));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_C).contains(C0702) && haplotype2.getAlleles(Locus.HLA_C).contains(C0702));
			
			assertFalse(haplotype1.getAlleles(Locus.HLA_C).contains(C1203) && haplotype2.getAlleles(Locus.HLA_C).contains(C1203));
		}
	}
}
