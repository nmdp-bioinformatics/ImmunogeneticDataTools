package org.dash.valid;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;

public class LinkageDisequilibriumChecker {	
	
    private static final Logger LOGGER = Logger.getLogger(LinkageDisequilibriumChecker.class.getName());
    
    static {
    	try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
    	}
    	catch (IOException ioe) {
    		LOGGER.severe("Could not add file handler to logger");
    	}
    }
    
	public static void main(String[] args) {
		analyzeGLStringFiles(args);
		
		LinkageDisequilibriumWriter.getHandler().close();
	}
	
	public static void analyzeGLStringFiles(String[] filenames) {		
		for (int i=0;i<filenames.length;i++) {
			analyzeGLStringFile(filenames[i]);		
		}
	}

	/**
	 * @param filename
	 */
	private static void analyzeGLStringFile(String filename) {				
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile(filename);
		
		for (String key : glStrings.keySet()) {
			if (!GLStringUtilities.validateGLStringFormat(glStrings.get(key))) {
				glStrings.put(key, GLStringUtilities.fullyQualifyGLString(glStrings.get(key)));
			}
		}
		
		try {
			detectLinkages(glStrings);
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
	private static void detectLinkages(Map<String, String> glStrings) throws SecurityException, IOException {
		LinkageDisequilibriumGenotypeList linkedGLString;
		Map<Object, Boolean> linkagesFound;
		
		for (String key : glStrings.keySet()) {
			linkedGLString = new LinkageDisequilibriumGenotypeList(key, glStrings.get(key));
			linkagesFound = detectLinkages(linkedGLString);
			LinkageDisequilibriumWriter writer = LinkageDisequilibriumWriter.getInstance();
			writer.reportDetectedLinkages(linkedGLString, linkagesFound);
		}
	}
	
	public static void detectLinkages(MultilocusUnphasedGenotype mug) {
		LinkageDisequilibriumGenotypeList linkedGLString = new LinkageDisequilibriumGenotypeList(mug);
		Map<Object, Boolean> linkagesFound = detectLinkages(linkedGLString);
		LinkageDisequilibriumWriter writer = LinkageDisequilibriumWriter.getInstance();
		
		try {
			writer.reportDetectedLinkages(linkedGLString, linkagesFound);
		} catch (IOException e) {
			LOGGER.warning("Could not use file handler:  LinkageDisequiibrimFileHandler");
		}	
	}

	private static Map<Object, Boolean> detectLinkages(LinkageDisequilibriumGenotypeList linkedGLString) {
		Map<Object, Boolean> linkagesFound = HLALinkageDisequilibrium.hasDisequilibriumLinkage(linkedGLString);
				
		return linkagesFound;
	}
}
