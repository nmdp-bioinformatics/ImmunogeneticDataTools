package org.dash.valid.ars;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class AntigenRecognitionSiteLoaderTest {
	
	@Before
	public void setUp() {
		System.setProperty("org.dash.hladb", "3.18.0");
	}

	@Test
	public void test() {
		AntigenRecognitionSiteLoader arsLoader = AntigenRecognitionSiteLoader.getInstance();
		assertNotNull(arsLoader);
	}

}
