package org.dash.valid;

import java.util.HashMap;

import org.dash.valid.gl.haplo.Haplotype;

public class CoreDisequilibriumElement extends DisequilibriumElement {	
	public CoreDisequilibriumElement(HashMap<Locus, String> hlaElementMap, Haplotype haplotype) {
		setHlaElementMap(hlaElementMap);
		setHaplotype(haplotype);
	}
	
	public CoreDisequilibriumElement() {
		
	}
	
	@Override
	public String getFrequencyInfo() {
		// TODO Auto-generated method stub
		return null;
	}
}
