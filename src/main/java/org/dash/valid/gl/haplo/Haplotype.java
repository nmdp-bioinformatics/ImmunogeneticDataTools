package org.dash.valid.gl.haplo;

import java.util.Arrays;
import java.util.Set;

import org.dash.valid.Locus;
import org.dash.valid.report.DetectedDisequilibriumElement;

public abstract class Haplotype {	
	DetectedDisequilibriumElement linkage;

	public DetectedDisequilibriumElement getLinkage() {
		return linkage;
	}

	public void setLinkage(DetectedDisequilibriumElement linkage) {
		this.linkage = linkage;
	}
	
	public abstract String getHaplotypeString();
	
	public abstract Set<String> getAlleles();
	public abstract Set<String> getAlleles(Locus locus);
	public abstract int getHaplotypeInstance(Locus locus);
	public abstract int[] getHaplotypeInstances();
	
	public String toString() {
		return getHaplotypeString();
	}
	
	@Override
	public boolean equals(Object element1) {		
		if (Arrays.equals(getHaplotypeInstances(), ((Haplotype) element1).getHaplotypeInstances()) && getAlleles().containsAll(((Haplotype) element1).getAlleles()) &&
				getHaplotypeString().equals(((Haplotype) element1).getHaplotypeString()) && getLinkage().equals(((Haplotype) element1).getLinkage())) {
			return true;
		}
		
		return false;
	}
}
