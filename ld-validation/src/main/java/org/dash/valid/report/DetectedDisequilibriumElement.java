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

import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.CoreDisequilibriumElement;
import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.LocusComparator;
import org.dash.valid.LocusSet;
import org.dash.valid.gl.GLStringConstants;


public class DetectedDisequilibriumElement {
	private DisequilibriumElement disequilibriumElement;
	
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
	
	public String toString() {		
		StringBuffer sb = new StringBuffer();
		
		Set<Locus> loci = new LocusSet(new LocusComparator());
		loci.addAll(getDisequilibriumElement().getLoci());
		
		for (Locus locus : loci) {
			// TODO:  Make less clumsy to get rid of brackets?

			//sb.append(locus.getShortName() + " Locus: " + getDisequilibriumElement().getHlaElement(locus) + GLStringConstants.NEWLINE);
			if (getDisequilibriumElement().getHlaElement(locus).size() == 1) {
				sb.append(getDisequilibriumElement().getHlaElement(locus).get(0));
			} else {
				sb.append(getDisequilibriumElement().getHlaElement(locus));
			}
			
			sb.append(GLStringConstants.GENE_PHASE_DELIMITER);
		}
			
		return sb.substring(0, sb.length() - 1) + GLStringConstants.NEWLINE + ((CoreDisequilibriumElement) getDisequilibriumElement()).getFrequencyInfo();
	}
	
	@Override
	public boolean equals(Object element) {
		if (getDisequilibriumElement().equals(((DetectedDisequilibriumElement) element).getDisequilibriumElement())) {
			return true;
		}
		
		return false;
	}
}
