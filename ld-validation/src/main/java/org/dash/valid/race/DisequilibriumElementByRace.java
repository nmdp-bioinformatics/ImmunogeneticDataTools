package org.dash.valid.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;

public class DisequilibriumElementByRace extends DisequilibriumElement {
	private List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
	
	public DisequilibriumElementByRace() {
		
	}
	
	public DisequilibriumElementByRace (HashMap<Locus, String> hlaElementMap, List<FrequencyByRace> frequenciesByRace) {
		super(hlaElementMap);
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
			sb.append(freqsByRace + GLStringConstants.NEWLINE);
		}
		
		return sb.toString();
	}


}
