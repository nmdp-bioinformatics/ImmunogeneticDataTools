package org.dash.valid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
	public static final String DASH = "-";
	public static final String TAB = "\t";
	
    private static final Logger LOGGER = Logger.getLogger(HLALinkageDisequilibrium.class.getName());
	
	List<BCDisequilibriumElement> bcDisequilibriumElements = new ArrayList<BCDisequilibriumElement>();
	List<DRDQDisequilibriumElement> drdqDisequilibriumElements = new ArrayList<DRDQDisequilibriumElement>();
	
	public HLALinkageDisequilibrium() {
		init();
	}
	
	public void init() {
		try {
			loadBCLinkageReferenceData();
			loadDRDQLinkageReferenceData();
			
			LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
		}
		catch (FileNotFoundException fnfe) {
			LOGGER.severe("Couldn't find disequilibrium element reference file.");
			fnfe.printStackTrace();
		}
		catch (IOException ioe) {
			LOGGER.severe("Couldn't load disequilibrium element reference file.");
			ioe.printStackTrace();
		}
	}

	private void loadBCLinkageReferenceData() throws FileNotFoundException, IOException {
		File bcLinkages = new File("resources/BCLinkageDisequilibrium.txt");
		InputStream in = new FileInputStream(bcLinkages);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String row;
		String[] columns;
		while ((row = reader.readLine()) != null) {
			columns = row.split(TAB);

			bcDisequilibriumElements.add(new BCDisequilibriumElement(columns[0], columns[1], columns[2], columns[3]));
		}
		
		reader.close();
	}
	
	private void loadDRDQLinkageReferenceData() throws FileNotFoundException, IOException {
		File drdqLinkages = new File("resources/DRDQLinkageDisequilibrium.txt");
		InputStream in = new FileInputStream(drdqLinkages);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String row;
		String[] columns;
		while ((row = reader.readLine()) != null) {
			columns = row.split(TAB);
		
			drdqDisequilibriumElements.add(new DRDQDisequilibriumElement(columns[0], columns[1], columns[2], columns[3], columns[4], columns[5]));
		}
		
		reader.close();		
	}	
	
	public HashMap<DisequilibriumElement, Boolean> hasDisequilibriumLinkage(LinkageDisequilibriumGenotypeList glString) {
		HashMap<DisequilibriumElement, Boolean> linkageElementsFound = new HashMap<DisequilibriumElement, Boolean>();
		
		for (BCDisequilibriumElement disElement : bcDisequilibriumElements) {
			linkageElementsFound = detectBCLinkages(linkageElementsFound, glString, disElement);
		}
		
		for (DRDQDisequilibriumElement disElement : drdqDisequilibriumElements) {
			linkageElementsFound = detectDRDQLinkages(linkageElementsFound, glString, disElement);
		}
		
		return linkageElementsFound;
	}

	private HashMap<DisequilibriumElement, Boolean> detectBCLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
								LinkageDisequilibriumGenotypeList glString,
								BCDisequilibriumElement disElement) {
		for (List<String> bList : glString.getBAlleles()) {
			for (String bAllele : bList) {
				if (bAllele.equals(disElement.getHlabElement())) {
					linkageElementsFound = detectCLinkages(linkageElementsFound, disElement, glString, true);
				}
				else if (GLStringUtilities.shortenAllele(bAllele).equals(GLStringUtilities.shortenAllele(disElement.getHlabElement()))) {
					linkageElementsFound = detectCLinkages(linkageElementsFound, disElement, glString);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectCLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
								BCDisequilibriumElement disElement,
								LinkageDisequilibriumGenotypeList glString) {
		return detectCLinkages(linkageElementsFound, disElement, glString, false);
	}

	private HashMap<DisequilibriumElement, Boolean> detectCLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
								BCDisequilibriumElement disElement,
								LinkageDisequilibriumGenotypeList glString,
								boolean nonCodingVariationLinkage) {		
		for (List<String> cList : glString.getCAlleles()) {	
			for (String cAllele : cList) {
				if (nonCodingVariationLinkage && cAllele.equals(disElement.getHlacElement())) {
					// perfect hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound,
							disElement, true);
				}
				else if (cAllele.equals(disElement.getHlacElement()) || 
						GLStringUtilities.shortenAllele(cAllele).equals(GLStringUtilities.shortenAllele(disElement.getHlacElement()))) {
					// partial hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound,
							disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectDRDQLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DRDQDisequilibriumElement disElement) {
		for (List<String> drb1List : glString.getDrb1Alleles()) {
			for (String drb1Allele : drb1List) {
				if (drb1Allele.equals(disElement.getHladrb1Element())) {
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, disElement, true);
				}
				else if (GLStringUtilities.shortenAllele(drb1Allele).equals(GLStringUtilities.shortenAllele(disElement.getHladrb1Element()))) {
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectDRB345DQLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
			LinkageDisequilibriumGenotypeList glString,
			DRDQDisequilibriumElement disElement) {
		return detectDRB345DQLinkages(linkageElementsFound, glString, disElement, false);
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectDRB345DQLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
										LinkageDisequilibriumGenotypeList glString,
										DRDQDisequilibriumElement disElement, boolean nonCodingVariationLinkage) {
		if (glString.drb345AppearsHomozygous() && disElement.getHladrb345Element().equals(DASH)) {
			// TODO:  consider whether true should be sent on this call
			linkageElementsFound = detectDQLinkages(linkageElementsFound, glString, disElement);
		} 
		for (List<String> drb345List : glString.getDrb345Alleles()) {
			for (String drb345Allele : drb345List) {
				if (nonCodingVariationLinkage && drb345Allele.equals(disElement.getHladrb345Element())) {
					linkageElementsFound = detectDQLinkages(linkageElementsFound, glString, disElement, true);
				}
				else if (drb345Allele.equals(disElement.getHladrb345Element()) || 
						GLStringUtilities.shortenAllele(drb345Allele).equals(GLStringUtilities.shortenAllele(disElement.getHladrb345Element()))) {
					linkageElementsFound = detectDQLinkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectDQLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound, 
			LinkageDisequilibriumGenotypeList glString,
			DRDQDisequilibriumElement disElement) {
		return detectDQLinkages(linkageElementsFound, glString, disElement, false);
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectDQLinkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound, 
								LinkageDisequilibriumGenotypeList glString,
								DRDQDisequilibriumElement disElement, boolean nonCodingVariationLinkage) {
		for (List<String> dqa1List : glString.getDqa1Alleles()) {
			for (String dqa1Allele : dqa1List) {
				if (nonCodingVariationLinkage && dqa1Allele.equals(disElement.getHladqa1Element())) {
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, disElement, true);
				}
				else if (dqa1Allele.equals(disElement.getHladqa1Element()) ||
						GLStringUtilities.shortenAllele(dqa1Allele).equals(GLStringUtilities.shortenAllele(disElement.getHladqa1Element()))) {
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectDQB1Linkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
			LinkageDisequilibriumGenotypeList glString,
			DRDQDisequilibriumElement disElement) {
		return detectDQB1Linkages(linkageElementsFound, glString, disElement, false);
	}
	
	private HashMap<DisequilibriumElement, Boolean> detectDQB1Linkages(HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
									LinkageDisequilibriumGenotypeList glString,
									DRDQDisequilibriumElement disElement, boolean nonCodingLinkageFound) {
		for (List<String> dqb1List : glString.getDqb1Alleles()) {
			for (String dqb1Allele : dqb1List) {
				if (nonCodingLinkageFound && dqb1Allele.equals(disElement.getHladqb1Element())) {
					// full hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, disElement, true);
				}
				else if (dqb1Allele.equals(disElement.getHladqb1Element()) ||
						GLStringUtilities.shortenAllele(dqb1Allele).equals(GLStringUtilities.shortenAllele(disElement.getHladqb1Element()))) {
					// partial hit
					linkageElementsFound = addMatchedDisequilibriumElement(linkageElementsFound, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private HashMap<DisequilibriumElement, Boolean> addMatchedDisequilibriumElement(
			HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
			DisequilibriumElement disElement) {
		return addMatchedDisequilibriumElement(linkageElementsFound, disElement, false);
	}

	/**
	 * @param linkageElementsFound
	 * @param disElement
	 */
	private HashMap<DisequilibriumElement, Boolean> addMatchedDisequilibriumElement(
			HashMap<DisequilibriumElement, Boolean> linkageElementsFound,
			DisequilibriumElement disElement, boolean nonCodingVariationLinkage) {
		if (nonCodingVariationLinkage == true) {
			linkageElementsFound.put(disElement, Boolean.TRUE);
		}
		else if (!linkageElementsFound.containsKey(disElement)) {
			linkageElementsFound.put(disElement, Boolean.FALSE);
		}
		
		return linkageElementsFound;
	}
}
