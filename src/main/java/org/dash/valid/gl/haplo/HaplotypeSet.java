package org.dash.valid.gl.haplo;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class HaplotypeSet extends TreeSet<Haplotype> implements SortedSet<Haplotype> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6375128421399449696L;

	@Override
	public Comparator<? super Haplotype> comparator() {
		return new HaplotypeComparator();
	}
	
	public HaplotypeSet(Comparator<Haplotype> comparator) {
		super(comparator);
	}
}
