package org.dash.valid.ars;

import junit.framework.TestCase;

import org.junit.Test;

public class AntigenRecognitionSiteLoaderTest extends TestCase {
	@Test
	public void test() {
		AntigenRecognitionSiteLoader arsLoader = AntigenRecognitionSiteLoader.getInstance();
		assertNotNull(arsLoader);
		assertTrue(arsLoader.getbArsMap() != null && arsLoader.getbArsMap().size() > 0);
		assertTrue(arsLoader.getcArsMap() != null && arsLoader.getcArsMap().size() > 0);
		assertTrue(arsLoader.getDrb1ArsMap() != null && arsLoader.getDrb1ArsMap().size() > 0);
		assertTrue(arsLoader.getDrb3ArsMap() != null && arsLoader.getDrb3ArsMap().size() > 0);
		assertTrue(arsLoader.getDrb4ArsMap() != null && arsLoader.getDrb4ArsMap().size() > 0);
		assertTrue(arsLoader.getDrb5ArsMap() != null && arsLoader.getDrb5ArsMap().size() > 0);
		assertTrue(arsLoader.getDqb1ArsMap() != null && arsLoader.getDqb1ArsMap().size() > 0);
	}

}
