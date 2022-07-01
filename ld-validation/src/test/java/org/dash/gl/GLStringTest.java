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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GLStringTest {	
	private static final String SIMPLE_DRB4_STRING = "HLA-DRB4*01:01:01:01";
	private static final String HOMOZYGOUS_C_STRING = "HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34";
	private static String STRICT_GL_STRING;

	private static LinkageDisequilibriumGenotypeList glString;
	
	@BeforeAll
	public static void setUp() throws IOException {
		List<LinkageDisequilibriumGenotypeList> validGLStrings = GLStringUtilities.readGLStringFile("fullyQualifiedExample.txt");
				
		glString = validGLStrings.get(0);
		
		List<LinkageDisequilibriumGenotypeList> strictGLStrings = GLStringUtilities.readGLStringFile("strictExample.txt");
		
		STRICT_GL_STRING = strictGLStrings.get(0).getGLString();
		
		assertNotNull(STRICT_GL_STRING);
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
}
