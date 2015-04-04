package org.dash.valid.cwd;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class CommonWellDocumentedLoaderTest {	
	@Before
	public void setUp() {
		System.setProperty("org.dash.hladb", "3.18.0");
	}
	
	@Test
	public void test() {
		assertNotNull(CommonWellDocumentedLoader.getInstance().getCwdAlleles());
	}

}
