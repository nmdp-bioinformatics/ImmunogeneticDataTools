package org.dash.valid;

import java.util.Comparator;

import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.report.DetectedDisequilibriumElement;

public class DisequilibriumElementComparator implements Comparator<DetectedDisequilibriumElement> {

	@Override
	public int compare(DetectedDisequilibriumElement element1, DetectedDisequilibriumElement element2) {
		int ret;
		
		if (element1.equals(element2)) {
			return 0;
		}
		else if (element1.getDisequilibriumElement() instanceof DisequilibriumElementByRace && element2.getDisequilibriumElement() instanceof DisequilibriumElementByRace) {
			// those with more linkages should sort first
			ret = ((DisequilibriumElementByRace) element2.getDisequilibriumElement()).getFrequenciesByRace().size() - 
					((DisequilibriumElementByRace) element1.getDisequilibriumElement()).getFrequenciesByRace().size();
			if (ret != 0) {
				return ret;
			}
		}
		
		// else sort alphabetically
		ret = element1.toString().compareTo(element2.toString());	
		return ret;
	}
}
