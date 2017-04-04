package org.nmdp.validation.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

public class AnalyzeGLStringsTest extends TestCase {

	public void testAnalyzeGLString() throws Exception {
		InputStream is = AnalyzeGLStringsTest.class.getClassLoader().getResourceAsStream("fullyQualifiedExample.txt");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		File inputFile = null;
		File outputFile = null;
		String hladb = null;
		String freq = null;
		Boolean warnings = null;
		File frequencyFile = null;
		File allelesFile = null;
		
		AnalyzeGLStrings analyzer = new AnalyzeGLStrings(inputFile, outputFile, hladb, freq, warnings, frequencyFile, allelesFile);
		analyzer.runAnalysis(reader);
		
		assertTrue(true);
	}

}
