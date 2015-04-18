package org.dash.gl;

import java.util.LinkedHashMap;
import java.util.Set;

import junit.framework.TestCase;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.Before;
import org.junit.Test;

public class GLStringTest extends TestCase {	
	private static final String SIMPLE_DRB4_STRING = "DRB4*01:01:01:01";
	private LinkageDisequilibriumGenotypeList glString;
	
	@Before
	public void setUp() {
		LinkedHashMap<String, String> validGLStrings = GLStringUtilities.readGLStringFile("resources/test/fullyQualifiedExample.txt");
		
		Set<String> keys = validGLStrings.keySet();
		
		for (String key : keys) {
			glString = new LinkageDisequilibriumGenotypeList(key, GLStringUtilities.fullyQualifyGLString(validGLStrings.get(key)));
		}
	}
	
	@Test
	public void testToString() {
		assertNotNull(glString.toString());
	}
	
	@Test
	public void testDRB345AppearsHomozygous() {
		assertFalse(glString.drb345AppearsHomozygous());
		
		LinkageDisequilibriumGenotypeList simpleDRB4String = new LinkageDisequilibriumGenotypeList("HOMOZYGOUS-DRB4", SIMPLE_DRB4_STRING);
		assertTrue(simpleDRB4String.drb345AppearsHomozygous());
	}
}
