package org.dash.valid.race;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class RelativeFrequencyByRaceSet extends TreeSet<RelativeFrequencyByRace> implements SortedSet<RelativeFrequencyByRace> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6512080112641323193L;

	@Override
	public Comparator<? super RelativeFrequencyByRace> comparator() {
		return new RelativeFrequencyByRaceComparator();
	}
	
	public RelativeFrequencyByRaceSet(Comparator<RelativeFrequencyByRace> comparator) {
		super(comparator);
	}
}
