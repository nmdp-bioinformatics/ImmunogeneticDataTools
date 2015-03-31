package org.dash;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.LinkageElementsMap;
import org.dash.valid.race.BCDisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.junit.Test;

public class LinkageElementsMapTest {

	@Test
	public void test() {
		List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
		FrequencyByRace freq = new FrequencyByRace(new Double(.2), "1","AAFA");
		frequenciesByRace.add(freq);
		
		BCDisequilibriumElementByRace element1 = new BCDisequilibriumElementByRace("HLA-C*01:01", "HLA-B*07:01", frequenciesByRace);
		
		frequenciesByRace = new ArrayList<FrequencyByRace>();
		
		freq = new FrequencyByRace(new Double(.3), "2", "CAU");
		frequenciesByRace.add(freq);
		freq = new FrequencyByRace(new Double(.4), "3", "API");
		frequenciesByRace.add(freq);
		
		BCDisequilibriumElementByRace element2 = new BCDisequilibriumElementByRace("HLA-C*04:01", "HLA-B*52:01", frequenciesByRace);
		
		Map<Object, Boolean> map = new LinkageElementsMap(new DisequilibriumElementComparator());
		
		map.put(element1,  Boolean.TRUE);
		map.put(element2, Boolean.TRUE);
		
		Set<Object> set = map.keySet();
		
		int idx = 0;
		for (Object obj : set) {
			if (idx == 0) {
				assertTrue(obj.equals(element2));
			}
			else {
				assertTrue(obj.equals(element1));
			}
			
			idx++;
		}
	}

}
