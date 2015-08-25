package org.dash;

import java.io.IOException;
import java.util.LinkedHashMap;

import junit.framework.TestCase;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.report.DetectedLinkageFindings;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;
import org.junit.Test;

public class LinkageDisequilibriumAnalyzerTest extends TestCase {	
	@Test
	public void testLinkageReportingExamples() {
		LinkageDisequilibriumAnalyzer.main(new String[] {"contrivedExamples.txt", "fullyQualifiedExample.txt", "strictExample.txt", "shorthandExamples.txt"});
	}
	
	@Test
	public void testLinkageReportingMugs() {
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile("fullyQualifiedExample.txt");
		
		for (String key : glStrings.keySet()) {
			MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(glStrings.get(key));
			
			assertNotNull(mug);
			
			DetectedLinkageFindings findings = LinkageDisequilibriumAnalyzer.detectLinkages(mug);
			
			assertNotNull(findings);
		}
	}
	
	@Test
	public void testLinkageReportingInlineGLString() throws IOException {
		String fullyQualified = GLStringUtilities.fullyQualifyGLString("HLA-A*03:01:01:01/HLA-A*03:01:01:02N/HLA-A*03:01:01:03+HLA-A*33:01:01^HLA-B*07:02:01/HLA-B*07:02:16/HLA-B*07:23/HLA-B*07:82/HLA-B*07:93/HLA-B*07:102/HLA-B*07:111N/HLA-B*07:171+HLA-B*14:02:01^HLA-C*07:02:01:01/HLA-C*07:02:01:02/HLA-C*07:02:01:03/HLA-C*07:02:01:04/HLA-C*07:02:01:05/HLA-C*07:195+HLA-C*08:02:01/HLA-C*08:34^HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05+HLA-DPA1*01:04^HLA-DPB1*03:01:01/HLA-DPB1*104:01+HLA-DPB1*15:01^HLA-DQA1*01:01:01/HLA-DQA1*01:01:02/HLA-DQA1*01:04:01:01/HLA-DQA1*01:04:01:02/HLA-DQA1*01:05+HLA-DQA1*05:05:01:01/HLA-DQA1*05:05:01:02/HLA-DQA1*05:05:01:03/HLA-DQA1*05:09/HLA-DQA1*05:11^HLA-DQB1*03:01:01:01/HLA-DQB1*03:01:01:02/HLA-DQB1*03:01:01:03+HLA-DQB1*05:01:01:01/HLA-DQB1*05:01:01:02^HLA-DRB1*01:02:01+HLA-DRB1*11:04:01^HLA-DRB3*02:02:01:01/HLA-DRB3*02:02:01:02");
		MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(fullyQualified);
		DetectedLinkageFindings findings = LinkageDisequilibriumAnalyzer.detectLinkages(mug);
		
		assertNotNull(findings);		
	}
}
