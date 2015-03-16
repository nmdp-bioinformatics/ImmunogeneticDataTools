package org.dash.valid.gl;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
	private List<List<String>> bAlleles;
	private List<List<String>> cAlleles;
	private List<List<String>> drb1Alleles;
	private List<List<String>> drb345Alleles;
	private List<List<String>> dqb1Alleles;
	private List<List<String>> dqa1Alleles;
		
    private static final Logger LOGGER = Logger.getLogger(LinkageDisequilibriumGenotypeList.class.getName());
    
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
		bAlleles = new ArrayList<List<String>>();
		cAlleles = new ArrayList<List<String>>();
		drb1Alleles = new ArrayList<List<String>>();
		drb345Alleles = new ArrayList<List<String>>();
		dqb1Alleles = new ArrayList<List<String>>();
		dqa1Alleles = new ArrayList<List<String>>();	
	}
	
	public boolean drb345AppearsHomozygous() {
		if (getDrb345Alleles().size() <= 1) {
			return true;
		}
		
		return false;
	}
	
	private void parseGLString() {		
		List<String> genes = GLStringUtilities.parse(glString, GLStringConstants.GENE_DELIMITER);
		for (String gene : genes) {			
			String[] splitString = gene.split(GLStringUtilities.ESCAPED_ASTERISK);
			String locus = splitString[0];
			
			List<String> genotypeAmbiguities = GLStringUtilities.parse(gene, GLStringConstants.GENOTYPE_AMBIGUITY_DELIMITER);
			for (String genotypeAmbiguity : genotypeAmbiguities) {
				List<String> geneCopies = GLStringUtilities.parse(genotypeAmbiguity, GLStringConstants.GENE_COPY_DELIMITER);
				for (String geneCopy : geneCopies) {
					List<String> genePhases = GLStringUtilities.parse(geneCopy, GLStringConstants.GENE_PHASE_DELIMITER);
					for (String genePhase : genePhases) {
						List<String> alleleAmbiguities = GLStringUtilities.parse(genePhase, GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
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
						List<String> alleleStrings = new ArrayList<String>();
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
	
	private void organizeByLocus(String locus, List<String> alleleAmbiguities) {
		if (alleleAmbiguities.size() == 0) {
			LOGGER.warning("Unexpected formatting of LinkageDisequilibriumGenotypeList.  No alleles found");
			return;
		}
		
		String allele = alleleAmbiguities.get(0);
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
	
	public List<List<String>> getBAlleles() {
		return bAlleles;
	}

	public List<List<String>> getCAlleles() {
		return cAlleles;
	}

	public List<List<String>> getDrb1Alleles() {
		return drb1Alleles;
	}

	public List<List<String>> getDrb345Alleles() {
		return drb345Alleles;
	}

	public List<List<String>> getDqb1Alleles() {
		return dqb1Alleles;
	}

	public List<List<String>> getDqa1Alleles() {
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
