/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.dash.valid.race;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="linkage")
@XmlType(propOrder={"frequency", "relativeFrequency", "hap1Frequency", "hap1Rank", "hap2Frequency", "hap2Rank"})
public class RelativeFrequencyByRace implements RaceFrequency {
	Float relativeFrequency;
	Double frequency;
	String race;
	FrequencyByRace hap1Frequency;
	FrequencyByRace hap2Frequency;
	
	@XmlElement(name="hap1-frequency")
	public Double getHap1Frequency() {
		return hap1Frequency.getFrequency();
	}
	
	@XmlElement(name="hap1-rank")
	public String getHap1Rank() {
		return hap1Frequency.getRank();
	}

	private void setHap1Frequency(FrequencyByRace hap1Frequency) {
		this.hap1Frequency = hap1Frequency;
	}

	@XmlElement(name="hap2-frequency")
	public Double getHap2Frequency() {
		return hap2Frequency.getFrequency();
	}
	
	@XmlElement(name="hap2-rank")
	public String getHap2Rank() {
		return hap2Frequency.getRank();
	}

	private void setHap2Frequency(FrequencyByRace hap2Frequency) {
		this.hap2Frequency = hap2Frequency;
	}
	
	@XmlElement(name="relative-frequency")
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
	
	public RelativeFrequencyByRace() {
		
	}
	
	public RelativeFrequencyByRace(Double frequency, String race, FrequencyByRace hap1Frequency, FrequencyByRace hap2Frequency) {
		setFrequency(frequency);
		setRace(race);
		setHap1Frequency(hap1Frequency);
		setHap2Frequency(hap2Frequency);
	}
	
	@XmlElement(name="frequency")
	public Double getFrequency() {
		return frequency;
	}
	public void setFrequency(Double frequency) {
		this.frequency = frequency;
	}
	
	@XmlAttribute(name="race")
	public String getRace() {
		return race;
	}
	public void setRace(String race) {
		this.race = race;
	}

	public String getRaceType() {
		return BroadRace.contains(getRace()) ? "Broad" : "Detailed";
	}
	
	public String toString() {
		return getRaceType() + " Race: " + getRace() + ", Freq: + " + getFrequency() + ", Relative Freq (%): + " + getRelativeFrequencyString();
	}
}
