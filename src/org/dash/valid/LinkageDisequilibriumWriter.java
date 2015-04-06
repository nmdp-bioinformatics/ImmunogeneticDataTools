package org.dash.valid;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import org.dash.valid.handler.LinkageDisequilibriumFileHandler;

public class LinkageDisequilibriumWriter {	
	private static LinkageDisequilibriumWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(LinkageDisequilibriumWriter.class.getName());
	private static FileHandler handler;

	static {
		try {
			handler = new LinkageDisequilibriumFileHandler();
			FILE_LOGGER.addHandler(handler);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private LinkageDisequilibriumWriter() {
		
	}
	
	public static FileHandler getHandler() {
		return handler;
	}
	
	public static LinkageDisequilibriumWriter getInstance() {
		if (instance == null) {
			instance = new LinkageDisequilibriumWriter();
		}
		
		return instance;
	}
	/**
	 * @param linkagesFound
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public synchronized void reportDetectedLinkages(DetectedLinkageFindings findings) throws SecurityException, IOException {				
		StringBuffer sb = new StringBuffer("Id: " + findings.getGenotypeList().getId() + "\nGL String: " + findings.getGenotypeList().getGLString());
		sb.append("\nHLA DB Version: " + findings.getHladb());
		
		for (String allele : findings.getNonCWDAlleles()) {
			sb.append("\nAllele: " + allele + " is not in the Common Well Documented list\n");
		}
		
		if (!findings.hasLinkages()) {
			sb.append("\n\n");
			sb.append("NO LINKAGES FOUND\n");
		}
				
		for (Object linkage : findings.getLinkages().keySet()) {
			sb.append("\n\n");
			Boolean value = findings.getLinkages().get(linkage);
			if (value == null) {
				System.out.println("The boolean is null!!!");
			}
			if (findings.getLinkages().get(linkage).equals(Boolean.TRUE)) {
				sb.append("We found perfect linkages:\n");
			}
			else {
				sb.append("We found partial linkages:\n");
			}
			sb.append(linkage);
		}
		
		if (findings.getBcLinkageCount() < DetectedLinkageFindings.EXPECTED_LINKAGES) {
			sb.append("\n");
			sb.append("WARNING: " + (DetectedLinkageFindings.EXPECTED_LINKAGES-findings.getBcLinkageCount()) + " BC Linkage(s) not found\n");
		}
		if (findings.getDrdqLinkageCount() < DetectedLinkageFindings.EXPECTED_LINKAGES) {
			sb.append("\n");
			sb.append("WARNING: " + (DetectedLinkageFindings.EXPECTED_LINKAGES-findings.getDrdqLinkageCount()) + " DRDQ Linkage(s) not found\n");
		}
		
		if (findings.hasCommonRaceElements()) {
			sb.append("\n\n");
			sb.append("Common race element(s) found: " + findings.getCommonRaceElements() + "\n");
		}
		else {
			sb.append("\nWARNING: Common Races not found\n");
		}
		
		sb.append("\n***************************************\n");
	
		FILE_LOGGER.info(sb.toString());
	}
}
