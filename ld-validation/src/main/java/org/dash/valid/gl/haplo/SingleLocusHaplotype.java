package org.dash.valid.gl.haplo;

import java.util.ArrayList;
import java.util.List;

import org.dash.valid.Locus;

public class SingleLocusHaplotype {
	private List<String> alleles = new ArrayList<String>();
	private int	haplotypeInstance;
	private Locus locus;
	
	public int getHaplotypeInstance() {
		return this.haplotypeInstance;
	}

	public List<String> getAlleles() {
		return this.alleles;
	}
	
	public Locus getLocus() {
		return this.locus;
	}
	
	public SingleLocusHaplotype(Locus locus, List<String> alleles, int haplotypeInstance) {
		this.locus = locus;
		this.alleles = alleles;
		this.haplotypeInstance = haplotypeInstance;
	}
}
