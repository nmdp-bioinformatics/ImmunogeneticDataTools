package org.dash.valid.gl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.Linkages;
import org.dash.valid.Locus;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.dash.valid.gl.haplo.SingleLocusHaplotype;
import org.nmdp.gl.Allele;
import org.nmdp.gl.AlleleList;
import org.nmdp.gl.Genotype;
import org.nmdp.gl.GenotypeList;
import org.nmdp.gl.Haplotype;
import org.nmdp.gl.MultilocusUnphasedGenotype;

public class LinkageDisequilibriumGenotypeList {
	private String id;
	private String glString;
	private MultilocusUnphasedGenotype mug;
	
	private HashMap<Locus, List<List<String>>> allelesMap = new HashMap<Locus, List<List<String>>>();
	private HashMap<EnumSet<Locus>, Set<MultiLocusHaplotype>> possibleHaplotypeMap = new HashMap<EnumSet<Locus>, Set<MultiLocusHaplotype>>();
	
	public static final boolean EXPECTING_DQA1;
	
	static {
		EXPECTING_DQA1 = Frequencies.WIKIVERSITY.getShortName().equals(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
	}

	private static final Logger LOGGER = Logger
			.getLogger(LinkageDisequilibriumGenotypeList.class.getName());

	private static Integer ALLELE_AMBIGUITY_THRESHOLD = 20;
	private static Integer PROTEIN_THRESHOLD = 10;

	static {
		String alleleAmbiguityThreshold;
		if ((alleleAmbiguityThreshold = System
				.getProperty("org.dash.ambThreshold")) != null) {
			ALLELE_AMBIGUITY_THRESHOLD = new Integer(alleleAmbiguityThreshold);
		}

		String proteinThreshold;
		if ((proteinThreshold = System.getProperty("org.dash.proteinThreshold")) != null) {
			PROTEIN_THRESHOLD = new Integer(proteinThreshold);
		}
	}

	public LinkageDisequilibriumGenotypeList(String id, String glString) {
		this.glString = glString;
		this.id = id;
		parseGLString();
		postParseInit();
		
		for (Linkages linkage : HLAFrequenciesLoader.getInstance().getLinkages()) {
			setPossibleHaplotypes(linkage.getLoci());
		}
	}

	public LinkageDisequilibriumGenotypeList(MultilocusUnphasedGenotype mug) {
		this.mug = mug;
		this.glString = mug.getGlstring();
		this.id = mug.getId();
		decomposeMug();
		postParseInit();

		for (Linkages linkage : HLAFrequenciesLoader.getInstance().getLinkages()) {
			setPossibleHaplotypes(linkage.getLoci());
		}
	}
	
	private void postParseInit() {
		if (!this.allelesMap.containsKey(Locus.HLA_DRB345) || this.allelesMap.get(Locus.HLA_DRB345).size() == 0) {
			List<String> drb345Set = new ArrayList<String>();
			drb345Set.add(GLStringConstants.NNNN);
			setAlleles(Locus.HLA_DRB345, drb345Set);
		}
	}
	
	private void removeAlleles(List<String> allelesToRemove) {
		for (Locus locus : allelesMap.keySet()) {
			for (List<String> alleles : getAlleles(locus)) {
				alleles.removeAll(allelesToRemove);
			}
		}
	}
	
	public static Locus normalizeLocus(Locus locus) {
		switch (locus) {
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRBX:
			return Locus.HLA_DRB345;
		default:
			return locus;
		}
	}

	public boolean checkHomozygous(Locus locus) {
		locus = normalizeLocus(locus);
		return GLStringUtilities.checkHomozygous(getAlleles(locus));
	}

	private List<?>[] getAlleles(Locus... loci) {
		List<?>[] allelesByLocus = new List[loci.length];

		int i = 0;

		for (Locus locus : loci) {
			allelesByLocus[i] = getAlleles(locus);
			i++;
		}

		return allelesByLocus;
	}
	
	public Integer getHaplotypeIndex(Locus locus, List<String> alleles) {
		List<List<String>> locusAlleles = getAlleles(locus);
		
		int i=0;
		
		for (List<String> locusHaplotypeAlleles : locusAlleles) {
			if (locusHaplotypeAlleles.containsAll(alleles)) {
				return new Integer(i);
			}
			i++;
		}
		
		LOGGER.warning("Can't find a haplotype index for a set of alleles: " + locusAlleles);
		
		return -1;
	}

	// TODO: Write unit tests
	public boolean checkAmbiguitiesThresholds() {
		for (Linkages linkages : HLAFrequenciesLoader.getInstance().getLinkages()) {
			for (Locus locus : linkages.getLoci()) {
				if (getAlleleCount(locus) > ALLELE_AMBIGUITY_THRESHOLD) {
					LOGGER.warning("Exceeded the allele ambiguity threshold of : "
							+ ALLELE_AMBIGUITY_THRESHOLD + " at locus: "
							+ locus.getFullName());
					return false;
				}
	
				if (getProteinCount(locus) > PROTEIN_THRESHOLD) {
					LOGGER.warning("Exceeded the protein threshold of : "
							+ PROTEIN_THRESHOLD + " at locus: "
							+ locus.getFullName());
					return false;
				}
	
				// TODO: Check against single locus frequencies - move elsewhere (perhaps when alleles are originally parse)
				
				List<String> allelesToRemove = new ArrayList<String>();
				
				if (GLStringUtilities.individualFrequenciesLoaded()) {
					List<List<String>> allLocusAlleles = getAlleles(locus);
					allelesToRemove = new ArrayList<String>();

					for (List<String> locusAlleles : allLocusAlleles) {						
						for (String allele : locusAlleles) {
							if (!GLStringUtilities.hasFrequency(locus, allele)) {
								LOGGER.finest("Removing allele with no frequency: " + allele);
								allelesToRemove.add(allele);
							}
						}
					}
					
					removeAlleles(allelesToRemove);
				}
			}
		}

		return true;
	}

	public int getAlleleCount(Locus locus) {
		int alleleCount = 0;

		for (List<String> alleleList : getAlleles(locus)) {
			alleleCount += alleleList.size();
		}

		return alleleCount;
	}

	public int getProteinCount(Locus locus) {
		HashSet<String> proteins = new HashSet<String>();

		for (List<String> alleleList : getAlleles(locus)) {
			for (String allele : alleleList) {
				proteins.add(GLStringUtilities.convertToProteinLevel(allele));
			}
		}

		return proteins.size();
	}

	private void parseGLString() {
		List<String> genes = GLStringUtilities.parse(glString,
				GLStringConstants.GENE_DELIMITER);
		for (String gene : genes) {
			String[] splitString = gene
					.split(GLStringUtilities.ESCAPED_ASTERISK);
			String locus = splitString[0];

			List<String> genotypeAmbiguities = GLStringUtilities.parse(gene,
					GLStringConstants.GENOTYPE_AMBIGUITY_DELIMITER);
			for (String genotypeAmbiguity : genotypeAmbiguities) {
				List<String> geneCopies = GLStringUtilities.parse(
						genotypeAmbiguity,
						GLStringConstants.GENE_COPY_DELIMITER);
				for (String geneCopy : geneCopies) {
					List<String> genePhases = GLStringUtilities.parse(geneCopy,
							GLStringConstants.GENE_PHASE_DELIMITER);
					for (String genePhase : genePhases) {
						List<String> alleleAmbiguities = GLStringUtilities
								.parse(genePhase,
										GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
						setAlleles(normalizeLocus(Locus.lookup(locus)), alleleAmbiguities);
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
						setAlleles(normalizeLocus(Locus.lookup(locus)), alleleStrings);
					}
				}
			}
		}
	}

	private void setAlleles(Locus locus, List<String> alleleAmbiguities) {
		if (alleleAmbiguities.size() == 0) {
			LOGGER.warning("Unexpected formatting of LinkageDisequilibriumGenotypeList.  No alleles found");
			return;
		}

		String allele = alleleAmbiguities.iterator().next();
		if (allele == null) {
			LOGGER.warning("Unexpected formatting of LinkageDisequilibriumGenotypeList, allele == null");
			return;
		}
		
		List<List<String>> alleles;
		if (this.allelesMap.containsKey(locus)) {
			alleles = allelesMap.get(locus);
		}
		else {
			alleles = new ArrayList<List<String>>();
		}
		
		alleles.add(alleleAmbiguities);
		this.allelesMap.put(locus, alleles);
	}

	public static List<List<Object>> cartesianProduct(List<?>... sets) {
		if (sets.length < 2)
			throw new IllegalArgumentException(
					"Can't have a product of fewer than two sets (got "
							+ sets.length + ")");

		return _cartesianProduct(0, sets);
	}

	private static List<List<Object>> _cartesianProduct(int index, List<?>... sets) {
		List<List<Object>> ret = new ArrayList<List<Object>>();
		if (index == sets.length) {
			ret.add(new ArrayList<Object>());
		} else {
			for (Object obj : sets[index]) {
				for (List<Object> set : _cartesianProduct(index + 1, sets)) {
					set.add(obj);
					ret.add(set);
				}
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public Set<MultiLocusHaplotype> constructPossibleHaplotypes(Set<Locus> loci) {
		HashMap<Locus, SingleLocusHaplotype> singleLocusHaplotypes = new HashMap<Locus, SingleLocusHaplotype>();
		Set<MultiLocusHaplotype> possibleHaplotypes = new HashSet<MultiLocusHaplotype>();

		Locus[] locusArray = loci.toArray(new Locus[loci.size()]);
		List<?>[] allelesByLocus = (List<?>[]) getAlleles(locusArray);

		List<List<Object>> haplotypeCombinations = cartesianProduct(allelesByLocus);
		String[] alleleParts;
		Locus locus;

		for (List<Object> haplotypeCombo : haplotypeCombinations) {
			for (Object haplotypePart : haplotypeCombo) {
				List<String> alleles = (List<String>) haplotypePart;
				alleleParts = alleles.iterator().next()
						.split(GLStringUtilities.ESCAPED_ASTERISK);
				locus = Locus.lookup(alleleParts[0]);

				locus = normalizeLocus(locus);
				
				singleLocusHaplotypes.put(locus, new SingleLocusHaplotype(
						locus, (List<String>) haplotypePart, getHaplotypeIndex(locus, (List<String>) haplotypePart)));
			}
			possibleHaplotypes.add(new MultiLocusHaplotype(
					singleLocusHaplotypes));
		}

		return possibleHaplotypes;
	}
	
	public Set<MultiLocusHaplotype> getPossibleHaplotypes(EnumSet<Locus> loci) {
		if (!this.possibleHaplotypeMap.containsKey(loci)) {
			setPossibleHaplotypes(loci);
		}
		
		return this.possibleHaplotypeMap.get(loci);		
	}

	public void setPossibleHaplotypes(EnumSet<Locus> loci) {
		this.possibleHaplotypeMap.put(loci, constructPossibleHaplotypes(loci));
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public List<List<String>> getAlleles(Locus locus) {
		if (this.allelesMap.containsKey(locus)) {
			return this.allelesMap.get(locus);
		}
		
		return new ArrayList<List<String>>();
	}

	public String getGLString() {
		return glString;
	}

	public String toString() {
		String alleleSummary = glString + GLStringConstants.NEWLINE + getAlleles(Locus.HLA_A)
				+ GLStringConstants.NEWLINE + getAlleles(Locus.HLA_B)
				+ GLStringConstants.NEWLINE + getAlleles(Locus.HLA_C)
				+ GLStringConstants.NEWLINE + getAlleles(Locus.HLA_DRB1)
				+ GLStringConstants.NEWLINE + getAlleles(Locus.HLA_DRB345)
				+ GLStringConstants.NEWLINE + getAlleles(Locus.HLA_DQB1)
				+ GLStringConstants.NEWLINE + getAlleles(Locus.HLA_DQA1);
		return alleleSummary;
	}
}
