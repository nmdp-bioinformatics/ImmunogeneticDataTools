package org.dash.valid;

import java.util.Map;
import java.util.Set;

import org.dash.valid.base.BaseDRDQDisequilibriumElement;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;

/*
 * Linkage disequilibrium
 * 
 * Non-random association of alleles at two or more loci that descend from a single,
 * ancestral chromosome
 * 
 * http://en.wikipedia.org/wiki/Linkage_disequilibrium
 * 
 * This class leverages a specific set of linkage disequilibrium associations relevant in the context
 * of HLA (http://en.wikipedia.org/wiki/Human_leukocyte_antigen)  and immunogenetics:
 * 
 * http://en.wikiversity.org/wiki/HLA/Linkage_Disequilibrium/B-C_Blocks
 * http://en.wikiversity.org/wiki/HLA/Linkage_Disequilibrium/DR-DQ_Blocks
 * 
 */

public class HLALinkageDisequilibrium {
	private static final String DASH = "-";
	private static final String NNNN = "DRBX*NNNN";
			
	public static Map<Object, Boolean> hasDisequilibriumLinkage(LinkageDisequilibriumGenotypeList glString) {
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		Map<Object, Boolean> linkageElementsFound = new LinkageElementsMap(new DisequilibriumElementComparator());
				
		for (BCDisequilibriumElement disElement : freqLoader.getBCDisequilibriumElements()) {
			linkageElementsFound = detectBCLinkages(linkageElementsFound, glString, disElement);
		}
		
		for (DRDQDisequilibriumElement disElement : freqLoader.getDRDQDisequilibriumElements()) {
			linkageElementsFound = detectDRDQLinkages(linkageElementsFound, glString, disElement);
		}
		
		return linkageElementsFound;
	}

	private static Map<Object, Boolean> detectBCLinkages(Map<Object, Boolean> linkageElementsFound,
								LinkageDisequilibriumGenotypeList glString,
								BCDisequilibriumElement disElement) {
		for (Set<String> bList : glString.getBAlleles()) {
			for (String bAllele : bList) {
				if (bAllele.equals(disElement.getHlabElement())) {
					linkageElementsFound = detectCLinkages(linkageElementsFound, disElement, glString, true);
				}
				else if (GLStringUtilities.fieldLevelComparison(bAllele, disElement.getHlabElement()) || 
							GLStringUtilities.checkAntigenRecognitionSite(bAllele, disElement.getHlabElement())) {
					linkageElementsFound = detectCLinkages(linkageElementsFound, disElement, glString);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Map<Object, Boolean> detectCLinkages(Map<Object, Boolean> linkageElementsFound,
								BCDisequilibriumElement disElement,
								LinkageDisequilibriumGenotypeList glString) {
		return detectCLinkages(linkageElementsFound, disElement, glString, false);
	}

	private static Map<Object, Boolean> detectCLinkages(Map<Object, Boolean> linkageElementsFound,
								BCDisequilibriumElement disElement,
								LinkageDisequilibriumGenotypeList glString,
								boolean nonCodingVariationLinkage) {		
		for (Set<String> cList : glString.getCAlleles()) {	
			for (String cAllele : cList) {
				if (nonCodingVariationLinkage && cAllele.equals(disElement.getHlacElement())) {
					// perfect hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound,
							disElement, true);
				}
				else if (cAllele.equals(disElement.getHlacElement()) || 
						GLStringUtilities.fieldLevelComparison(cAllele, disElement.getHlacElement()) ||
						GLStringUtilities.checkAntigenRecognitionSite(cAllele, disElement.getHlacElement())) {
					// partial hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound,
							disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Map<Object, Boolean> detectDRDQLinkages(Map<Object, Boolean> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DRDQDisequilibriumElement disElement) {
		for (Set<String> drb1List : glString.getDrb1Alleles()) {
			for (String drb1Allele : drb1List) {
				if (drb1Allele.equals(disElement.getHladrb1Element())) {
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, disElement, true);
				}
				else if (GLStringUtilities.fieldLevelComparison(drb1Allele, disElement.getHladrb1Element()) ||
							GLStringUtilities.checkAntigenRecognitionSite(drb1Allele, disElement.getHladrb1Element())) {
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Map<Object, Boolean> detectDRB345DQLinkages(Map<Object, Boolean> linkageElementsFound,
			LinkageDisequilibriumGenotypeList glString,
			DRDQDisequilibriumElement disElement) {
		return detectDRB345DQLinkages(linkageElementsFound, glString, disElement, false);
	}
	
	private static Map<Object, Boolean> detectDRB345DQLinkages(Map<Object, Boolean> linkageElementsFound,
										LinkageDisequilibriumGenotypeList glString,
										DRDQDisequilibriumElement disElement, boolean nonCodingVariationLinkage) {
		if (glString.drb345AppearsHomozygous() && (disElement.getHladrb345Element().equals(DASH) || disElement.getHladrb345Element().equals(NNNN))) {
			// TODO:  consider whether true should be sent on this call
			linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, disElement);
		} 
		for (Set<String> drb345List : glString.getDrb345Alleles()) {
			for (String drb345Allele : drb345List) {
				if (nonCodingVariationLinkage && drb345Allele.equals(disElement.getHladrb345Element())) {
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, disElement, true);
				}				
				else if (drb345Allele.equals(disElement.getHladrb345Element()) || 
						GLStringUtilities.fieldLevelComparison(drb345Allele, disElement.getHladrb345Element()) ||
							GLStringUtilities.checkAntigenRecognitionSite(drb345Allele, disElement.getHladrb345Element())) {
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Map<Object, Boolean> detectDQA1Linkages(Map<Object, Boolean> linkageElementsFound, 
			LinkageDisequilibriumGenotypeList glString,
			BaseDRDQDisequilibriumElement disElement) {
		return detectDQA1Linkages(linkageElementsFound, glString, disElement, false);
	}
	
	private static Map<Object, Boolean> detectDQA1Linkages(Map<Object, Boolean> linkageElementsFound, 
								LinkageDisequilibriumGenotypeList glString,
								BaseDRDQDisequilibriumElement disElement, boolean nonCodingVariationLinkage) {
		for (Set<String> dqa1List : glString.getDqa1Alleles()) {
			for (String dqa1Allele : dqa1List) {
				if (nonCodingVariationLinkage && dqa1Allele.equals(disElement.getHladqa1Element())) {
					// perfect hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, disElement, true);				}
				else if (dqa1Allele.equals(disElement.getHladqa1Element()) ||
							GLStringUtilities.fieldLevelComparison(dqa1Allele, disElement.getHladqa1Element()) ||
							GLStringUtilities.checkAntigenRecognitionSite(dqa1Allele, disElement.getHladqa1Element())) {
					// partial hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, disElement);				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Map<Object, Boolean> detectDQB1Linkages(Map<Object, Boolean> linkageElementsFound,
			LinkageDisequilibriumGenotypeList glString,
			DRDQDisequilibriumElement disElement) {
		return detectDQB1Linkages(linkageElementsFound, glString, disElement, false);
	}
	
	private static Map<Object, Boolean> detectDQB1Linkages(Map<Object, Boolean> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DRDQDisequilibriumElement disElement, boolean nonCodingLinkageFound) {
		for (Set<String> dqb1List : glString.getDqb1Alleles()) {
			for (String dqb1Allele : dqb1List) {
				if (nonCodingLinkageFound && dqb1Allele.equals(disElement.getHladqb1Element())) {
					if (disElement instanceof BaseDRDQDisequilibriumElement) {
						linkageElementsFound = detectDQA1Linkages(linkageElementsFound, glString, (BaseDRDQDisequilibriumElement) disElement, true);
					}
					else {
						// full hit
						linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, disElement, true);
					}
				}
				else if (dqb1Allele.equals(disElement.getHladqb1Element()) ||
							GLStringUtilities.fieldLevelComparison(dqb1Allele, disElement.getHladqb1Element()) ||
							GLStringUtilities.checkAntigenRecognitionSite(dqb1Allele, disElement.getHladqb1Element())) {
					if (disElement instanceof BaseDRDQDisequilibriumElement) {
						linkageElementsFound = detectDQA1Linkages(linkageElementsFound, glString, (BaseDRDQDisequilibriumElement) disElement);
					}
					else {
						// partial hit
						linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, disElement);
					}
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Map<Object, Boolean> addMatchedDisequilibriumElement(
			Map<Object, Boolean> linkageElementsFound,
			Object disElement) {
		return addMatchedDisequilibriumElement(linkageElementsFound, disElement, false);
	}

	/**
	 * @param linkageElementsFound
	 * @param disElement
	 * @param nonCodingVariationLinkage
	 */
	private static Map<Object, Boolean> addMatchedDisequilibriumElement(
			Map<Object, Boolean> linkageElementsFound,
			Object disElement, boolean nonCodingVariationLinkage) {
		if (nonCodingVariationLinkage) {
			linkageElementsFound.put(disElement, Boolean.TRUE);
		}
		else if (!linkageElementsFound.containsKey(disElement)) {
			linkageElementsFound.put(disElement, Boolean.FALSE);
		}
		
		return linkageElementsFound;
	}
}
