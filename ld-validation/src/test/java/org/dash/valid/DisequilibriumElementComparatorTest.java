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
package org.dash.valid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.Locus;
import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.report.DetectedDisequilibriumElement;
import org.junit.Test;

public class DisequilibriumElementComparatorTest extends TestCase {

	@Test
	public void testComparisonByRace() {
		List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
		FrequencyByRace freq = new FrequencyByRace(new Double(.2), "1","AAFA");
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
		
		freq = new FrequencyByRace(new Double(.3), "2", "CAU");
		frequenciesByRace.add(freq);
		freq = new FrequencyByRace(new Double(.4), "3", "API");
		frequenciesByRace.add(freq);
		
		hlaElementMap = new HashMap<Locus, List<String>>();
		val = new ArrayList<String>();
		val.add("HLA-B*52:01");
		hlaElementMap.put(Locus.HLA_B,  val);
		val = new ArrayList<String>();
		val.add("HLA-C*04:01");
		hlaElementMap.put(Locus.HLA_C, val);
		
		DisequilibriumElementByRace element2 = new DisequilibriumElementByRace(hlaElementMap, frequenciesByRace);
		
		DisequilibriumElementComparator comparator = new DisequilibriumElementComparator();
		assertTrue(comparator.compare(new DetectedDisequilibriumElement(element1), new DetectedDisequilibriumElement(element2)) > 0);
	}

	@Test
	public void testBaseComparison() {
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		List<String> val = new ArrayList<String>();
		val.add("HLA-B*52:01");
		hlaElementMap.put(Locus.HLA_B, val);
		val = new ArrayList<String>();
		val.add("HLA-C*04:01");
		hlaElementMap.put(Locus.HLA_C,  val);
		
		BaseDisequilibriumElement element1 = new BaseDisequilibriumElement(hlaElementMap, "1", "Some note");
		
		hlaElementMap = new HashMap<Locus, List<String>>();
		val = new ArrayList<String>();
		val.add("HLA-B*07:01");
		hlaElementMap.put(Locus.HLA_B, val);
		val = new ArrayList<String>();
		val.add("HLA-C*01:01");
		hlaElementMap.put(Locus.HLA_C, val);

		BaseDisequilibriumElement element2 = new BaseDisequilibriumElement(hlaElementMap, "2", "Another note");
				
		DisequilibriumElementComparator comparator = new DisequilibriumElementComparator();
		assertTrue(comparator.compare(new DetectedDisequilibriumElement(element1), new DetectedDisequilibriumElement(element2)) > 0);
	}
}
