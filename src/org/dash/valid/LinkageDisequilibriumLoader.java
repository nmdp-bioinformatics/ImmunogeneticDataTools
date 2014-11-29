package org.dash.valid;

import java.util.List;

import org.dash.valid.gl.GLString;
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
				
		detectLinkages(glStrings);
	}

	/**
	 * @param glStrings
	 */
	private static void detectLinkages(List<String> glStrings) {
		GLString linkedGLString;
		List<DisequilibriumElement> linkagesFound;
		
		for (String glString : glStrings) {
			linkedGLString = new GLString(glString);
			linkagesFound = detectLinkages(linkedGLString);
			reportDetectedLinkages(linkedGLString, linkagesFound);
		}
	}

	private static List<DisequilibriumElement> detectLinkages(GLString linkedGLString) {
		HLALinkageDisequilibrium linkDis = new HLALinkageDisequilibrium();

		List<DisequilibriumElement> linkagesFound = linkDis.hasBCDisequilibriumLinkage(linkedGLString);
		linkagesFound.addAll(linkDis.hasDRDQDisequilibriumLinkage(linkedGLString));
				
		return linkagesFound;
	}

	/**
	 * @param linkagesFound
	 */
	private static void reportDetectedLinkages(GLString linkedGLString, 
			List<DisequilibriumElement> linkagesFound) {
		System.out.println("Your GL String: " + linkedGLString.getGLString());

		for (DisequilibriumElement linkages : linkagesFound) {
			System.out.println("\n");
			System.out.println("We found linkages:\n");
			System.out.println(linkages);
		}
		
		System.out.println("\n***************************************\n");
	}
}
