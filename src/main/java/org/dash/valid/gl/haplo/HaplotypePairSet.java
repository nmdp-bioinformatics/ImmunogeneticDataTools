package org.dash.valid.gl.haplo;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class HaplotypePairSet extends TreeSet<HaplotypePair> implements SortedSet<HaplotypePair> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 335889816918287070L;

	@Override
	public Comparator<? super HaplotypePair> comparator() {
		return new HaplotypePairComparator();
	}
	
	public HaplotypePairSet(Comparator<HaplotypePair> comparator) {
		super(comparator);
	}
}
