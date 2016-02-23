package org.dash.gl;

import java.io.IOException;
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
	private static String STRICT_GL_STRING;

	private LinkageDisequilibriumGenotypeList glString;
	
	@Before
	public void setUp() throws IOException {
		LinkedHashMap<String, String> validGLStrings = GLStringUtilities.readGLStringFile("fullyQualifiedExample.txt");
		
		Set<String> keys = validGLStrings.keySet();
		
		for (String key : keys) {
			glString = new LinkageDisequilibriumGenotypeList(key, GLStringUtilities.fullyQualifyGLString(validGLStrings.get(key)));
		}
		
		LinkedHashMap<String, String> strictGLStrings = GLStringUtilities.readGLStringFile("strictExample.txt");
		
		keys = strictGLStrings.keySet();
		String key = keys.iterator().next();
		
		STRICT_GL_STRING = strictGLStrings.get(key);
	}
	
	@Test
	public void testToString() {
		assertNotNull(glString.toString());
	}
	
	@Test
	public void testDRB345AppearsHomozygous() throws IOException {
		assertFalse(glString.hasHomozygous(Locus.HLA_DRB4));
		
		LinkageDisequilibriumGenotypeList simpleDRB4String = new LinkageDisequilibriumGenotypeList("HOMOZYGOUS-DRB4", SIMPLE_DRB4_STRING);
		assertTrue(simpleDRB4String.hasHomozygous(Locus.HLA_DRB4));
		
		LinkageDisequilibriumGenotypeList homozygousCString = new LinkageDisequilibriumGenotypeList("HOMOZYGOUS-C", HOMOZYGOUS_C_STRING);
		assertTrue(homozygousCString.hasHomozygous(Locus.HLA_C));
	}
	
//	@Test
//	public void testHttpClient() {
//		BufferedReader reader = null;
//		String glString = null;
//		
//		HttpClient glClient = new RestAssuredHttpClient();
//		String id = glClient.post("https://gl.nmdp.org/imgt-hla/3.18.0/multilocus-unphased-genotype", STRICT_GL_STRING);
//		
//		InputStream in = glClient.get(id);
//		
//		try {
//			reader = new BufferedReader(new InputStreamReader(in));
//			
//			while ((glString = reader.readLine()) != null) {
//				assertTrue(STRICT_GL_STRING.equals(glString));
//			}
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
//		finally {
//			try {
//				reader.close();
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
}
