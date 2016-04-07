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
package org.dash.valid.gl.haplo;

import java.util.List;
import java.util.Set;

import org.dash.valid.Locus;
import org.dash.valid.report.DetectedDisequilibriumElement;

public abstract class Haplotype {	
	DetectedDisequilibriumElement linkage;

	public DetectedDisequilibriumElement getLinkage() {
		return linkage;
	}

	public void setLinkage(DetectedDisequilibriumElement linkage) {
		this.linkage = linkage;
	}
	
	public abstract String getHaplotypeString();
	
	public abstract List<String> getAlleles();
	public abstract List<String> getAlleles(Locus locus);
	public abstract Integer getHaplotypeInstance(Locus locus);
	
	public abstract Set<Locus> getLoci();
	
	public abstract List<Integer> getHaplotypeInstances();
	
	public String toString() {
		return getHaplotypeString();
	}
	
	@Override
	public boolean equals(Object element1) {		
		if (getHaplotypeInstances().equals(((Haplotype) element1).getHaplotypeInstances()) && getAlleles().containsAll(((Haplotype) element1).getAlleles()) &&
				getHaplotypeString().equals(((Haplotype) element1).getHaplotypeString()) && 
				(getLinkage() == null || (getLinkage() != null && getLinkage().equals(((Haplotype) element1).getLinkage())))) {
			return true;
		}
		
		return false;
	}
}
