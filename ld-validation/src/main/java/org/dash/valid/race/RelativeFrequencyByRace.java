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
