package org.dash.valid.gl;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
		
    private static final Logger LOGGER = Logger.getLogger(LinkageDisequilibriumGenotypeList.class.getName());
    
    // TODO:  Revisit - necessary or in appropriate place?
    static {
    	try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
    	}
    	catch (IOException ioe) {
    		LOGGER.severe("Could not add file handler to logger");
    	}
    }
	
	public LinkageDisequilibriumGenotypeList(String id, String glString) {
		this.glString = glString;
		this.id = id;
		init();
		parseGLString();
	}
	
	public LinkageDisequilibriumGenotypeList(MultilocusUnphasedGenotype mug) {
		this.mug = mug;
		this.glString = mug.getGlstring();
		this.id = mug.getId();
		init();
		decomposeMug();
	}
	
	private void init() {
		bAlleles = new HashSet<Set<String>>();
		cAlleles = new HashSet<Set<String>>();
		drb1Alleles = new HashSet<Set<String>>();
		drb345Alleles = new HashSet<Set<String>>();
		dqb1Alleles = new HashSet<Set<String>>();
		dqa1Alleles = new HashSet<Set<String>>();	
	}
	
	public boolean drb345AppearsHomozygous() {
		if (getDrb345Alleles().size() <= 1) {
			return true;
		}
		
		return false;
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
						organizeByLocus(locus, alleleAmbiguities);
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
						organizeByLocus(locus, alleleStrings);
					}
				}
			}
		}
	}
	
	private void organizeByLocus(String locus, Set<String> alleleAmbiguities) {
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
		case GLStringConstants.HLA_B:
			bAlleles.add(alleleAmbiguities);
			break;
		case GLStringConstants.HLA_C:
			cAlleles.add(alleleAmbiguities);
			break;
		case GLStringConstants.HLA_DRB1:
			drb1Alleles.add(alleleAmbiguities);
			break;
		case GLStringConstants.HLA_DRB3:
		case GLStringConstants.HLA_DRB4:
		case GLStringConstants.HLA_DRB5:
			drb345Alleles.add(alleleAmbiguities);
			break;
		case GLStringConstants.HLA_DQB1:
			dqb1Alleles.add(alleleAmbiguities);
			break;
		case GLStringConstants.HLA_DQA1:
			dqa1Alleles.add(alleleAmbiguities);
			break;
		default:
			break;
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
		String alleleSummary = glString + "\n" + bAlleles + "\n" + cAlleles + "\n" + drb1Alleles + "\n" + drb345Alleles + "\n" + dqb1Alleles + "\n" + dqa1Alleles;
		return alleleSummary;
	}
}
