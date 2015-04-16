package org.dash.valid.report;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.dash.valid.cwd.CommonWellDocumentedLoader;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.handler.LinkageDisequilibriumFileHandler;

public class LinkageDisequilibriumWriter {	
	private static LinkageDisequilibriumWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(LinkageDisequilibriumWriter.class.getName());
	private static FileHandler handler;
	private static final String NEWLINE = "\n";

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
		StringBuffer sb = new StringBuffer("Id: " + findings.getGenotypeList().getId() + NEWLINE + "GL String: " + findings.getGenotypeList().getGLString());
		sb.append(NEWLINE + NEWLINE + "HLA DB Version: " + findings.getHladb() + NEWLINE);
		
		CommonWellDocumentedLoader loader = CommonWellDocumentedLoader.getInstance();
		String accession;
		
		for (String allele : findings.getNonCWDAlleles()) {
			sb.append("Allele: " + allele + " not in the CWD list for HLA DB: " + findings.getHladb());
			accession = loader.getAccessionByAllele(allele);
			if (accession != null) {
				sb.append(" (Found under accession: " + accession + " in these HLA DBs: " + 
					loader.getHlaDbsByAccession(accession) + ")");
			}
			sb.append(NEWLINE);
		}
		
		sb.append("\nFrequencies:  " + Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY)) + NEWLINE);
		if (!findings.hasLinkages()) {
			sb.append(NEWLINE + "NO LINKAGES FOUND" + NEWLINE);
		}
				
		for (DetectedDisequilibriumElement linkage : findings.getLinkages()) {
			sb.append(NEWLINE);
			sb.append("We found linkages:" + NEWLINE);
			sb.append(linkage.toString());
		}
		
		if (findings.getBcLinkageCount() < DetectedLinkageFindings.EXPECTED_LINKAGES) {
			sb.append(NEWLINE);
			sb.append("WARNING: " + (DetectedLinkageFindings.EXPECTED_LINKAGES-findings.getBcLinkageCount()) + " BC Linkage(s) not found" + NEWLINE);
		}
		if (findings.getDrdqLinkageCount() < DetectedLinkageFindings.EXPECTED_LINKAGES) {
			sb.append(NEWLINE);
			sb.append("WARNING: " + (DetectedLinkageFindings.EXPECTED_LINKAGES-findings.getDrdqLinkageCount()) + " DRDQ Linkage(s) not found" + NEWLINE);
		}
		
		sb.append(NEWLINE + "***************************************" + NEWLINE);
	
		FILE_LOGGER.info(sb.toString());
	}
}
