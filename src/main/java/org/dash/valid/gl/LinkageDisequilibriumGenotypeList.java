package org.dash.valid.gl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.Locus;
import org.dash.valid.gl.haplo.BCHaplotype;
import org.dash.valid.gl.haplo.DRDQHaplotype;
import org.immunogenomics.gl.Allele;
import org.immunogenomics.gl.AlleleList;
import org.immunogenomics.gl.Genotype;
import org.immunogenomics.gl.GenotypeList;
import org.immunogenomics.gl.Haplotype;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;

public class LinkageDisequilibriumGenotypeList {
	private String id;
	private String glString;
	private MultilocusUnphasedGenotype mug;
	private Set<Set<String>> bAlleles;
	private Set<Set<String>> cAlleles;
	private Set<Set<String>> drb1Alleles;
	private Set<Set<String>> drb345Alleles;
	private Set<Set<String>> dqb1Alleles;
	private Set<Set<String>> dqa1Alleles;
	private Set<BCHaplotype> possibleBCHaplotypes;
	private Set<DRDQHaplotype> possibleDRDQHaplotypes;
		
    private static final Logger LOGGER = Logger.getLogger(LinkageDisequilibriumGenotypeList.class.getName());
	
	public LinkageDisequilibriumGenotypeList(String id, String glString) {
		this.glString = glString;
		this.id = id;
		init();
		parseGLString();
		setPossibleBCHaplotypes();
		setPossibleDRDQHaplotypes();
	}
	
	public LinkageDisequilibriumGenotypeList(MultilocusUnphasedGenotype mug) {
		this.mug = mug;
		this.glString = mug.getGlstring();
		this.id = mug.getId();
		init();
		decomposeMug();
		setPossibleBCHaplotypes();
		setPossibleDRDQHaplotypes();
	}
	
	private void init() {
		bAlleles = new HashSet<Set<String>>();
		cAlleles = new HashSet<Set<String>>();
		drb1Alleles = new HashSet<Set<String>>();
		drb345Alleles = new HashSet<Set<String>>();
		dqb1Alleles = new HashSet<Set<String>>();
		dqa1Alleles = new HashSet<Set<String>>();	
	}
	
	public boolean checkHomozygous(Locus locus) {
		switch (locus) {
		case HLA_B:
			return GLStringUtilities.checkHomozygous(bAlleles);
		case HLA_C:
			return GLStringUtilities.checkHomozygous(cAlleles);
		case HLA_DRB1:
			return GLStringUtilities.checkHomozygous(drb1Alleles);
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRB345:
			return GLStringUtilities.checkHomozygous(drb345Alleles);
		case HLA_DQB1:
			return GLStringUtilities.checkHomozygous(dqb1Alleles);
		case HLA_DQA1:
			return GLStringUtilities.checkHomozygous(dqa1Alleles);
		default:
			return false;
		}
	}
	
	public Set<Set<String>> getAlleles(Locus locus) {
		switch (locus) {
		case HLA_B:
			return getBAlleles();
		case HLA_C:
			return getCAlleles();
		case HLA_DRB1:
			return getDrb1Alleles();
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRB345:
			return getDrb345Alleles();
		case HLA_DQB1:
			return getDqb1Alleles();
		case HLA_DQA1:
			return getDqa1Alleles();
		default:
			return null;
		}
	}
	
	private void parseGLString() {		
		Set<String> genes = GLStringUtilities.parse(glString, GLStringConstants.GENE_DELIMITER);
		for (String gene : genes) {			
			String[] splitString = gene.split(GLStringUtilities.ESCAPED_ASTERISK);
			String locus = splitString[0];
			
			Set<String> genotypeAmbiguities = GLStringUtilities.parse(gene, GLStringConstants.GENOTYPE_AMBIGUITY_DELIMITER);
			for (String genotypeAmbiguity : genotypeAmbiguities) {
				Set<String> geneCopies = GLStringUtilities.parse(genotypeAmbiguity, GLStringConstants.GENE_COPY_DELIMITER);
				for (String geneCopy : geneCopies) {
					Set<String> genePhases = GLStringUtilities.parse(geneCopy, GLStringConstants.GENE_PHASE_DELIMITER);
					for (String genePhase : genePhases) {
						Set<String> alleleAmbiguities = GLStringUtilities.parse(genePhase, GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
						organizeByLocus(Locus.lookup(locus), alleleAmbiguities);
					}
				}
			}
		}
	}
	
	private void decomposeMug() {
		String locus = null;
		
		List<GenotypeList> genotypeLists = mug.getGenotypeLists();
		for (GenotypeList gl : genotypeLists) {
			List<Genotype> genotypes = gl.getGenotypes();
			for (Genotype genotype : genotypes) {
				List<Haplotype> haplotypes = genotype.getHaplotypes();
				for (Haplotype haplotype : haplotypes) {
					List<AlleleList> alleleLists = haplotype.getAlleleLists();
					for (AlleleList alleleList : alleleLists) {
						List<Allele> alleles = alleleList.getAlleles();
						Set<String> alleleStrings = new HashSet<String>();
						for (Allele allele : alleles) {
							alleleStrings.add(allele.getGlstring());
							locus = allele.getLocus().toString();
						}
						organizeByLocus(Locus.lookup(locus), alleleStrings);
					}
				}
			}
		}
	}
	
	private void organizeByLocus(Locus locus, Set<String> alleleAmbiguities) {
		if (alleleAmbiguities.size() == 0) {
			LOGGER.warning("Unexpected formatting of LinkageDisequilibriumGenotypeList.  No alleles found");
			return;
		}
		
		String allele = alleleAmbiguities.iterator().next();
		if (allele == null) {
			LOGGER.warning("Unexpected formatting of LinkageDisequilibriumGenotypeList, allele == null");
			return;
		}
		
		switch (locus) {
		case HLA_B:
			bAlleles.add(alleleAmbiguities);
			break;
		case HLA_C:
			cAlleles.add(alleleAmbiguities);
			break;
		case HLA_DRB1:
			drb1Alleles.add(alleleAmbiguities);
			break;
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
			drb345Alleles.add(alleleAmbiguities);
			break;
		case HLA_DQB1:
			dqb1Alleles.add(alleleAmbiguities);
			break;
		case HLA_DQA1:
			dqa1Alleles.add(alleleAmbiguities);
			break;
		default:
			break;
		}
	}
	
	public Set<BCHaplotype> getPossibleBCHaplotypes() {
		return this.possibleBCHaplotypes;
	}
	
	public void setPossibleBCHaplotypes() {
		Set<BCHaplotype> possibleHaplotypes = new HashSet<BCHaplotype>();
		
		int bHaplotypeInstance = -1;
		
		for (Set<String> bHaplotypeAlleles : bAlleles) {
			int cHaplotypeInstance = -1;
			bHaplotypeInstance++;
			for (Set<String> cHaplotypeAlleles : cAlleles) {
				cHaplotypeInstance++;
				possibleHaplotypes.add(new BCHaplotype(bHaplotypeAlleles, bHaplotypeInstance, cHaplotypeAlleles, cHaplotypeInstance));
				if (checkHomozygous(Locus.HLA_C)) {
					break;
				}
			}
			if (checkHomozygous(Locus.HLA_B)) {
				break;
			}
		}
		
		LOGGER.info(possibleHaplotypes.size() + " possible BC haplotypes");
		this.possibleBCHaplotypes = possibleHaplotypes;
	}
	
	public Set<DRDQHaplotype> getPossibleDRDQHaplotypes() {
		return this.possibleDRDQHaplotypes;
	}
	
	public void setPossibleDRDQHaplotypes() {
		Set<DRDQHaplotype> possibleHaplotypes = new HashSet<DRDQHaplotype>();
		
		if (drb345Alleles.size() == 0) {
			Set<String> drb345Set = new HashSet<String>();
			drb345Set.add(GLStringConstants.NNNN);
			drb345Alleles.add(drb345Set);
		}
		
		int drb1HaplotypeInstance = -1;
		int drb345HaplotypeInstance = -1;
		int dqb1HaplotypeInstance = -1;
		int dqa1HaplotypeInstance = -1;
		
		for (Set<String> drb1HaplotypeAlleles : drb1Alleles) {
			drb345HaplotypeInstance = -1;
			drb1HaplotypeInstance++;
			for (Set<String> drb345HaplotypeAlleles : drb345Alleles) {
				dqb1HaplotypeInstance = -1;
				drb345HaplotypeInstance++;
				for (Set<String> dqb1HaplotypeAlleles : dqb1Alleles) {
					dqa1HaplotypeInstance = -1;
					dqb1HaplotypeInstance++;
					if (DRDQHaplotype.EXPECTING_DQA1) {
						for (Set<String> dqa1HaplotypeAlleles : dqa1Alleles) {
							dqa1HaplotypeInstance++;
							possibleHaplotypes.add(new DRDQHaplotype(drb1HaplotypeAlleles, drb1HaplotypeInstance,
									drb345HaplotypeAlleles, drb345HaplotypeInstance,
									dqb1HaplotypeAlleles, dqb1HaplotypeInstance,
									dqa1HaplotypeAlleles, dqa1HaplotypeInstance));
							if (checkHomozygous(Locus.HLA_DQA1)) {
								break;
							}
						}
					}
					else {
						possibleHaplotypes.add(new DRDQHaplotype(drb1HaplotypeAlleles, drb1HaplotypeInstance,
								drb345HaplotypeAlleles, drb345HaplotypeInstance,
								dqb1HaplotypeAlleles, dqb1HaplotypeInstance,
								new HashSet<String>(), dqa1HaplotypeInstance));
						if (checkHomozygous(Locus.HLA_DQB1)) {
							break;
						}
					}
				}
				if (checkHomozygous(Locus.HLA_DRB345)) {
					break;
				}
			}
			if (checkHomozygous(Locus.HLA_DRB1)) {
				break;
			}
		}
		
		LOGGER.info(possibleHaplotypes.size() + " possible DRDQ haplotypes");
		this.possibleDRDQHaplotypes = possibleHaplotypes;
		
	for (DRDQHaplotype haplotype : this.possibleDRDQHaplotypes) {
			int[] hap1Instances = haplotype.getHaplotypeInstances();
			LOGGER.fine("Possible haplotype: " + haplotype + " and instances: " + hap1Instances[0] + ", " + hap1Instances[1] + ", " + hap1Instances[2] + ", " + hap1Instances[3]);
		}
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public Set<Set<String>> getBAlleles() {
		return bAlleles;
	}

	public Set<Set<String>> getCAlleles() {
		return cAlleles;
	}

	public Set<Set<String>> getDrb1Alleles() {
		return drb1Alleles;
	}

	public Set<Set<String>> getDrb345Alleles() {
		return drb345Alleles;
	}

	public Set<Set<String>> getDqb1Alleles() {
		return dqb1Alleles;
	}

	public Set<Set<String>> getDqa1Alleles() {
		return dqa1Alleles;
	}
	
	public String getGLString() {
		return glString;
	}
	
	public String toString() {
		String alleleSummary = glString + GLStringConstants.NEWLINE + bAlleles + GLStringConstants.NEWLINE + cAlleles + GLStringConstants.NEWLINE + 
				drb1Alleles + GLStringConstants.NEWLINE + drb345Alleles + GLStringConstants.NEWLINE + dqb1Alleles + GLStringConstants.NEWLINE + dqa1Alleles;
		return alleleSummary;
	}
}
