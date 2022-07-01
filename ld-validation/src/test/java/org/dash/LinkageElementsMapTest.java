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
package org.dash;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.LinkageElementsSet;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.report.DetectedDisequilibriumElement;
import org.junit.jupiter.api.Test;

public class LinkageElementsMapTest {

	@Test
	public void test() {
		System.setProperty(GLStringConstants.HLADB_PROPERTY, "3.20.0");
		List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
		FrequencyByRace freq = new FrequencyByRace(.2, "1","AAFA");
		frequenciesByRace.add(freq);
		
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		List<String> val = new ArrayList<String>();
		val.add("HLA-B*07:01");
		hlaElementMap.put(Locus.HLA_B, val);
		val = new ArrayList<String>();
		val.add("HLA-C*01:01");
		hlaElementMap.put(Locus.HLA_C, val);
		
		
		DisequilibriumElementByRace element1 = new DisequilibriumElementByRace(hlaElementMap, frequenciesByRace);
		
		frequenciesByRace = new ArrayList<FrequencyByRace>();
		
		freq = new FrequencyByRace(.3, "2", "CAU");
		frequenciesByRace.add(freq);
		freq = new FrequencyByRace(.4, "3", "API");
		frequenciesByRace.add(freq);
		
		hlaElementMap = new HashMap<Locus, List<String>>();
		val = new ArrayList<String>();
		val.add("HLA-B*52:01");
		hlaElementMap.put(Locus.HLA_B, val);
		val = new ArrayList<String>();
		val.add("HLA-C*04:01");
		hlaElementMap.put(Locus.HLA_C, val);
		
		DisequilibriumElementByRace element2 = new DisequilibriumElementByRace(hlaElementMap, frequenciesByRace);
		
		Set<DetectedDisequilibriumElement> set = new LinkageElementsSet(new DisequilibriumElementComparator());
		
		set.add(new DetectedDisequilibriumElement(element1));
		set.add(new DetectedDisequilibriumElement(element2));
				
		int idx = 0;
		for (DetectedDisequilibriumElement obj : set) {
			if (idx == 0) {
				assertTrue(obj.getDisequilibriumElement().equals(element2));
			}
			else {
				assertTrue(obj.getDisequilibriumElement().equals(element1));
			}
			
			idx++;
		}
	}

}
