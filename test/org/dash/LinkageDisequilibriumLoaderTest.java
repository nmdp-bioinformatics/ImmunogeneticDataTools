package org.dash;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Set;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.LinkageDisequilibriumLoader;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;
import org.junit.Test;

public class LinkageDisequilibriumLoaderTest {

	@Test
	public void testLinkageReportingExamples() {
		LinkageDisequilibriumLoader.main(new String[] {"resources/test/fullyQualifiedExample.txt",
				"resources/test/shorthandExamples.txt",
				"resources/test/contrivedExamples.txt"});
	}
	
	@Test
	public void testLinkageReportingMugs() {
		List<String> glStrings = GLStringUtilities.readGLStringFile("resources/test/fullyQualifiedExample.txt");
		String glString = glStrings.get(0);
		
		MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(glString);
		
		assertNotNull(mug);
		
		LinkageDisequilibriumGenotypeList linkedGLString = new LinkageDisequilibriumGenotypeList(mug);
		Set<DisequilibriumElement> linkagesFound = LinkageDisequilibriumLoader.detectLinkages(linkedGLString);
		LinkageDisequilibriumLoader.reportDetectedLinkages(linkedGLString, linkagesFound);
	}
}
