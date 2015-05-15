package org.dash.valid.gl.haplo;

import java.util.HashSet;
import java.util.Set;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.report.DetectedBCDisequilibriumElement;
import org.dash.valid.report.DetectedDisequilibriumElement;

public class BCHaplotype extends Haplotype {
	private Set<String> bAlleles = new HashSet<String>();
	private Set<String> cAlleles = new HashSet<String>();
	private int	bHaplotypeInstance;
	private int cHaplotypeInstance;
	private int[] haplotypeInstances = new int[2];
	
	private int getbHaplotypeInstance() {
		return bHaplotypeInstance;
	}

	private int getcHaplotypeInstance() {
		return cHaplotypeInstance;
	}

	private Set<String> getbAlleles() {
		return bAlleles;
	}

	private Set<String> getcAlleles() {
		return cAlleles;
	}
	
	@Override
	public int getHaplotypeInstance(Locus locus) {
		switch (locus) {
		case HLA_B:
			return getbHaplotypeInstance();
		case HLA_C:
			return getcHaplotypeInstance();
			default:
		return -1;
		}
	}
	
	@Override
	public int[] getHaplotypeInstances() {
		return haplotypeInstances;
	}
	
	@Override
	public Set<String> getAlleles(Locus locus) {
		switch (locus) {
		case HLA_B:
			return getbAlleles();
		case HLA_C:
			return getcAlleles();
		default:
			return null;
		}
	}
	
	@Override
	public Set<String> getAlleles() {
		Set<String> alleles = new HashSet<String>();
		alleles.addAll(bAlleles);
		alleles.addAll(cAlleles);
		
		return alleles;
	}
	
	public BCHaplotype(Set<String> bAlleles, int bHaplotypeInstance, Set<String> cAlleles, int cHaplotypeInstance) {
		this.bAlleles = bAlleles;
		this.bHaplotypeInstance = bHaplotypeInstance;
		this.haplotypeInstances[0] = bHaplotypeInstance;

		this.cAlleles = cAlleles;
		this.cHaplotypeInstance = cHaplotypeInstance;
		this.haplotypeInstances[1] = cHaplotypeInstance;

	}
	
	public BCHaplotype(DetectedDisequilibriumElement foundElement, Haplotype haplotype) {
		this.bAlleles.add(((DetectedBCDisequilibriumElement) foundElement).getHitDegree(Locus.HLA_B).getAllele());
		this.bHaplotypeInstance = haplotype.getHaplotypeInstance(Locus.HLA_B);

		this.cAlleles.add(((DetectedBCDisequilibriumElement) foundElement).getHitDegree(Locus.HLA_C).getAllele());
		this.cHaplotypeInstance = haplotype.getHaplotypeInstance(Locus.HLA_C);
		
		setLinkage(foundElement);
	}
	
	@Override
	public String getHaplotypeString() {
		StringBuffer sb = new StringBuffer();

		if (this.linkage != null) {
			sb.append(linkage.getHitDegree(Locus.HLA_B).getMatchedValue() + GLStringConstants.GENE_DELIMITER + 
					linkage.getHitDegree(Locus.HLA_C).getMatchedValue());
		}
		else {
			sb.append(getbAlleles() + GLStringConstants.GENE_DELIMITER + 
					getcAlleles());
		}
		return sb.toString();
	}
}
