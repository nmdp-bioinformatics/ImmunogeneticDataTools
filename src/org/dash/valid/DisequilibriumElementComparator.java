package org.dash.valid;

import java.util.Comparator;

import org.dash.valid.race.DisequilibriumElementByRace;

public class DisequilibriumElementComparator implements Comparator<Object> {

	@Override
	public int compare(Object element1, Object element2) {
		if (element1 instanceof DisequilibriumElementByRace && element2 instanceof DisequilibriumElementByRace) {
			// those with more linkages should sort first
			return ((DisequilibriumElementByRace)element2).getFrequenciesByRace().size() - ((DisequilibriumElementByRace)element1).getFrequenciesByRace().size();
		}
		
		// else sort alphabetically
		return element1.toString().compareTo(element2.toString());
	}
}
