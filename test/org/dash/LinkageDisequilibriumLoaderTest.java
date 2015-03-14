package org.dash;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.dash.valid.LinkageDisequilibriumLoader;
import org.dash.valid.gl.GLStringUtilities;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;
import org.junit.Test;

public class LinkageDisequilibriumLoaderTest {
	@Test
	public void testLinkageReportingExamples() {
		LinkageDisequilibriumLoader.main(new String[] {"resources/test/stanfordExamples.txt"});
	}
	
	@Test
	public void testLinkageReportingMugs() {
		List<String> glStrings = GLStringUtilities.readGLStringFile("resources/test/fullyQualifiedExample.txt");
		String glString = glStrings.get(0);
		
		MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(glString);
		
		assertNotNull(mug);
		
		LinkageDisequilibriumLoader.detectLinkages(mug);
	}
	
	@Test
	public void testLinkageReportingInlineGLString() {
		String fullyQualified = GLStringUtilities.fullyQualifyGLString("HLA-A*02:01:01:01+HLA-A*24:02:01:01/24:02:01:03^HLA-B*27:02:01+HLA-B*35:02:01/35:211^HLA-C*02:02:02+HLA-C*04:01:01:01/04:01:01:02/04:01:01:03/04:01:01:04/04:01:01:05/04:20/04:117^HLA-DPA1*01:03:01:01/01:03:01:02/01:03:01:03/01:03:01:04/01:03:01:05+HLA-DPA1*02:01:01^HLA-DPB1*04:01:01:01/04:01:01:02+HLA-DPB1*17:01^HLA-DQA1*02:01+HLA-DQA1*05:05:01:01/05:05:01:02/05:05:01:03/05:09/05:11^HLA-DQB1*02:02+HLA-DQB1*03:01:01:01/03:01:01:02/03:01:01:03^HLA-DRB1*07:01:01:01/07:01:01:02+HLA-DRB1*11:01:01^HLA-DRB3*01:01:02:01/01:01:02:02^HLA-DRB4*01:03:01:01/01:03:01:03");
		MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(fullyQualified);
		LinkageDisequilibriumLoader.detectLinkages(mug);
	}
}
