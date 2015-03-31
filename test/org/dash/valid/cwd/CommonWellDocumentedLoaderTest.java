package org.dash.valid.cwd;

import static org.junit.Assert.*;

import org.junit.Test;

public class CommonWellDocumentedLoaderTest {

	@Test
	public void test() {
		assertNotNull(CommonWellDocumentedLoader.getInstance().getCwdAlleles());
	}

}
