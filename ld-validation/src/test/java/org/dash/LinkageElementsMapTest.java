package org.dash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.LinkageElementsSet;
import org.dash.valid.Locus;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.report.DetectedDisequilibriumElement;
import org.junit.Test;

public class LinkageElementsMapTest extends TestCase {

	@Test
	public void test() {
		List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
		FrequencyByRace freq = new FrequencyByRace(new Double(.2), "1","AAFA");
		frequenciesByRace.add(freq);
		
		HashMap<Locus, String> hlaElementMap = new HashMap<Locus, String>();
		hlaElementMap.put(Locus.HLA_B, "HLA-B*07:01");
		hlaElementMap.put(Locus.HLA_C, "HLA-C*01:01");
		
		
		DisequilibriumElementByRace element1 = new DisequilibriumElementByRace(hlaElementMap, frequenciesByRace);
		
		frequenciesByRace = new ArrayList<FrequencyByRace>();
		
		freq = new FrequencyByRace(new Double(.3), "2", "CAU");
		frequenciesByRace.add(freq);
		freq = new FrequencyByRace(new Double(.4), "3", "API");
		frequenciesByRace.add(freq);
		
		hlaElementMap = new HashMap<Locus, String>();
		hlaElementMap.put(Locus.HLA_B, "HLA-B*52:01");
		hlaElementMap.put(Locus.HLA_C, "HLA-C*04:01");
		
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
