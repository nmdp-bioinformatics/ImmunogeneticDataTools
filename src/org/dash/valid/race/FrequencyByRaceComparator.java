package org.dash.valid.race;

import java.util.Comparator;


public class FrequencyByRaceComparator implements Comparator<FrequencyByRace> {

	@Override
	public int compare(FrequencyByRace o1, FrequencyByRace o2) {
		if (BroadRace.contains(o1.getRace()) && !BroadRace.contains(o2.getRace())) {
			return -1;
		}
		else if (BroadRace.contains(o2.getRace()) && !BroadRace.contains(o1.getRace())) {
			return 1;
		}
		
		return o1.getRace().compareTo(o2.getRace());
	}
}
