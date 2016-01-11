package org.dash.valid.ars;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;

public class AntigenRecognitionSiteLoaderTest extends TestCase {
	@Test
	public void test() throws InvalidFormatException, IOException {
		AntigenRecognitionSiteLoader arsLoader = AntigenRecognitionSiteLoader.getInstance();
		assertNotNull(arsLoader);
		assertTrue(arsLoader.getArsMap() != null && arsLoader.getArsMap().size() > 0);
	}
}
