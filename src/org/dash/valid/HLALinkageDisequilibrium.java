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
	private static final int P_GROUP_LEVEL = 2;
		
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
		for (Set<String> bList : glString.getBAlleles()) {
			for (String bAllele : bList) {
				int hitBlock = GLStringUtilities.fieldLevelComparison(bAllele, disElement.getHlabElement());
				if (hitBlock >= 0) {
					DetectedBCDisequilibriumElement foundElement = new DetectedBCDisequilibriumElement(disElement);
					foundElement.setbHitDegree(new LinkageHitDegree(hitBlock, bAllele));
					
					linkageElementsFound = detectCLinkages(linkageElementsFound, foundElement, glString);
				}
				else if (GLStringUtilities.checkAntigenRecognitionSite(bAllele, disElement.getHlabElement())) {
					DetectedBCDisequilibriumElement foundElement = new DetectedBCDisequilibriumElement(disElement);
					foundElement.setbHitDegree(new LinkageHitDegree(P_GROUP_LEVEL, bAllele));
					
					linkageElementsFound = detectCLinkages(linkageElementsFound, foundElement, glString);
				}
			}
		}
		
		return linkageElementsFound;
	}

	private static Set<DetectedDisequilibriumElement> detectCLinkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
								DetectedBCDisequilibriumElement foundElement,
								LinkageDisequilibriumGenotypeList glString) {	
		for (Set<String> cList : glString.getCAlleles()) {	
			for (String cAllele : cList) {
				int hitBlock = GLStringUtilities.fieldLevelComparison(cAllele, foundElement.getDisequilibriumElement().getHlacElement());
				
				if (hitBlock >= 0) {
					foundElement.setcHitDegree(new LinkageHitDegree(hitBlock, cAllele));
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
				else if (GLStringUtilities.checkAntigenRecognitionSite(cAllele, foundElement.getDisequilibriumElement().getHlacElement())) {
					foundElement.setcHitDegree(new LinkageHitDegree(P_GROUP_LEVEL, cAllele));
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Set<DetectedDisequilibriumElement> detectDRDQLinkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DRDQDisequilibriumElement disElement) {
		for (Set<String> drb1List : glString.getDrb1Alleles()) {
			for (String drb1Allele : drb1List) {
				int hitBlock = GLStringUtilities.fieldLevelComparison(drb1Allele, disElement.getHladrb1Element());
				
				if (hitBlock >= 0) {
					DetectedDRDQDisequilibriumElement foundElement = new DetectedDRDQDisequilibriumElement(disElement);
					foundElement.setDrb1HitDegree(new LinkageHitDegree(hitBlock, drb1Allele));
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, foundElement);
				}
				else if (GLStringUtilities.checkAntigenRecognitionSite(drb1Allele, disElement.getHladrb1Element())) {
					DetectedDRDQDisequilibriumElement foundElement = new DetectedDRDQDisequilibriumElement(disElement);
					foundElement.setDrb1HitDegree(new LinkageHitDegree(P_GROUP_LEVEL, drb1Allele));
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
		for (Set<String> drb345List : glString.getDrb345Alleles()) {
			for (String drb345Allele : drb345List) {
				int hitBlock = GLStringUtilities.fieldLevelComparison(drb345Allele, foundElement.getDisequilibriumElement().getHladrb345Element());
				
				if (hitBlock >= 0) {
					foundElement.setDrb345HitDegree(new LinkageHitDegree(hitBlock, drb345Allele));
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, foundElement);
				}
				else if (GLStringUtilities.checkAntigenRecognitionSite(drb345Allele, foundElement.getDisequilibriumElement().getHladrb345Element())) {
					foundElement.setDrb345HitDegree(new LinkageHitDegree(P_GROUP_LEVEL, drb345Allele));
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, foundElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Set<DetectedDisequilibriumElement> detectDQA1Linkages(Set<DetectedDisequilibriumElement> linkageElementsFound, 
								LinkageDisequilibriumGenotypeList glString,
								DetectedDRDQDisequilibriumElement foundElement) {
		for (Set<String> dqa1List : glString.getDqa1Alleles()) {
			for (String dqa1Allele : dqa1List) {
				int hitBlock = GLStringUtilities.fieldLevelComparison(dqa1Allele, ((BaseDRDQDisequilibriumElement)foundElement.getDisequilibriumElement()).getHladqa1Element());
				
				if (hitBlock >= 0) {
					foundElement.setDqa1HitDegree(new LinkageHitDegree(hitBlock, dqa1Allele));
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
				else if (GLStringUtilities.checkAntigenRecognitionSite(dqa1Allele, ((BaseDRDQDisequilibriumElement)foundElement.getDisequilibriumElement()).getHladqa1Element())) {
					foundElement.setDqa1HitDegree(new LinkageHitDegree(P_GROUP_LEVEL, dqa1Allele));
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private static Set<DetectedDisequilibriumElement> detectDQB1Linkages(Set<DetectedDisequilibriumElement> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DetectedDRDQDisequilibriumElement foundElement) {
		for (Set<String> dqb1List : glString.getDqb1Alleles()) {
			for (String dqb1Allele : dqb1List) {
				int hitBlock = GLStringUtilities.fieldLevelComparison(dqb1Allele, foundElement.getDisequilibriumElement().getHladqb1Element());
				
				if (hitBlock >= 0) {
					foundElement.setDqb1HitDegree(new LinkageHitDegree(hitBlock, dqb1Allele));
					if (foundElement.getDisequilibriumElement() instanceof BaseDRDQDisequilibriumElement) {
						linkageElementsFound = detectDQA1Linkages(linkageElementsFound, glString, foundElement);
					}
					else {
						linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, foundElement);
					}
				}
				else if (GLStringUtilities.checkAntigenRecognitionSite(dqb1Allele, foundElement.getDisequilibriumElement().getHladqb1Element())) {
					foundElement.setDqb1HitDegree(new LinkageHitDegree(P_GROUP_LEVEL, dqb1Allele));
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
