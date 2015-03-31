package org.dash.valid;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

public class LinkageElementsMap extends TreeMap<Object, Boolean> implements SortedMap<Object, Boolean> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6512080112641323193L;

	@Override
	public Comparator<? super Object> comparator() {
		return new DisequilibriumElementComparator();
	}
	
	public LinkageElementsMap(Comparator<Object> comparator) {
		super(comparator);
	}
}
