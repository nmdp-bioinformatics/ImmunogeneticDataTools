package org.dash.valid;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class LocusSet extends TreeSet<Locus> implements SortedSet<Locus> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 335889816918287070L;

	@Override
	public Comparator<? super Locus> comparator() {
		return new LocusComparator();
	}
	
	public LocusSet(Comparator<Locus> comparator) {
		super(comparator);
	}
}

