package org.dash;

import org.dash.valid.LinkageDisequilibriumLoader;
import org.junit.Test;

public class LinkageDisequilibriumLoaderTest {

	@Test
	public void testLinkageReportingExamples() {
		LinkageDisequilibriumLoader.main(new String[] {"resources/test/fullyQualifiedExamples.txt",
				"resources/test/shorthandExamples.txt",
				"resources/test/contrivedExamples.txt"});
	}
}
