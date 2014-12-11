package org.dash.valid;

import java.util.List;
import java.util.Set;

import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.GLStringUtilities;

public class LinkageDisequilibriumLoader {	
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
	private static void detectLinkages(List<String> glStrings) {
		LinkageDisequilibriumGenotypeList linkedGLString;
		Set<DisequilibriumElement> linkagesFound;
		
		for (String glString : glStrings) {
			linkedGLString = new LinkageDisequilibriumGenotypeList(glString);
			linkagesFound = detectLinkages(linkedGLString);
			reportDetectedLinkages(linkedGLString, linkagesFound);
		}
	}

	public static Set<DisequilibriumElement> detectLinkages(LinkageDisequilibriumGenotypeList linkedGLString) {
		HLALinkageDisequilibrium linkDis = new HLALinkageDisequilibrium();

		Set<DisequilibriumElement> linkagesFound = linkDis.hasDisequilibriumLinkage(linkedGLString);
				
		return linkagesFound;
	}

	/**
	 * @param linkagesFound
	 */
	public static void reportDetectedLinkages(LinkageDisequilibriumGenotypeList linkedGLString, 
			Set<DisequilibriumElement> linkagesFound) {
		System.out.println("Your GL String: " + linkedGLString.getGLString());

		for (DisequilibriumElement linkages : linkagesFound) {
			System.out.println("\n");
			System.out.println("We found linkages:\n");
			System.out.println(linkages);
		}
		
		System.out.println("\n***************************************\n");
	}
}
