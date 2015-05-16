package org.dash.valid.race;

import java.util.Comparator;


public class RelativeFrequencyByRaceComparator implements Comparator<RelativeFrequencyByRace> {

	@Override
	public int compare(RelativeFrequencyByRace o1, RelativeFrequencyByRace o2) {
		if (BroadRace.contains(o1.getRace()) && !BroadRace.contains(o2.getRace())) {
			return -1;
		}
		else if (BroadRace.contains(o2.getRace()) && !BroadRace.contains(o1.getRace())) {
			return 1;
		}
		
		if (o2.getRace().equals(o1.getRace()) && o2.getRelativeFrequency() != null && o1.getRelativeFrequency() != null) {
			return o2.getRelativeFrequency().compareTo(o1.getRelativeFrequency());
		}
		
		return o1.toString().compareTo(o2.toString());
	}
}
