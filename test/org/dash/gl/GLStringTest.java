package org.dash.gl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.dash.valid.gl.GLString;
import org.dash.valid.gl.GLStringUtilities;
import org.junit.Before;
import org.junit.Test;

public class GLStringTest {	
	private static final String TEST_GL_STRING = "A*01:01:01:01+26:01:01^B*38:01:01/38:27+44:03:01/44:03:10/44:125^C*04:01:01:01/04:01:01:02/04:01:01:03/04:01:01:04/04:01:01:05/04:20/04:117+12:03:01:01/12:03:01:02/12:34^DPA1*01:03:01:01/01:03:01:02/01:03:01:03/01:03:01:04/01:03:01:05+01:03:01:01/01:03:01:02/01:03:01:03/01:03:01:04/01:03:01:05^DPB1*04:01:01:01/04:01:01:02+04:01:01:01/04:01:01:02^DQA1*02:01+05:05:01:01/05:05:01:02/05:05:01:03/05:09/05:11^DQB1*02:02+03:01:01:01/03:01:01:02/03:01:01:03^DRB1*07:01:01:01/07:01:01:02+11:01:01^DRB3*02:02:01:01/02:02:01:02^DRB4*01:01:01:01/03:01N";
	private static final String SIMPLE_DRB4_STRING = "DRB4*01:01:01:01";
	private GLString glString;
	
	@Before
	public void setUp() {
		glString = new GLString(GLStringUtilities.fullyQualifyGLString(TEST_GL_STRING));
	}
	
	@Test
	public void testToString() {
		assertNotNull(glString.toString());
	}
	
	@Test
	public void testDRB345AppearsHomozygous() {
		assertFalse(glString.drb345AppearsHomozygous());
		
		GLString simpleDRB4String = new GLString(SIMPLE_DRB4_STRING);
		assertTrue(simpleDRB4String.drb345AppearsHomozygous());
	}
}
