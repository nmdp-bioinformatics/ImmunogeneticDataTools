package org.dash.valid;

import java.util.Comparator;
import java.util.EnumSet;

public class LocusComparator implements Comparator<Locus> {

	@Override
	public int compare(Locus element1, Locus element2) {
		if (element1.equals(element2)) {
			return 0;
		}
		
		EnumSet<Locus> locusSet = EnumSet.allOf(Locus.class);
		
		int element1Idx = 0;
		int element2Idx = 0;
		
		int idx = 0;
		
		for (Locus locus : locusSet) {
			if (locus.equals(element1)) {
				element1Idx = idx;
			}
			if (locus.equals(element2)) {
				element2Idx = idx;
			}
			idx++;
		}
		
		return element1Idx - element2Idx;
	}
}
