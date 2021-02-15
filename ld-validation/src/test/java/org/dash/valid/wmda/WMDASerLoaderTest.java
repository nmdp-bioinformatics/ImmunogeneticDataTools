package org.dash.valid.wmda;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class WMDASerLoaderTest {

	@Test
	public void testLoadWMDASerAlleles() {
		Map<String, List<String>> serMap = WMDASerLoader.getInstance().getSerMap();
		
		assertNotNull(serMap);
	}

}
