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
}
