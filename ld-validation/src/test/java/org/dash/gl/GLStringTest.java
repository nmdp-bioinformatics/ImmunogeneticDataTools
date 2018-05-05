/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.dash.gl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.Before;
import org.junit.Test;
import org.nmdp.gl.client.http.HttpClient;
import org.nmdp.gl.client.http.restassured.RestAssuredHttpClient;

import junit.framework.TestCase;

public class GLStringTest extends TestCase {	
	private static final String SIMPLE_DRB4_STRING = "HLA-DRB4*01:01:01:01";
	private static final String HOMOZYGOUS_C_STRING = "HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34";
	private static String STRICT_GL_STRING;

	private LinkageDisequilibriumGenotypeList glString;
	
	@Before
	public void setUp() throws IOException {
		List<LinkageDisequilibriumGenotypeList> validGLStrings = GLStringUtilities.readGLStringFile("fullyQualifiedExample.txt");
				
		glString = validGLStrings.get(0);
		
		List<LinkageDisequilibriumGenotypeList> strictGLStrings = GLStringUtilities.readGLStringFile("strictExample.txt");
		
		STRICT_GL_STRING = strictGLStrings.get(0).getGLString();
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
//		String id = glClient.post("https://gl.nmdp.org/nonstrict/multilocus-unphased-genotype", STRICT_GL_STRING);
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
