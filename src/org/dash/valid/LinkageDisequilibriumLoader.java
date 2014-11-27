package org.dash.valid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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
		File glStringFile  = new File(filename);
		BufferedReader reader = null;
		String glString;
		GLString linkedGLString;
		List<DisequilibriumElement> linkagesFound;
		
		try {
			InputStream in = new FileInputStream(glStringFile);
			reader = new BufferedReader(new InputStreamReader(in));
			
			while ((glString = reader.readLine()) != null) {
				linkedGLString = new GLString(glString);
				linkagesFound = detectLinkages(linkedGLString);
				reportDetectedLinkages(linkedGLString, linkagesFound);
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
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
