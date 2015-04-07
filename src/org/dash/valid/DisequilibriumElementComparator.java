package org.dash.valid;

import java.util.Comparator;

import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.report.DetectedDisequilibriumElement;

public class DisequilibriumElementComparator implements Comparator<DetectedDisequilibriumElement> {

	@Override
	public int compare(DetectedDisequilibriumElement element1, DetectedDisequilibriumElement element2) {
		if (element1.getDisequilibriumElement() instanceof DisequilibriumElementByRace && element2.getDisequilibriumElement() instanceof DisequilibriumElementByRace) {
			// those with more linkages should sort first
			int ret = ((DisequilibriumElementByRace)element2.getDisequilibriumElement()).getFrequenciesByRace().size() - 
					((DisequilibriumElementByRace)element1.getDisequilibriumElement()).getFrequenciesByRace().size();
			if (ret != 0) {
				return ret;
			}
		}
		
		// else sort alphabetically
		return element1.toString().compareTo(element2.toString());	}
}
