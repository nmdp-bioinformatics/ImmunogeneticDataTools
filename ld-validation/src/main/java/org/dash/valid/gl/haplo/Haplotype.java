package org.dash.valid.gl.haplo;

import java.util.List;
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
	
	public abstract List<String> getAlleles();
	public abstract List<String> getAlleles(Locus locus);
	public abstract Integer getHaplotypeInstance(Locus locus);
	
	public abstract Set<Locus> getLoci();
	
	public abstract List<Integer> getHaplotypeInstances();
	
	public String toString() {
		return getHaplotypeString();
	}
	
	@Override
	public boolean equals(Object element1) {		
		if (getHaplotypeInstances().equals(((Haplotype) element1).getHaplotypeInstances()) && getAlleles().containsAll(((Haplotype) element1).getAlleles()) &&
				getHaplotypeString().equals(((Haplotype) element1).getHaplotypeString()) && 
				(getLinkage() == null || (getLinkage() != null && getLinkage().equals(((Haplotype) element1).getLinkage())))) {
			return true;
		}
		
		return false;
	}
}
