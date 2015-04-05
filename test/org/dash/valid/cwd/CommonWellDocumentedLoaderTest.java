package org.dash.valid.cwd;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommonWellDocumentedLoaderTest {		
	@Test
	public void test() {
		CommonWellDocumentedLoader cwdLoader = CommonWellDocumentedLoader.getInstance();
		assertNotNull(cwdLoader);
		assertTrue(cwdLoader.getCwdAlleles() != null && cwdLoader.getCwdAlleles().size() > 0);
	}

}
