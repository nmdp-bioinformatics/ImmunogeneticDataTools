package org.dash.valid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.dash.valid.gl.GLString;

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
	
	public List<DisequilibriumElement> hasDisequilibriumLinkage(GLString glString) {
		List<DisequilibriumElement> linkageElementsFound = new ArrayList<DisequilibriumElement>();
		
		for (BCDisequilibriumElement disElement : bcDisequilibriumElements) {
			linkageElementsFound = detectBCLinkages(linkageElementsFound, glString, disElement);
		}
		
		for (DRDQDisequilibriumElement disElement : drdqDisequilibriumElements) {
			linkageElementsFound = detectDRDQLinkages(linkageElementsFound, glString, disElement);
		}
		
		return linkageElementsFound;
	}

	private List<DisequilibriumElement> detectBCLinkages(List<DisequilibriumElement> linkageElementsFound,
								GLString glString,
								BCDisequilibriumElement disElement) {
		for (List<String> bList : glString.getBAlleles()) {
			for (String bAllele : bList) {
				if (bAllele.equals(disElement.getHlabElement())) {
					linkageElementsFound = detectCLinkages(linkageElementsFound, disElement, glString);
				}
			}
		}
		
		return linkageElementsFound;
	}

	private List<DisequilibriumElement> detectCLinkages(List<DisequilibriumElement> linkageElementsFound,
								BCDisequilibriumElement disElement,
								GLString glString) {
		for (List<String> cList : glString.getCAlleles()) {	
			for (String cAllele : cList) {
				if (cAllele.equals(disElement.getHlacElement())) {
					//hit!!!
					linkageElementsFound.add(disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private List<DisequilibriumElement> detectDRDQLinkages(List<DisequilibriumElement> linkageElementsFound,
									GLString glString,
									DRDQDisequilibriumElement disElement) {
		for (List<String> drb1List : glString.getDrb1Alleles()) {
			for (String drb1Allele : drb1List) {
				if (drb1Allele.equals(disElement.getHladrb1Element())) {
					linkageElementsFound = detectDRB345DQLinkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private List<DisequilibriumElement> detectDRB345DQLinkages(List<DisequilibriumElement> linkageElementsFound,
										GLString glString,
										DRDQDisequilibriumElement disElement) {
		if (glString.drb345AppearsHomozygous() && disElement.getHladrb345Element().equals(DASH)) {
			linkageElementsFound = detectDQLinkages(linkageElementsFound, glString, disElement);
		} 
		for (List<String> drb345List : glString.getDrb345Alleles()) {
			for (String drb345Allele : drb345List) {
				if (drb345Allele.equals(disElement.getHladrb345Element())) {
					linkageElementsFound = detectDQLinkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private List<DisequilibriumElement> detectDQLinkages(List<DisequilibriumElement> linkageElementsFound, 
								GLString glString,
								DRDQDisequilibriumElement disElement) {
		for (List<String> dqa1List : glString.getDqa1Alleles()) {
			for (String dqa1Allele : dqa1List) {
				if (dqa1Allele.equals(disElement.getHladqa1Element())) {
					linkageElementsFound = detectDQB1Linkages(linkageElementsFound, glString, disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
	
	private List<DisequilibriumElement> detectDQB1Linkages(List<DisequilibriumElement> linkageElementsFound,
									GLString glString,
									DRDQDisequilibriumElement disElement) {
		for (List<String> dqb1List : glString.getDqb1Alleles()) {
			for (String dqb1Allele : dqb1List) {
				if (dqb1Allele.equals(disElement.getHladqb1Element())) {
					// hit
					linkageElementsFound.add(disElement);
				}
			}
		}
		
		return linkageElementsFound;
	}
}
