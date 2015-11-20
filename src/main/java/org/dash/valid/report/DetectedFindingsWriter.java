package org.dash.valid.report;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import org.dash.valid.Linkages;
import org.dash.valid.Locus;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;

public class DetectedFindingsWriter {	
	private static DetectedFindingsWriter instance = null;
	private static Logger LOGGER = Logger.getLogger(DetectedFindingsWriter.class.getName());
	private static FileWriter fileWriter;
	private static PrintWriter printWriter;

	static {
		try {
			fileWriter = new FileWriter("./detectedFindings.csv");
			printWriter = new PrintWriter(fileWriter);
			
		} catch (IOException e) {
			LOGGER.warning("Couldn't write to file detectedFindings.csv");
		}
	}
	
	public void closeWriters() {
		printWriter.flush();
		printWriter.close();
		
		try {
			fileWriter.close();
		}
		catch (IOException ioe) {
			LOGGER.warning("Couldn't close fileWriter after writing to detectedFindings.csv.");
		}
	}
	
	private DetectedFindingsWriter() {
		
	}
	
	public static DetectedFindingsWriter getInstance() {
		if (instance == null) {
			instance = new DetectedFindingsWriter();
		}
		
		return instance;
	}
	/**
	 * @param linkagesFound
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public synchronized void reportDetectedFindings(DetectedLinkageFindings findings) throws SecurityException, IOException {				
		printWriter.write(findings.getGenotypeList().getId() + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_A) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_B) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_C) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_DRB1) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_DRB345) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_DQB1) + GLStringConstants.COMMA);
		for (Linkages linkage : HLAFrequenciesLoader.getInstance().getLinkages()) {
			printWriter.write(findings.getLinkageCount(linkage.getLoci()) + GLStringConstants.COMMA);		
		}
		printWriter.write(GLStringConstants.NEWLINE);
	}
}
