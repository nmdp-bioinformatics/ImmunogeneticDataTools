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

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.haplo.Haplotype;


public abstract class DisequilibriumElement {	
	private Haplotype haplotype;
	public DisequilibriumElement() {
		super();
	}
	
	public Haplotype getHaplotype() {
		return this.haplotype;
	}
	
	public void setHaplotype(Haplotype haplotype) {
		this.haplotype = haplotype;
	}

	public abstract String getFrequencyInfo();
		
	private HashMap<Locus, String> hlaElementMap = new HashMap<Locus, String>();

	public Collection<String> getHlaElements() {
		return hlaElementMap.values();
	}
	
	public void setHlaElementMap(HashMap<Locus, String> hlaElementMap) {
		this.hlaElementMap = hlaElementMap;
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
		Set<Locus> loci = hlaElementMap.keySet();
		
		return loci;
	}
	
	@Override
	public boolean equals(Object element1) {
		if (getLoci().equals(((DisequilibriumElement) element1).getLoci())) {
			if (getHlaElements().containsAll(((DisequilibriumElement) element1).getHlaElements())) {
				return true;
			}
		}
		
		for (Locus locus : getLoci()) {
			if (GLStringUtilities.fieldLevelComparison(getHlaElement(locus), ((DisequilibriumElement)element1).getHlaElement(locus)) != null) {
				continue;
			}
			
			if (GLStringUtilities.checkAntigenRecognitionSite(getHlaElement(locus), ((DisequilibriumElement)element1).getHlaElement(locus)) != null) {
				continue;
			}
				
			if (Locus.isDRB345(locus) &&
					(getHaplotype() != null &&
					getHaplotype().getDrb345Homozygous() && 
					(getHlaElement(locus).equals(GLStringConstants.DASH) || getHlaElement(locus).equals(GLStringConstants.NNNN))) ||
					(((DisequilibriumElement) element1).getHaplotype() != null &&
					((DisequilibriumElement) element1).getHaplotype().getDrb345Homozygous() && 
					(((DisequilibriumElement) element1).getHlaElement(locus).equals(GLStringConstants.DASH) || 
					((DisequilibriumElement) element1).getHlaElement(locus).equals(GLStringConstants.NNNN))))
			{
				continue;
			}
				
			return false;
		}
		
		return true;
	}
}
