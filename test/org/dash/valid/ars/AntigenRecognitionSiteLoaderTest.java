package org.dash.valid.ars;

import static org.junit.Assert.*;

import org.junit.Test;

public class AntigenRecognitionSiteLoaderTest {

	@Test
	public void test() {
		AntigenRecognitionSiteLoader arsLoader = AntigenRecognitionSiteLoader.getInstance();
		assertNotNull(arsLoader);
	}

}
