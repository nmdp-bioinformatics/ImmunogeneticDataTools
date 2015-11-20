package org.dash.valid;

import java.util.HashMap;
import java.util.Set;

public abstract class DisequilibriumElement {
	HashMap<Locus, String> hlaElementMap = new HashMap<Locus, String>();
	
	public DisequilibriumElement() {
		
	}
	
	public void setHlaElement(Locus locus, String hlaElement) {
		hlaElementMap.put(locus, hlaElement);
	}
	
	public String getHlaElement(Locus locus) {
		return hlaElementMap.get(locus);
	}
	
	public DisequilibriumElement(HashMap<Locus, String> hlaElementMap) {
		this.hlaElementMap = hlaElementMap;
	}
	
	public Set<Locus> getLoci() {
		return hlaElementMap.keySet();
	}
	
	public abstract String getFrequencyInfo();
	
}
