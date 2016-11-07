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
package org.dash.valid.report;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;


public class DetectedDisequilibriumElement {
	private DisequilibriumElement disequilibriumElement;
	HashMap<Locus, LinkageHitDegree> linkageHitDegreeMap = new HashMap<Locus, LinkageHitDegree>();
	
    protected static final Logger LOGGER = Logger.getLogger(DetectedDisequilibriumElement.class.getName());
    
    public DetectedDisequilibriumElement(DisequilibriumElement disequilibriumElement) {
    	this.disequilibriumElement = disequilibriumElement;
    }

	public DisequilibriumElement getDisequilibriumElement() {
		return disequilibriumElement;
	}

	public void setDisequilibriumElement(DisequilibriumElement disequilibriumElement) {
		this.disequilibriumElement = disequilibriumElement;
	}
		
	public LinkageHitDegree getHitDegree(Locus locus) {
		return linkageHitDegreeMap.get(locus);
	}
	
	public void setHitDegree(Locus locus, LinkageHitDegree hitDegree) {
		linkageHitDegreeMap.put(locus, hitDegree);
	}
	
	public Set<Locus> getLoci() {
		return linkageHitDegreeMap.keySet();
	}
	
	public String toString() {		
		StringBuffer sb = new StringBuffer();
		
		for (Locus locus : getDisequilibriumElement().getLoci()) {
			//sb.append(locus.getShortName() + " Locus: " + getDisequilibriumElement().getHlaElement(locus) + " (" + getHitDegree(locus) + ")" + GLStringConstants.NEWLINE);
			sb.append(locus.getShortName() + " Locus: " + getDisequilibriumElement().getHlaElement(locus) + GLStringConstants.NEWLINE);
		}
				
		sb.append(getDisequilibriumElement().getFrequencyInfo());
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object element) {
		if (getDisequilibriumElement().equals(((DetectedDisequilibriumElement) element).getDisequilibriumElement())) {
			return true;
		}
		
		return false;
	}
}
