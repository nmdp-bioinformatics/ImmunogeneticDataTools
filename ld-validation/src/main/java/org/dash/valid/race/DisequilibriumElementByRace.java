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
