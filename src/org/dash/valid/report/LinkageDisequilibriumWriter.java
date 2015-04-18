package org.dash.valid.report;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.dash.valid.cwd.CommonWellDocumentedLoader;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.gl.GLStringConstants;
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
		StringBuffer sb = new StringBuffer("Id: " + findings.getGenotypeList().getId() + GLStringConstants.NEWLINE + "GL String: " + findings.getGenotypeList().getGLString());
		sb.append(GLStringConstants.NEWLINE + GLStringConstants.NEWLINE + "HLA DB Version: " + findings.getHladb() + GLStringConstants.NEWLINE);
		
		CommonWellDocumentedLoader loader = CommonWellDocumentedLoader.getInstance();
		String accession;
		
		for (String allele : findings.getNonCWDAlleles()) {
			sb.append("Allele: " + allele + " not in the CWD list for HLA DB: " + findings.getHladb());
			accession = loader.getAccessionByAllele(allele);
			if (accession != null) {
				sb.append(" (Found under accession: " + accession + " in these HLA DBs: " + 
					loader.getHlaDbsByAccession(accession) + ")");
			}
			sb.append(GLStringConstants.NEWLINE);
		}
		
		sb.append(GLStringConstants.NEWLINE + "Frequencies:  " + Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY)) + GLStringConstants.NEWLINE);
		if (!findings.hasLinkages()) {
			sb.append(GLStringConstants.NEWLINE + "NO LINKAGES FOUND" + GLStringConstants.NEWLINE);
		}
				
		for (DetectedDisequilibriumElement linkage : findings.getLinkages()) {
			sb.append(GLStringConstants.NEWLINE);
			sb.append("We found linkages:" + GLStringConstants.NEWLINE);
			sb.append(linkage.toString());
		}
		
		if (findings.getBcLinkageCount() < DetectedLinkageFindings.EXPECTED_LINKAGES) {
			sb.append(GLStringConstants.NEWLINE);
			sb.append("WARNING: " + (DetectedLinkageFindings.EXPECTED_LINKAGES-findings.getBcLinkageCount()) + " BC Linkage(s) not found" + GLStringConstants.NEWLINE);
		}
		if (findings.getDrdqLinkageCount() < DetectedLinkageFindings.EXPECTED_LINKAGES) {
			sb.append(GLStringConstants.NEWLINE);
			sb.append("WARNING: " + (DetectedLinkageFindings.EXPECTED_LINKAGES-findings.getDrdqLinkageCount()) + " DRDQ Linkage(s) not found" + GLStringConstants.NEWLINE);
		}
		
		sb.append(GLStringConstants.NEWLINE + "***************************************" + GLStringConstants.NEWLINE);
	
		FILE_LOGGER.info(sb.toString());
	}
}
