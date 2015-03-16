package org.dash.valid;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;

public class LinkageDisequilibriumLoader {	
	
    private static final Logger LOGGER = Logger.getLogger(LinkageDisequilibriumLoader.class.getName());
    
    static {
    	try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
    	}
    	catch (IOException ioe) {
    		LOGGER.severe("Could not add file handler to logger");
    	}
    }
    
	public static void main(String[] args) {
		String filename = null;
		
		for (int i=0;i<args.length;i++) {
			filename = args[i];
			analyzeGLStringFile(filename);		
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
		
		detectLinkages(glStrings);
	}

	/**
	 * @param glStrings
	 */
	public static void detectLinkages(LinkedHashMap<String, String> glStrings) {
		LinkageDisequilibriumGenotypeList linkedGLString;
		HashMap<DisequilibriumElement, Boolean> linkagesFound;
		
		for (String key : glStrings.keySet()) {
			linkedGLString = new LinkageDisequilibriumGenotypeList(key, glStrings.get(key));
			linkagesFound = detectLinkages(linkedGLString);
			LinkageDisequilibriumWriter.reportDetectedLinkages(linkedGLString, linkagesFound);
		}
	}
	
	public static void detectLinkages(MultilocusUnphasedGenotype mug) {
		LinkageDisequilibriumGenotypeList linkedGLString = new LinkageDisequilibriumGenotypeList(mug);
		HashMap<DisequilibriumElement, Boolean> linkagesFound = LinkageDisequilibriumLoader.detectLinkages(linkedGLString);
		LinkageDisequilibriumWriter.reportDetectedLinkages(linkedGLString, linkagesFound);
	}

	private static HashMap<DisequilibriumElement, Boolean> detectLinkages(LinkageDisequilibriumGenotypeList linkedGLString) {
		HLALinkageDisequilibrium linkDis = new HLALinkageDisequilibrium();

		HashMap<DisequilibriumElement, Boolean> linkagesFound = linkDis.hasDisequilibriumLinkage(linkedGLString);
				
		return linkagesFound;
	}
}
