package org.dash.valid.report;

import java.io.IOException;
import java.util.logging.Logger;

import org.dash.valid.freq.Frequencies;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.handler.HaplotypePairFileHandler;
import org.dash.valid.handler.HaplotypePairWarningFileHandler;

public class HaplotypePairWriter {	
	private static HaplotypePairWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(HaplotypePairWriter.class.getName());

	static {
		try {
			FILE_LOGGER.addHandler(new HaplotypePairFileHandler());
			FILE_LOGGER.addHandler(new HaplotypePairWarningFileHandler());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private HaplotypePairWriter() {
		
	}
	
	public static HaplotypePairWriter getInstance() {
		if (instance == null) {
			instance = new HaplotypePairWriter();
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
		
		sb.append(GLStringConstants.NEWLINE + "Frequencies:  " + Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY)) + GLStringConstants.NEWLINE);
		
		if (findings.hasBcLinkedPairs() && findings.getFirstBCPair() != null) {
			sb.append(GLStringConstants.NEWLINE + "First BC Haplotype pair:" + GLStringConstants.NEWLINE + findings.getFirstBCPair());
		}
		else {
			sb.append(GLStringConstants.NEWLINE + "WARNING - No BC haplotype pairs detected." + GLStringConstants.NEWLINE);
		}
		
		if (findings.hasDrdqLinkedPairs() && findings.getFirstDRDQPair() != null) {
			sb.append(GLStringConstants.NEWLINE + "First DRDQ Haplotype pair:" + GLStringConstants.NEWLINE + findings.getFirstDRDQPair());
		}
		else {
			sb.append(GLStringConstants.NEWLINE + "WARNING - No DRDQ haplotype pairs detected." + GLStringConstants.NEWLINE);
		}
		
		for (HaplotypePair pair : findings.getLinkedPairs()) {
			if (pair.equals(findings.getFirstBCPair()) || pair.equals(findings.getFirstDRDQPair())) {
				continue;
			}
			else {
				sb.append(GLStringConstants.NEWLINE + "Possible " + (pair.isBCPair() ? "BC " : "DRDQ ") + "Haplotype Pair:" + GLStringConstants.NEWLINE);
			}
			sb.append(pair);
		}
		
		sb.append(GLStringConstants.NEWLINE + "***************************************" + GLStringConstants.NEWLINE);
	
		if (findings.hasAnomalies()) {
			FILE_LOGGER.warning(sb.toString());
		}
		else {
			FILE_LOGGER.info(sb.toString());
		}
	}
}
