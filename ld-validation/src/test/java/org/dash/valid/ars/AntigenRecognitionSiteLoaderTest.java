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
package org.dash.valid.ars;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class AntigenRecognitionSiteLoaderTest extends TestCase {
	@Test
	public void test() throws InvalidFormatException, IOException, ParserConfigurationException, SAXException {
		AntigenRecognitionSiteLoader arsLoader = AntigenRecognitionSiteLoader.getInstance();
		assertNotNull(arsLoader);
		assertTrue(arsLoader.getArsMap() != null && arsLoader.getArsMap().size() > 0);
	}
	
//	public void testRemoteArs() throws InvalidFormatException, IOException, ParserConfigurationException, SAXException {
//		HashMap<String, HashSet<String>> gAllelesMap = AntigenRecognitionSiteLoader.loadGGroups("3.20.0");
//		
//		assertNotNull(gAllelesMap);
//		assertTrue(gAllelesMap.keySet().size() > 0);
//	}
}
