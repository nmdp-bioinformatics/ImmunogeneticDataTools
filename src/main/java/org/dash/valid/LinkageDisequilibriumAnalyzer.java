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
import org.nmdp.gl.MultilocusUnphasedGenotype;

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
		try {
			analyzeGLStringFiles(args);
		} catch (IOException e) {
			LOGGER.severe("Unable to process.  Please check log files and configuration.");
			return;
		}
		
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
	
	private static void analyzeGLStringFiles(String[] filenames) throws IOException {		
		for (int i=0;i<filenames.length;i++) {
			LOGGER.info("Processing file: " + filenames[i] + " (" + (i+1) + " of " + filenames.length + ")");
			analyzeGLStringFile(filenames[i]);		
		}		
	}

	/**
	 * @param filename
	 */
	public static void analyzeGLStringFile(String filename) throws IOException {				
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile(filename);
		List<DetectedLinkageFindings> findingsList = null;
		
		findingsList = detectLinkages(glStrings);
		
		for (DetectedLinkageFindings findings : findingsList) {
			LinkageDisequilibriumWriter.getInstance().reportDetectedLinkages(findings);
			HaplotypePairWriter.getInstance().reportDetectedLinkages(findings);
			CommonWellDocumentedWriter.getInstance().reportCommonWellDocumented(findings);
			DetectedFindingsWriter.getInstance().reportDetectedFindings(findings);
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
			// TODO:  Actually implement by skipping the record
			if (!linkedGLString.checkAmbiguitiesThresholds()) {
				LOGGER.info("GL String contains an unusual number of ambiguities, proteins and/or uncommon alleles");
			}
			
			findingsList.add(detectLinkages(linkedGLString));
			idx++;
		}
		
		return findingsList;
	}
	
	public static DetectedLinkageFindings detectLinkages(MultilocusUnphasedGenotype mug) throws IOException {
		LinkageDisequilibriumGenotypeList linkedGLString = new LinkageDisequilibriumGenotypeList(mug);
		DetectedLinkageFindings findings = detectLinkages(linkedGLString);

		return findings;
	}

	private static DetectedLinkageFindings detectLinkages(LinkageDisequilibriumGenotypeList linkedGLString) throws IOException {
		DetectedLinkageFindings findings = HLALinkageDisequilibrium.hasLinkageDisequilibrium(linkedGLString);
				
		return findings;
	}
}
