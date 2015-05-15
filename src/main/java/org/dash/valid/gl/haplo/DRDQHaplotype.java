package org.dash.valid.gl.haplo;

import java.util.HashSet;
import java.util.Set;

import org.dash.valid.Locus;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.report.DetectedDRDQDisequilibriumElement;
import org.dash.valid.report.DetectedDisequilibriumElement;

public class DRDQHaplotype extends Haplotype {
	private Set<String> drb1Alleles = new HashSet<String>();
	private Set<String> drb345Alleles = new HashSet<String>();
	private Set<String> dqa1Alleles = new HashSet<String>();
	private Set<String> dqb1Alleles = new HashSet<String>();
	private int	drb1HaplotypeInstance;
	private int drb345HaplotypeInstance;
	private int dqb1HaplotypeInstance;
	private int dqa1HaplotypeInstance;
	private int[] haplotypeInstances = new int[4];
	
	public static final boolean EXPECTING_DQA1;
	
	static {
		EXPECTING_DQA1 = Frequencies.WIKIVERSITY.getShortName().equals(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
	}
	
	private int getDrb1HaplotypeInstance() {
		return drb1HaplotypeInstance;
	}

	private int getDrb345HaplotypeInstance() {
		return drb345HaplotypeInstance;
	}

	private int getDqb1HaplotypeInstance() {
		return dqb1HaplotypeInstance;
	}

	private int getDqa1HaplotypeInstance() {
		return dqa1HaplotypeInstance;
	}
	
	@Override
	public int[] getHaplotypeInstances() {
		return haplotypeInstances;
	}
	
	@Override
	public int getHaplotypeInstance(Locus locus) {
		switch (locus) {
		case HLA_DRB1:
			return getDrb1HaplotypeInstance();
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRB345:
		case HLA_DRBX:
			return getDrb345HaplotypeInstance();
		case HLA_DQB1:
			return getDqb1HaplotypeInstance();
		case HLA_DQA1:
			return getDqa1HaplotypeInstance();
		default:
			return -1;
		}
	}
	
	private Set<String> getDrb1Alleles() {
		return drb1Alleles;
	}

	private Set<String> getDrb345Alleles() {
		return drb345Alleles;
	}

	private Set<String> getDqa1Alleles() {
		return dqa1Alleles;
	}

	private Set<String> getDqb1Alleles() {
		return dqb1Alleles;
	}
	
	@Override
	public Set<String> getAlleles() {
		Set<String> alleles = new HashSet<String>();
		alleles.addAll(drb1Alleles);
		alleles.addAll(drb345Alleles);
		alleles.addAll(dqb1Alleles);
		alleles.addAll(dqa1Alleles);
		
		return alleles;
	}
	
	@Override
	public Set<String> getAlleles(Locus locus) {
		switch (locus) {
		case HLA_DRB1:
			return getDrb1Alleles();
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRB345:
		case HLA_DRBX:
			return getDrb345Alleles();
		case HLA_DQB1:
			return getDqb1Alleles();
		case HLA_DQA1:
			return getDqa1Alleles();
		default:
			return null;
		}
	}

	public DRDQHaplotype(Set<String> drb1Alleles, int drb1HaplotypeInstance,
			Set<String> drb345Alleles, int drb345HaplotypeInstance,
			Set<String> dqb1Alleles, int dqb1HaplotypeInstance,
			Set<String> dqa1Alleles, int dqa1HaplotypeInstance) {
		this.drb1Alleles.addAll(drb1Alleles);
		this.drb1HaplotypeInstance = drb1HaplotypeInstance;
		haplotypeInstances[0] = this.drb1HaplotypeInstance;
		
		this.drb345Alleles.addAll(drb345Alleles);
		this.drb345HaplotypeInstance = drb345HaplotypeInstance;
		haplotypeInstances[1] = this.drb345HaplotypeInstance;
		
		this.dqb1Alleles.addAll(dqb1Alleles);
		this.dqb1HaplotypeInstance = dqb1HaplotypeInstance;
		haplotypeInstances[2] = this.dqb1HaplotypeInstance;
		
		this.dqa1Alleles.addAll(dqa1Alleles);
		this.dqa1HaplotypeInstance = dqa1HaplotypeInstance;
		haplotypeInstances[3] = this.dqa1HaplotypeInstance;
	}
	
	public DRDQHaplotype(DetectedDisequilibriumElement foundElement, Haplotype haplotype) {		
		this.drb1Alleles.add(((DetectedDRDQDisequilibriumElement) foundElement).getHitDegree(Locus.HLA_DRB1).getAllele());
		this.drb1HaplotypeInstance = haplotype.getHaplotypeInstance(Locus.HLA_DRB1);
		haplotypeInstances[0] = this.drb1HaplotypeInstance;
		
		this.drb345Alleles.add(((DetectedDRDQDisequilibriumElement) foundElement).getHitDegree(Locus.HLA_DRB345).getAllele());
		this.drb345HaplotypeInstance = haplotype.getHaplotypeInstance(Locus.HLA_DRB345);
		haplotypeInstances[1] = this.drb345HaplotypeInstance;

		this.dqb1Alleles.add(((DetectedDRDQDisequilibriumElement) foundElement).getHitDegree(Locus.HLA_DQB1).getAllele());
		this.dqb1HaplotypeInstance = haplotype.getHaplotypeInstance(Locus.HLA_DQB1);
		haplotypeInstances[2] = this.dqb1HaplotypeInstance;

		if (EXPECTING_DQA1) {
			this.dqa1Alleles.add(((DetectedDRDQDisequilibriumElement) foundElement).getHitDegree(Locus.HLA_DQA1).getAllele());
			this.dqa1HaplotypeInstance = haplotype.getHaplotypeInstance(Locus.HLA_DQA1);
			haplotypeInstances[3] = this.dqa1HaplotypeInstance;
		}
		setLinkage(foundElement);
	}
	
	@Override
	public String getHaplotypeString() {
		StringBuffer sb = new StringBuffer();
		
		if (this.linkage != null) {
			sb.append(linkage.getHitDegree(Locus.HLA_DRB1).getMatchedValue() + GLStringConstants.GENE_DELIMITER + 
					linkage.getHitDegree(Locus.HLA_DRB345).getMatchedValue() + GLStringConstants.GENE_DELIMITER + 
					linkage.getHitDegree(Locus.HLA_DQB1).getMatchedValue());
			if (EXPECTING_DQA1) {
				sb.append(GLStringConstants.GENE_DELIMITER + linkage.getHitDegree(Locus.HLA_DQA1).getMatchedValue());
			}
		}
		else {	
			sb.append(getDrb1Alleles() + GLStringConstants.GENE_DELIMITER + 
						getDrb345Alleles() + GLStringConstants.GENE_DELIMITER + 
						getDqb1Alleles());
			if (EXPECTING_DQA1) {
				sb.append(GLStringConstants.GENE_DELIMITER + getDqa1Alleles());
			}
		}
		return sb.toString();
	}
}
