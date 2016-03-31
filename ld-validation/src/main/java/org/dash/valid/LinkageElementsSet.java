package org.dash.valid;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dash.valid.report.DetectedDisequilibriumElement;

public class LinkageElementsSet extends TreeSet<DetectedDisequilibriumElement> implements SortedSet<DetectedDisequilibriumElement> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6512080112641323193L;

	@Override
	public Comparator<? super DetectedDisequilibriumElement> comparator() {
		return new DisequilibriumElementComparator();
	}
	
	public LinkageElementsSet(Comparator<DetectedDisequilibriumElement> comparator) {
		super(comparator);
	}
}
