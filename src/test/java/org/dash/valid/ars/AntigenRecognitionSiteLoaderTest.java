package org.dash.valid.ars;

import junit.framework.TestCase;

import org.junit.Test;

public class AntigenRecognitionSiteLoaderTest extends TestCase {
	@Test
	public void test() {
		AntigenRecognitionSiteLoader arsLoader = AntigenRecognitionSiteLoader.getInstance();
		assertNotNull(arsLoader);
		assertTrue(arsLoader.getArsMap() != null && arsLoader.getArsMap().size() > 0);
	}

}
