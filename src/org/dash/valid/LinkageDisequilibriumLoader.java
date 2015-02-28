package org.dash.valid;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
		List<String> glStrings = GLStringUtilities.readGLStringFile(filename);
		String glString;
		
		for (int i=0;i<glStrings.size();i++) {
			glString = glStrings.get(i);
			if (!GLStringUtilities.validateGLStringFormat(glString)) {
				glStrings.set(i, GLStringUtilities.fullyQualifyGLString(glString));
			}
		}
		
		detectLinkages(glStrings);
	}

	/**
	 * @param glStrings
	 */
	public static void detectLinkages(List<String> glStrings) {
		LinkageDisequilibriumGenotypeList linkedGLString;
		HashMap<DisequilibriumElement, Boolean> linkagesFound;
		
		for (String glString : glStrings) {
			linkedGLString = new LinkageDisequilibriumGenotypeList(glString);
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
