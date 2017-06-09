package org.nmdp.validation.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import org.dash.valid.Sample;

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
		Set<File> frequencyFiles = null;
		File allelesFile = null;
		
		AnalyzeGLStrings analyzer = new AnalyzeGLStrings(inputFile, outputFile, hladb, freq, warnings, frequencyFiles, allelesFile);
		List<Sample> samplesList = analyzer.performAnalysis(reader);
		
		assertNotNull(samplesList);
		assertTrue(samplesList.size() > 0);
	}

}
