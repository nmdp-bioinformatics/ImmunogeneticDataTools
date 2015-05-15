package org.dash.valid.race;

public class RelativeFrequencyByRace implements RaceFrequency {
	Double frequency;
	Float relativeFrequency;
	
	public Float getRelativeFrequency() {
		return relativeFrequency;
	}
	
	private String getRelativeFrequencyString() {
		String formattedString = String.format("%.02f", getRelativeFrequency());
		return formattedString;
	}

	public void setRelativeFrequency(Float relativeFrequency) {
		this.relativeFrequency = relativeFrequency;
	}

	String race;
	
	public RelativeFrequencyByRace(Double frequency, String race) {
		this.frequency = frequency;
		this.race = race;
	}
	
	public Double getFrequency() {
		return frequency;
	}
	public void setFrequency(Double frequency) {
		this.frequency = frequency;
	}
	public String getRace() {
		return race;
	}
	public void setRace(String race) {
		this.race = race;
	}
	public String getRaceType() {
		return BroadRace.contains(race) ? "Broad" : "Detailed";
	}
	
	public String toString() {
		return getRaceType() + " Race: " + getRace() + ", Freq: + " + getFrequency() + ", Relative Freq (%): + " + getRelativeFrequencyString();
	}
}
