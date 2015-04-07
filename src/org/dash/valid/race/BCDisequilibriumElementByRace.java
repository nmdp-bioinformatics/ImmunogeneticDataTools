package org.dash.valid.race;

import java.util.ArrayList;
import java.util.List;

import org.dash.valid.BCDisequilibriumElement;

public class BCDisequilibriumElementByRace extends BCDisequilibriumElement implements DisequilibriumElementByRace {
	private List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
	
	public BCDisequilibriumElementByRace() {
		
	}
	
	public BCDisequilibriumElementByRace (String hlacElement, String hlabElement, List<FrequencyByRace> frequenciesByRace) {
		setHlabElement(hlabElement);
		setHlacElement(hlacElement);
		setFrequenciesByRace(frequenciesByRace);
	}
	
	public List<FrequencyByRace> getFrequenciesByRace() {
		return frequenciesByRace;
	}
	
	public void setFrequenciesByRace(List<FrequencyByRace> frequenciesByRace) {
		this.frequenciesByRace = frequenciesByRace;
	}
	
	@Override
	public String getFrequencyInfo() {
		StringBuffer sb = new StringBuffer();
		
		for (FrequencyByRace freqsByRace : this.frequenciesByRace) {
			sb.append(freqsByRace + "\n");
		}
		
		return sb.toString();
	}


}
