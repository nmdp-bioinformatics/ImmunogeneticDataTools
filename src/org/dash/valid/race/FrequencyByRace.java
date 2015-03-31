package org.dash.valid.race;

public class FrequencyByRace {
	Double frequency;
	String rank;
	String race;
	
	public FrequencyByRace(Double frequency, String rank, String race) {
		this.frequency = frequency;
		this.rank = rank;
		this.race = race;
	}
	
	public Double getFrequency() {
		return frequency;
	}
	public void setFrequency(Double frequency) {
		this.frequency = frequency;
	}
	public String getRank() {
		return rank;
	}
	public void setRank(String rank) {
		this.rank = rank;
	}
	public String getRace() {
		return race;
	}
	public void setRace(String race) {
		this.race = race;
	}
	
	public String toString() {
		return "Race: " + getRace() + ", Freq: + " + getFrequency() + ", Rank: + " + getRank();
	}
}
