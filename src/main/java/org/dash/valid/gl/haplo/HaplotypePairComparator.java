package org.dash.valid.gl.haplo;

import java.util.Comparator;

import org.dash.valid.race.RelativeFrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRaceComparator;

public class HaplotypePairComparator implements Comparator<HaplotypePair> {

	@Override
	public int compare(HaplotypePair element1, HaplotypePair element2) {
		int ret = 0;
		if (element1.equals(element2)) {
			return 0;
		}
		else if (element1.getPrimaryFrequency() != null && element1.getPrimaryFrequency() instanceof RelativeFrequencyByRace &&
				element2.getPrimaryFrequency() != null) {
			ret =  new RelativeFrequencyByRaceComparator().compare((RelativeFrequencyByRace) element1.getPrimaryFrequency(), 
					(RelativeFrequencyByRace) element2.getPrimaryFrequency());
		}
		
		if (ret != 0) {
			return ret;
		}
		
		// else sort alphabetically
		ret = element1.getHaplotype1().toString().compareTo(element2.getHaplotype1().toString());
		return ret;
	}
}
