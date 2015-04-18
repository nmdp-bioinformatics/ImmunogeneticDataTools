package org.dash.valid;

import java.util.Set;

import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.base.BaseDRDQDisequilibriumElement;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.report.DetectedBCDisequilibriumElement;
import org.dash.valid.report.DetectedDRDQDisequilibriumElement;
import org.dash.valid.report.DetectedDisequilibriumElement;
import org.dash.valid.report.DetectedLinkageFindings;
import org.dash.valid.report.LinkageHitDegree;

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
	private static HLADatabaseVersion hladb;
		
	static {
		hladb = HLADatabaseVersion.lookup(System.getProperty(HLADatabaseVersion.HLADB_PROPERTY));
	}
			
	public static DetectedLinkageFindings hasDisequilibriumLinkage(LinkageDisequilibriumGenotypeList glString) {
		DetectedLinkageFindings findings = new DetectedLinkageFindings();
		
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		Set<DetectedDisequilibriumElement> linkageElementsFound = new LinkageElementsSet(new DisequilibriumElementComparator());
		
		Set<String> notCommon = GLStringUtilities.checkCommonWellDocumented(glString.getGLString());
				
		for (BCDisequilibriumElement disElement : freqLoader.getBCDisequilibriumElements()) {
			linkageElementsFound = detectBCLinkages(linkageElementsFound, glString, disElement);
		}
		
		for (DRDQDisequilibriumElement disElement : freqLoader.getDRDQDisequilibriumElements()) {
			linkageElementsFound = detectDRDQLinkages(linkageElementsFound, glString, disElement);
		}
		
		findings.setGenotypeList(glString);
		findings.addLinkages(linkageElementsFound);
		findings.setNonCWDAlleles(notCommon);
		findings.setHladb(hladb);
		
		return findings;
	}

	private static Set<DetectedDisequilibriumElement> detectBCLinkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
								LinkageDisequilibriumGenotypeList glString,
								BCDisequilibriumElement disElement) {
		LinkageHitDegree hitDegree;
		
		for (Set<String> bList : glString.getBAlleles()) {
			for (String bAllele : bList) {
				hitDegree = GLStringUtilities.fieldLevelComparison(bAllele, disElement.getHlabElement());
				if (hitDegree != null) {
					DetectedBCDisequilibriumElement foundElement = new DetectedBCDisequilibriumElement(disElement);
					foundElement.setbHitDegree(hitDegree);
					
					linkageElementsFound = detectCLinkages(linkageElementsFound, foundElement, glString);
				}
				else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(bAllele, disElement.getHlabElement())) != null) {
					DetectedBCDisequilibriumElement foundElement = new DetectedBCDisequilibriumElement(disElement);
					foundElement.setbHitDegree(hitDegree);
					
					linkageElementsFound = detectCLinkages(linkageElementsFound, foundElement, glString);
				}
			}
		}
		
		return linkageElementsFound;
	}

	private static Set<DetectedDisequilibriumElement> detectCLinkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
								DetectedBCDisequilibriumElement foundElement,
								LinkageDisequilibriumGenotypeList glString) {	
		LinkageHitDegree hitDegree;
		for (Set<String> cList : glString.getCAlleles()) {	
			for (String cAllele : cList) {
				hitDegree = GLStringUtilities.fieldLevelComparison(cAllele, foundElement.getDisequilibriumElement().getHlacElement());
				
				if (hitDegree != null) {
					foundElement.setcHitDegree(hitDegree);
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
				else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(cAllele, foundElement.getDisequilibriumElement().getHlacElement())) != null) {
					foundElement.setcHitDegree(hitDegree);
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Set<DetectedDisequilibriumElement> detectDRDQLinkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DRDQDisequilibriumElement disElement) {
		LinkageHitDegree hitDegree;
		for (Set<String> drb1List : glString.getDrb1Alleles()) {
			for (String drb1Allele : drb1List) {
				hitDegree = GLStringUtilities.fieldLevelComparison(drb1Allele, disElement.getHladrb1Element());
				
				if (hitDegree != null) {
					DetectedDRDQDisequilibriumElement foundElement = new DetectedDRDQDisequilibriumElement(disElement);
					foundElement.setDrb1HitDegree(hitDegree);
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, foundElement);
				}
				else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(drb1Allele, disElement.getHladrb1Element())) != null) {
					DetectedDRDQDisequilibriumElement foundElement = new DetectedDRDQDisequilibriumElement(disElement);
					foundElement.setDrb1HitDegree(hitDegree);
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, foundElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Set<DetectedDisequilibriumElement> detectDRB345DQLinkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
										LinkageDisequilibriumGenotypeList glString,
										DetectedDRDQDisequilibriumElement foundElement) {
		if (glString.drb345AppearsHomozygous() && (foundElement.getDisequilibriumElement().getHladrb345Element().equals(DASH) || foundElement.getDisequilibriumElement().getHladrb345Element().equals(NNNN))) {
			linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, foundElement);
		} 
		
		LinkageHitDegree hitDegree;
		
		for (Set<String> drb345List : glString.getDrb345Alleles()) {
			for (String drb345Allele : drb345List) {
				hitDegree = GLStringUtilities.fieldLevelComparison(drb345Allele, foundElement.getDisequilibriumElement().getHladrb345Element());
				
				if (hitDegree != null) {
					foundElement.setDrb345HitDegree(hitDegree);
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, foundElement);
				}
				else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(drb345Allele, foundElement.getDisequilibriumElement().getHladrb345Element())) != null) {
					foundElement.setDrb345HitDegree(hitDegree);
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, foundElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Set<DetectedDisequilibriumElement> detectDQA1Linkages(Set<DetectedDisequilibriumElement> linkageElementsFound, 
								LinkageDisequilibriumGenotypeList glString,
								DetectedDRDQDisequilibriumElement foundElement) {
		LinkageHitDegree hitDegree;
		
		for (Set<String> dqa1List : glString.getDqa1Alleles()) {
			for (String dqa1Allele : dqa1List) {
				hitDegree = GLStringUtilities.fieldLevelComparison(dqa1Allele, ((BaseDRDQDisequilibriumElement)foundElement.getDisequilibriumElement()).getHladqa1Element());
				
				if (hitDegree != null) {
					foundElement.setDqa1HitDegree(hitDegree);
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
				else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(dqa1Allele, ((BaseDRDQDisequilibriumElement)foundElement.getDisequilibriumElement()).getHladqa1Element())) != null) {
					foundElement.setDqa1HitDegree(hitDegree);
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Set<DetectedDisequilibriumElement> detectDQB1Linkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DetectedDRDQDisequilibriumElement foundElement) {
		LinkageHitDegree hitDegree;
		
		for (Set<String> dqb1List : glString.getDqb1Alleles()) {
			for (String dqb1Allele : dqb1List) {
				hitDegree = GLStringUtilities.fieldLevelComparison(dqb1Allele, foundElement.getDisequilibriumElement().getHladqb1Element());
				
				if (hitDegree != null) {
					foundElement.setDqb1HitDegree(hitDegree);
					if (foundElement.getDisequilibriumElement() instanceof BaseDRDQDisequilibriumElement) {
						linkageElementsFound = detectDQA1Linkages(linkageElementsFound, glString, foundElement);
					}
					else {
						linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
					}
				}
				else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(dqb1Allele, foundElement.getDisequilibriumElement().getHladqb1Element())) != null) {
					foundElement.setDqb1HitDegree(hitDegree);
					if (foundElement.getDisequilibriumElement() instanceof BaseDRDQDisequilibriumElement) {
						linkageElementsFound = detectDQA1Linkages(linkageElementsFound, glString, foundElement);
					}
					else {
						linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
					}
				}
			}
		}
		
		return linkageElementsFound;
	}

	/**
	 * @param linkageElementsFound
	 * @param disElement
	 * @param nonCodingVariationLinkage
	 */
	private static Set<DetectedDisequilibriumElement> addMatchedDisequilibriumElement(
			Set<DetectedDisequilibriumElement> linkageElementsFound,
			DetectedDisequilibriumElement foundElement) {
			linkageElementsFound.add(foundElement);
		
		return linkageElementsFound;
	}
}
