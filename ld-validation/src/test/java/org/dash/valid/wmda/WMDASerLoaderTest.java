package org.dash.valid.wmda;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class WMDASerLoaderTest {

	@Test
	public void testLoadWMDASerAlleles() {
		Map<String, List<String>> serMap = WMDASerLoader.getInstance().getSerMap();
		
		assertNotNull(serMap);
	}

}
