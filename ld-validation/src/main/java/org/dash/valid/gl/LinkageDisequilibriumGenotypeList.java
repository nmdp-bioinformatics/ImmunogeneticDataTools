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
package org.dash.valid.gl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
import org.dash.valid.Locus;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.dash.valid.gl.haplo.SingleLocusHaplotype;

public class LinkageDisequilibriumGenotypeList {
	private String id;
	private String glString;
	private String note;
	private String submittedGlString;
	
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
			ALLELE_AMBIGUITY_THRESHOLD = Integer.parseInt(alleleAmbiguityThreshold);
		}

		String proteinThreshold;
		if ((proteinThreshold = System.getProperty("org.dash.proteinThreshold")) != null) {
			PROTEIN_THRESHOLD = Integer.parseInt(proteinThreshold);
		}
	}
	
	public Set<Locus> getLoci() {
		return allelesMap.keySet();
	}

	public LinkageDisequilibriumGenotypeList(String id, String glString) {
		this.glString = glString;
		this.id = id;
		parseGLString();
		postParseInit();
		
		for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
			setPossibleHaplotypes(linkage.getLoci());
		}
	}
	

	
	public String getNote() {
		return note;
	}
	
	public void setNote(String note) {
		this.note = note;
	}
	
	public String getSubmittedGlString() {
		return submittedGlString;
	}

	public void setSubmittedGlString(String submittedGlString) {
		this.submittedGlString = submittedGlString;
	}
	
	private void postParseInit() {
		if (!this.allelesMap.containsKey(Locus.HLA_DRB345) || this.allelesMap.get(Locus.HLA_DRB345).size() == 0) {
			List<String> drb345Set = new ArrayList<String>();
			drb345Set.add(GLStringConstants.NNNN);
			setAlleles(Locus.HLA_DRB345, drb345Set);
		}
	}

	public boolean hasHomozygous(Locus locus) {
		locus = Locus.normalizeLocus(locus);
		return GLStringUtilities.checkHomozygous(getAlleles(locus));
	}
	
	public boolean hasHomozygous(Set<Locus> loci) {
		for (Locus locus : loci) {
			if (hasHomozygous(locus)) {
				return true;
			}
		}
		
		return false;
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
				return i;
			}
			i++;
		}
		
		LOGGER.warning("Can't find a haplotype index for a set of alleles: " + locusAlleles);
		
		return -1;
	}

	// TODO: Write unit tests
	public boolean checkAmbiguitiesThresholds() {
		for (Linkages linkages : LinkagesLoader.getInstance().getLinkages()) {
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
		HashMap<String, Locus> locusMap = new HashMap<String, Locus>();
		Locus locus = null;
		
		List<String> genes = GLStringUtilities.parse(glString,
				GLStringConstants.GENE_DELIMITER);
		for (String gene : genes) {
			String[] splitString = gene
					.split(GLStringUtilities.ESCAPED_ASTERISK);
			String locusVal = splitString[0];

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
						
						if (locusMap.containsKey(locusVal)) {
							locus = locusMap.get(locusVal);
						}
						else {
							locus = Locus.normalizeLocus(Locus.lookup(locusVal));
							locusMap.put(locusVal, locus);
						}
						setAlleles(locus, alleleAmbiguities);
					}
				}
			}
		}
	}



	private void setAlleles(Locus locus, List<String> alleleAmbiguities) {
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		List<String> replacementAlleles = new ArrayList<String>();
		
		// TODO: Implement low res conversion here...
		if (!alleleAmbiguities.contains(GLStringUtilities.COLON)) {
			//alleleAmbiguities = GLStringUtilities.convertLowResToAlleleAmbiguities(Locus locus, List<String> alleleAmbiguities);
		}
		
		if (freqLoader.hasIndividualFrequency(locus)) {
			String alleleWithFrequency;
			for (String allele : alleleAmbiguities) {
				alleleWithFrequency = freqLoader.hasFrequency(locus, allele);
				if (alleleWithFrequency == null) {
					LOGGER.info("Removing allele with no frequency: " + allele);
				}
				else {
					if (!replacementAlleles.contains(alleleWithFrequency)) {
						LOGGER.finest("Swapping in allele with frequency: " + alleleWithFrequency);
						replacementAlleles.add(alleleWithFrequency);
					}
				}	
			}	
			
			if (replacementAlleles.size() == 0) {
				LOGGER.finest("Couldn't find frequencies for entire haploid.  Leaving originals in place.");
			}
			else {
				alleleAmbiguities = replacementAlleles;
			}
		}			
		
		if (alleleAmbiguities.size() == 0) {
			LOGGER.warning("Unexpected formatting of LinkageDisequilibriumGenotypeList.  No alleles found or no alleles w/ haplotype frequencies found.");
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
		HashMap<String, Locus> locusMap = new HashMap<String, Locus>();

		for (List<Object> haplotypeCombo : haplotypeCombinations) {
			boolean drb345Homozygous = false;
			for (Object haplotypePart : haplotypeCombo) {
				List<String> alleles = (List<String>) haplotypePart;
				alleleParts = alleles.iterator().next()
						.split(GLStringUtilities.ESCAPED_ASTERISK);
				if (locusMap.containsKey(alleleParts[0])) {
					locus = locusMap.get(alleleParts[0]);
				}
				else {
					locus = Locus.normalizeLocus(Locus.lookup(alleleParts[0]));
					locusMap.put(alleleParts[0], locus);
				}
				
				if (Locus.HLA_DRB345.equals(locus) && hasHomozygous(locus)) {
					drb345Homozygous = true;
				}
				
				singleLocusHaplotypes.put(locus, new SingleLocusHaplotype(
						locus, (List<String>) haplotypePart, getHaplotypeIndex(locus, (List<String>) haplotypePart)));
			}
			possibleHaplotypes.add(new MultiLocusHaplotype(
					singleLocusHaplotypes, drb345Homozygous));
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
