package org.dash.valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.base.BaseDRDQDisequilibriumElement;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.BCHaplotype;
import org.dash.valid.gl.haplo.DRDQHaplotype;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.HaplotypeComparator;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.gl.haplo.HaplotypeSet;
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
	private static HLADatabaseVersion hladb;
	private static Integer LINKED_HAPLOTYPES_THRESHOLD = new Integer(360);
	
    private static final Logger LOGGER = Logger.getLogger(HLALinkageDisequilibrium.class.getName());
		
	static {
		hladb = HLADatabaseVersion.lookup(System.getProperty(HLADatabaseVersion.HLADB_PROPERTY));
		String hapThreshold;
		if ((hapThreshold = System.getProperty("org.dash.hapThreshold")) != null) {
			LINKED_HAPLOTYPES_THRESHOLD = new Integer(hapThreshold);
		}
	}
			
	public static DetectedLinkageFindings hasDisequilibriumLinkage(LinkageDisequilibriumGenotypeList glString) {
		DetectedLinkageFindings findings = new DetectedLinkageFindings();
		
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
		List<Haplotype> linkedHaplotypes = new ArrayList<Haplotype>();
		Set<DetectedDisequilibriumElement> linkageElementsFound = new LinkageElementsSet(new DisequilibriumElementComparator());
		
		Set<String> notCommon = GLStringUtilities.checkCommonWellDocumented(glString.getGLString());
				
		for (BCDisequilibriumElement disElement : freqLoader.getBCDisequilibriumElements()) {
			linkedHaplotypes.addAll(detectBCLinkages(glString, disElement));
		}
		
		LOGGER.info(linkedHaplotypes.size() + " linked BC haplotypes");
		if (linkedHaplotypes.size() > LINKED_HAPLOTYPES_THRESHOLD) {
			LOGGER.warning("Linked BC haplotype count: " + linkedHaplotypes.size() + " exceeds configured threshold: " + LINKED_HAPLOTYPES_THRESHOLD + ".  Not calculating relative frequencies.");
		}
		else {		
			for (Haplotype haplotype1 : linkedHaplotypes) {	
				linkageElementsFound.add(haplotype1.getLinkage());
				for (Haplotype haplotype2 : linkedHaplotypes) {
					if ((!glString.checkHomozygous(Locus.HLA_B) && 
							GLStringUtilities.checkFromSameHaplotype(Locus.HLA_B, haplotype1, haplotype2)) ||
							(!glString.checkHomozygous(Locus.HLA_C) &&
							GLStringUtilities.checkFromSameHaplotype(Locus.HLA_C, haplotype1, haplotype2))) {
									continue;
					}
					linkedPairs.add(new HaplotypePair(haplotype1, haplotype2));
					findings.setBcLinkedPairs(true);
				}
			}
		}
		
		linkedHaplotypes.clear();
		
		for (DRDQDisequilibriumElement disElement : freqLoader.getDRDQDisequilibriumElements()) {
			linkedHaplotypes.addAll(detectDRDQLinkages(glString, disElement));
		}
		
		LOGGER.info(linkedHaplotypes.size() + " linked DRDQ haplotypes");
		
		if (linkedHaplotypes.size() > LINKED_HAPLOTYPES_THRESHOLD) {
			LOGGER.warning("Linked DRDQ haplotype count: " + linkedHaplotypes.size() + " exceeds configured threshold: " + LINKED_HAPLOTYPES_THRESHOLD + ".  Not calculating relative frequencies.");
		}
		else {	
			for (Haplotype haplotype1 : linkedHaplotypes) {
				int[] hap1Instances = haplotype1.getHaplotypeInstances();
				LOGGER.fine("Linked haplotype (1): " + haplotype1 + " and instances: " + hap1Instances[0] + ", " + hap1Instances[1] + ", " + hap1Instances[2] + ", " + hap1Instances[3]);
				linkageElementsFound.add(haplotype1.getLinkage());
				for (Haplotype haplotype2 : linkedHaplotypes) {
					int[] hap2Instances = haplotype2.getHaplotypeInstances();
					LOGGER.fine("Linkedhaplotype (2): " + haplotype2 + " and instances: " + hap2Instances[0] + ", " + hap2Instances[1] + ", " + hap2Instances[2] + ", " + hap2Instances[3]);
					if ((!glString.checkHomozygous(Locus.HLA_DRB1) &&
							GLStringUtilities.checkFromSameHaplotype(Locus.HLA_DRB1, haplotype1, haplotype2)) ||
							(!glString.checkHomozygous(Locus.HLA_DRB345) && 
									GLStringUtilities.checkFromSameHaplotype(Locus.HLA_DRB345, haplotype1, haplotype2)) ||
							(!glString.checkHomozygous(Locus.HLA_DQB1) &&
									GLStringUtilities.checkFromSameHaplotype(Locus.HLA_DQB1, haplotype1, haplotype2)) ||
							(DRDQHaplotype.EXPECTING_DQA1 && !glString.checkHomozygous(Locus.HLA_DQA1) &&
									GLStringUtilities.checkFromSameHaplotype(Locus.HLA_DQA1, haplotype1, haplotype2))) {
						continue;
					}
					linkedPairs.add(new HaplotypePair(haplotype1, haplotype2));
					findings.setDrdqLinkedPairs(true);
				}
			}
		}
		
		LOGGER.info(linkedPairs.size() + " linkedPairs");
		
		findings.setGenotypeList(glString);
		findings.addLinkages(linkageElementsFound);
		findings.setLinkedPairs(linkedPairs);
		findings.setNonCWDAlleles(notCommon);
		findings.setHladb(hladb);
		
		return findings;
	}

	private static Set<Haplotype> detectBCLinkages(LinkageDisequilibriumGenotypeList glString,
								BCDisequilibriumElement disElement) {		
		Set<BCHaplotype> possibleHaplotypes = glString.getPossibleBCHaplotypes();
				
		Set<Haplotype> linkedHaplotypes = new HaplotypeSet(new HaplotypeComparator());
		
		for (BCHaplotype haplotype : possibleHaplotypes) {
			linkedHaplotypes = detectBCLinkages(linkedHaplotypes, disElement, haplotype);
		}
		
		return linkedHaplotypes;
	}

	/**
	 * @param linkageElementsFound
	 * @param glString
	 * @param disElement
	 * @param haplotype
	 * @return
	 */
	private static Set<Haplotype> detectBCLinkages(
			Set<Haplotype> linkedHaplotypes,
			BCDisequilibriumElement disElement, BCHaplotype haplotype) {
		LinkageHitDegree hitDegree;
		for (String bAllele : haplotype.getAlleles(Locus.HLA_B)) {
			hitDegree = GLStringUtilities.fieldLevelComparison(bAllele, disElement.getHlabElement());
			if (hitDegree != null) {
				DetectedBCDisequilibriumElement foundElement = new DetectedBCDisequilibriumElement(disElement);
				foundElement.setHitDegree(Locus.HLA_B, hitDegree);

				linkedHaplotypes = detectCLinkages(linkedHaplotypes, foundElement, haplotype);
			}
			else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(bAllele, disElement.getHlabElement())) != null) {
				DetectedBCDisequilibriumElement foundElement = new DetectedBCDisequilibriumElement(disElement);
				foundElement.setHitDegree(Locus.HLA_B, hitDegree);
				
				linkedHaplotypes = detectCLinkages(linkedHaplotypes, foundElement, haplotype);
			}
		}
		return linkedHaplotypes;
	}
	
	private static Set<Haplotype> detectCLinkages(Set<Haplotype> linkedHaplotypes,
			DetectedBCDisequilibriumElement foundElement,
			BCHaplotype haplotype) {	
		LinkageHitDegree hitDegree;
		
		for (String cAllele : haplotype.getAlleles(Locus.HLA_C)) {
			hitDegree = GLStringUtilities.fieldLevelComparison(cAllele, foundElement.getDisequilibriumElement().getHlacElement());
			
			if (hitDegree != null) {
				foundElement.setHitDegree(Locus.HLA_C, hitDegree);
				linkedHaplotypes.add(new BCHaplotype(foundElement, haplotype));
			}
			else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(cAllele, foundElement.getDisequilibriumElement().getHlacElement())) != null) {
				foundElement.setHitDegree(Locus.HLA_C, hitDegree);
				linkedHaplotypes.add(new BCHaplotype(foundElement, haplotype));
			}
		}

		return linkedHaplotypes;
	}
	
	private static Set<Haplotype> detectDRDQLinkages(LinkageDisequilibriumGenotypeList glString,
			DRDQDisequilibriumElement disElement) {		
		Set<DRDQHaplotype> possibleHaplotypes = glString.getPossibleDRDQHaplotypes();
		
		Set<Haplotype> linkedHaplotypes = new HaplotypeSet(new HaplotypeComparator());
		
		for (DRDQHaplotype haplotype : possibleHaplotypes) {
			linkedHaplotypes = detectDRDQLinkages(linkedHaplotypes, haplotype,
					glString, disElement);
		}
		
		return linkedHaplotypes;
	}
	
	private static Set<Haplotype> detectDRDQLinkages(Set<Haplotype> linkedHaplotypes,
									DRDQHaplotype haplotype, LinkageDisequilibriumGenotypeList glString,
									DRDQDisequilibriumElement disElement) {
		LinkageHitDegree hitDegree;
		
		for (String drb1Allele : haplotype.getAlleles(Locus.HLA_DRB1)) {
			hitDegree = GLStringUtilities.fieldLevelComparison(drb1Allele, disElement.getHladrb1Element());
			
			if (hitDegree != null) {
				DetectedDRDQDisequilibriumElement foundElement = new DetectedDRDQDisequilibriumElement(disElement);
				foundElement.setHitDegree(Locus.HLA_DRB1, hitDegree);
				linkedHaplotypes = detectDRB345DQLinkages(linkedHaplotypes, haplotype, glString, foundElement);
			}
			else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(drb1Allele, disElement.getHladrb1Element())) != null) {
				DetectedDRDQDisequilibriumElement foundElement = new DetectedDRDQDisequilibriumElement(disElement);
				foundElement.setHitDegree(Locus.HLA_DRB1, hitDegree);
				linkedHaplotypes = detectDRB345DQLinkages(linkedHaplotypes, haplotype, glString, foundElement);
			}
		}
		
		return linkedHaplotypes;
	}
	
	private static Set<Haplotype> detectDRB345DQLinkages(Set<Haplotype> linkedHaplotypes,
										DRDQHaplotype haplotype, LinkageDisequilibriumGenotypeList glString,
										DetectedDRDQDisequilibriumElement foundElement) {
		if (glString.checkHomozygous(Locus.HLA_DRB345) && (foundElement.getDisequilibriumElement().getHladrb345Element().equals(DASH) || foundElement.getDisequilibriumElement().getHladrb345Element().equals(GLStringConstants.NNNN))) {
			LinkageHitDegree hitDegree = new LinkageHitDegree(GLStringUtilities.P_GROUP_LEVEL, GLStringUtilities.P_GROUP_LEVEL, GLStringConstants.NNNN, GLStringConstants.NNNN);
			foundElement.setHitDegree(Locus.HLA_DRB345, hitDegree);
			linkedHaplotypes = detectDQB1Linkages(linkedHaplotypes, haplotype, foundElement);
		} 
		
		LinkageHitDegree hitDegree;
		
		for (String drb345Allele : haplotype.getAlleles(Locus.HLA_DRB345)) {
			hitDegree = GLStringUtilities.fieldLevelComparison(drb345Allele, foundElement.getDisequilibriumElement().getHladrb345Element());
			
			if (hitDegree != null) {
				foundElement.setHitDegree(Locus.HLA_DRB345, hitDegree);
				linkedHaplotypes = detectDQB1Linkages(linkedHaplotypes, haplotype, foundElement);
			}
			else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(drb345Allele, foundElement.getDisequilibriumElement().getHladrb345Element())) != null) {
				foundElement.setHitDegree(Locus.HLA_DRB345, hitDegree);
				linkedHaplotypes = detectDQB1Linkages(linkedHaplotypes, haplotype, foundElement);
			}
		}
		
		return linkedHaplotypes;
	}
	
	private static Set<Haplotype> detectDQA1Linkages(Set<Haplotype> linkedHaplotypes, 
								DRDQHaplotype haplotype,
								DetectedDRDQDisequilibriumElement foundElement) {
		LinkageHitDegree hitDegree;
		
		for (String dqa1Allele : haplotype.getAlleles(Locus.HLA_DQA1)) {
			hitDegree = GLStringUtilities.fieldLevelComparison(dqa1Allele, ((BaseDRDQDisequilibriumElement)foundElement.getDisequilibriumElement()).getHladqa1Element());
			
			if (hitDegree != null) {
				foundElement.setHitDegree(Locus.HLA_DQA1, hitDegree);
				linkedHaplotypes.add(new DRDQHaplotype(foundElement, haplotype));
			}
			else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(dqa1Allele, ((BaseDRDQDisequilibriumElement)foundElement.getDisequilibriumElement()).getHladqa1Element())) != null) {
				foundElement.setHitDegree(Locus.HLA_DQA1, hitDegree);
				linkedHaplotypes.add(new DRDQHaplotype(foundElement, haplotype));
			}
		}
		
		return linkedHaplotypes;
	}
	
	private static Set<Haplotype> detectDQB1Linkages(Set<Haplotype> linkedHaplotypes,
									DRDQHaplotype haplotype,
									DetectedDRDQDisequilibriumElement foundElement) {
		LinkageHitDegree hitDegree;
		
		for (String dqb1Allele : haplotype.getAlleles(Locus.HLA_DQB1)) {
			hitDegree = GLStringUtilities.fieldLevelComparison(dqb1Allele, foundElement.getDisequilibriumElement().getHladqb1Element());
			
			if (hitDegree != null) {
				foundElement.setHitDegree(Locus.HLA_DQB1, hitDegree);
				if (foundElement.getDisequilibriumElement() instanceof BaseDRDQDisequilibriumElement) {
					linkedHaplotypes = detectDQA1Linkages(linkedHaplotypes, haplotype, foundElement);
				}
				else {
					linkedHaplotypes.add(new DRDQHaplotype(foundElement, haplotype));
				}
			}
			else if ((hitDegree = GLStringUtilities.checkAntigenRecognitionSite(dqb1Allele, foundElement.getDisequilibriumElement().getHladqb1Element())) != null) {
				foundElement.setHitDegree(Locus.HLA_DQB1, hitDegree);
				if (foundElement.getDisequilibriumElement() instanceof BaseDRDQDisequilibriumElement) {
					linkedHaplotypes = detectDQA1Linkages(linkedHaplotypes, haplotype, foundElement);
				}
				else {
					linkedHaplotypes.add(new DRDQHaplotype(foundElement, haplotype));
				}
			}
		}
		
		return linkedHaplotypes;
	}
}
