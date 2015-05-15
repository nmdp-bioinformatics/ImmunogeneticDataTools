package org.dash.gl;

import java.util.LinkedHashMap;
import java.util.Set;

import junit.framework.TestCase;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.Before;
import org.junit.Test;

public class GLStringTest extends TestCase {	
	private static final String SIMPLE_DRB4_STRING = "HLA-DRB4*01:01:01:01";
	private static final String HOMOZYGOUS_C_STRING = "HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34";
	private LinkageDisequilibriumGenotypeList glString;
	
	@Before
	public void setUp() {
		LinkedHashMap<String, String> validGLStrings = GLStringUtilities.readGLStringFile("fullyQualifiedExample.txt");
		
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
		assertFalse(glString.checkHomozygous(Locus.HLA_DRB4));
		
		LinkageDisequilibriumGenotypeList simpleDRB4String = new LinkageDisequilibriumGenotypeList("HOMOZYGOUS-DRB4", SIMPLE_DRB4_STRING);
		assertTrue(simpleDRB4String.checkHomozygous(Locus.HLA_DRB4));
		
		LinkageDisequilibriumGenotypeList homozygousCString = new LinkageDisequilibriumGenotypeList("HOMOZYGOUS-C", HOMOZYGOUS_C_STRING);
		assertTrue(homozygousCString.checkHomozygous(Locus.HLA_C));
	}
}
