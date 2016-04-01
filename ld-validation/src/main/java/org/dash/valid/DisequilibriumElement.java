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
