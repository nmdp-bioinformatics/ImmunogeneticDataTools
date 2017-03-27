package org.dash.valid;

import java.util.HashMap;
import java.util.List;

import org.dash.valid.gl.haplo.Haplotype;

public class CoreDisequilibriumElement extends DisequilibriumElement {	
	public CoreDisequilibriumElement(HashMap<Locus, List<String>> hlaElementMap, Haplotype haplotype) {
		setHlaElementMap(hlaElementMap);
		setHaplotype(haplotype);
	}
	
	public CoreDisequilibriumElement() {
		
	}
	
	@Override
	public String getFrequencyInfo() {
		// TODO:  What happens if call getFrequencyInfo() here?
		return null;
	}
}
