package org.dash.valid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.handler.ProgressConsoleHandler;
import org.dash.valid.report.CommonWellDocumentedWriter;
import org.dash.valid.report.DetectedFindingsWriter;
import org.dash.valid.report.DetectedLinkageFindings;
import org.dash.valid.report.HaplotypePairWriter;
import org.dash.valid.report.LinkageDisequilibriumWriter;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;

public class LinkageDisequilibriumAnalyzer {		
    private static final Logger LOGGER = Logger.getLogger(LinkageDisequilibriumAnalyzer.class.getName());
    
    static {
    	try {
			LogManager.getLogManager().readConfiguration(LinkageDisequilibriumAnalyzer.class.getClassLoader().getResourceAsStream("logging.properties"));
			LOGGER.addHandler(new ProgressConsoleHandler());
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
    }
    
	public static void main(String[] args) {
		analyzeGLStringFiles(args);
		
		for (Handler handler : LogManager.getLogManager().getLogger(LinkageDisequilibriumWriter.class.getName()).getHandlers()) {
			handler.close();
		}
		
		for (Handler handler : LogManager.getLogManager().getLogger(HaplotypePairWriter.class.getName()).getHandlers()) {
			handler.close();
		}
		
		for (Handler handler : LogManager.getLogManager().getLogger(CommonWellDocumentedWriter.class.getName()).getHandlers()) {
			handler.close();
		}
		
		DetectedFindingsWriter.getInstance().closeWriters();
	}
	
	private static void analyzeGLStringFiles(String[] filenames) {		
		for (int i=0;i<filenames.length;i++) {
			LOGGER.info("Processing file: " + filenames[i] + " (" + (i+1) + " of " + filenames.length + ")");
			analyzeGLStringFile(filenames[i]);		
		}
	}

	/**
	 * @param filename
	 */
	public static void analyzeGLStringFile(String filename) {				
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile(filename);
		List<DetectedLinkageFindings> findingsList = null;
		
		try {
			findingsList = detectLinkages(glStrings);
			
			for (DetectedLinkageFindings findings : findingsList) {
				LinkageDisequilibriumWriter.getInstance().reportDetectedLinkages(findings);
				HaplotypePairWriter.getInstance().reportDetectedLinkages(findings);
				CommonWellDocumentedWriter.getInstance().reportCommonWellDocumented(findings);
				DetectedFindingsWriter.getInstance().reportDetectedFindings(findings);
			}
		}
		catch (IOException e) {
			LOGGER.warning("Could not use file handler:  LinkageDisequiibrimFileHandler");
		}		
	}

	/**
	 * @param glStrings
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	private static List<DetectedLinkageFindings> detectLinkages(Map<String, String> glStrings) throws SecurityException, IOException {
		LinkageDisequilibriumGenotypeList linkedGLString;
		String glString;
		List<DetectedLinkageFindings> findingsList = new ArrayList<DetectedLinkageFindings>();
		
		int idx = 1;
		for (String key : glStrings.keySet()) {
			glString = glStrings.get(key);
			if (!GLStringUtilities.validateGLStringFormat(glString)) {
				glString = GLStringUtilities.fullyQualifyGLString(glString);
			}
			
			linkedGLString = new LinkageDisequilibriumGenotypeList(key, glString);
			LOGGER.info("Processing gl string " + idx + " of " + glStrings.size() + " (" + (idx*100)/glStrings.size() + "%)");
			if (!linkedGLString.checkAmbiguitiesThresholds()) {
				LOGGER.info("GL String skipped due to volume of ambiguities, proteins and/or uncommon alleles");
			}
			
			findingsList.add(detectLinkages(linkedGLString));
			idx++;
		}
		
		return findingsList;
	}
	
	public static DetectedLinkageFindings detectLinkages(MultilocusUnphasedGenotype mug) {
		LinkageDisequilibriumGenotypeList linkedGLString = new LinkageDisequilibriumGenotypeList(mug);
		DetectedLinkageFindings findings = detectLinkages(linkedGLString);

		return findings;
	}

	private static DetectedLinkageFindings detectLinkages(LinkageDisequilibriumGenotypeList linkedGLString) {
		DetectedLinkageFindings findings = HLALinkageDisequilibrium.hasDisequilibriumLinkage(linkedGLString);
				
		return findings;
	}
}
