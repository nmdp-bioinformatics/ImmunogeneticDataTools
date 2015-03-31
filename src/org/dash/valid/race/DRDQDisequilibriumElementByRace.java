package org.dash.valid.race;

import java.util.List;

import org.dash.valid.DRDQDisequilibriumElement;

public class DRDQDisequilibriumElementByRace extends DRDQDisequilibriumElement implements DisequilibriumElementByRace {
	private List<FrequencyByRace> frequenciesByRace;
	
	public DRDQDisequilibriumElementByRace() {
		
	}
	
	public DRDQDisequilibriumElementByRace (String hladrb345Element, String hladrb1Element, String hladqb1Element, List<FrequencyByRace> frequenciesByRace) {
		setHladrb345Element(hladrb345Element);
		setHladrb1Element(hladrb1Element);
		setHladqb1Element(hladqb1Element);
		setFrequenciesByRace(frequenciesByRace);
	}
	
	public List<FrequencyByRace> getFrequenciesByRace() {
		return frequenciesByRace;
	}
	
	public void setFrequenciesByRace(List<FrequencyByRace> frequenciesByRace) {
		this.frequenciesByRace = frequenciesByRace;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("DRB345 Locus: " + this.getHladrb345Element() + "\n" + 
											"DRB1 Locus: " + this.getHladrb1Element() + "\n" +
											"DQB1 Locus: " + this.getHladqb1Element() + "\n");
		
		for (FrequencyByRace freqsByRace : this.frequenciesByRace) {
			sb.append(freqsByRace + "\n");
		}
		
		return sb.toString();
	}


}
