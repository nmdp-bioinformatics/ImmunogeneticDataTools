package org.dash.gl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dash.valid.gl.GLString;
import org.dash.valid.gl.GLStringUtilities;
import org.junit.Before;
import org.junit.Test;

public class GLStringTest {	
	private static String VALID_GL_STRING;
	private static final String SIMPLE_DRB4_STRING = "DRB4*01:01:01:01";
	private GLString glString;
	
	@Before
	public void setUp() {
		List<String> validGLStrings = GLStringUtilities.readGLStringFile("resources/test/fullyQualifiedExample.txt");
		VALID_GL_STRING = validGLStrings.get(0);
		glString = new GLString(GLStringUtilities.fullyQualifyGLString(VALID_GL_STRING));
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
