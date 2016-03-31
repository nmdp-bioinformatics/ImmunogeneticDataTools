package org.dash.valid.cwd;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

public class CommonWellDocumentedLoaderTest extends TestCase {	
	private static final String DQA10111 = "HLA-DQA1*01:11";
	private static final String HLA08433 = "HLA08433";
	@Test
	public void test() {
		CommonWellDocumentedLoader cwdLoader = CommonWellDocumentedLoader.getInstance();
		assertNotNull(cwdLoader);
		assertTrue(cwdLoader.getCwdAlleles() != null && cwdLoader.getCwdAlleles().size() > 0);
	}

	@Test
	public void testLoadAllCWD() throws FileNotFoundException, IOException {
		CommonWellDocumentedLoader cwdLoader = CommonWellDocumentedLoader.getInstance();
		List<String> hladbs;
		
		assertTrue(cwdLoader.getCwdByAccession().containsValue(DQA10111));
		for (String key : cwdLoader.getCwdByAccession().keySet()) {
			if (cwdLoader.getCwdByAccession().get(key).equals(DQA10111)) {
				assertTrue(key.equals(HLA08433));
				hladbs = cwdLoader.getHlaDbByAccession().get(key);
				assertNotNull(hladbs);
				break;
			}
		}
	}
}
