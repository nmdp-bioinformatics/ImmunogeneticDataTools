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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.dash.valid.Locus;
import org.dash.valid.report.DetectedDisequilibriumElement;

@XmlRootElement(name="haplotype")
@XmlType(propOrder={"sequence", "haplotypeString"})
public abstract class Haplotype {	
	DetectedDisequilibriumElement linkage;
	private boolean drb345Homozygous;
	
	@XmlTransient
	public DetectedDisequilibriumElement getLinkage() {
		return linkage;
	}

	public void setLinkage(DetectedDisequilibriumElement linkage) {
		this.linkage = linkage;
	}
	
	public void setDRB345Homozygous(boolean drb345Homozygous) {
		this.drb345Homozygous = drb345Homozygous;
	}
	
	public boolean getDrb345Homozygous() {
		return this.drb345Homozygous;
	}
	
	@XmlAttribute(name="seq")
	public abstract Integer getSequence();
	public abstract void setSequence(Integer sequence);
	
	@XmlAttribute(name="value")
	public abstract String getHaplotypeString();
	
	public abstract List<String> getAlleles();
	public abstract Map<Locus, List<String>> getAlleleMap();
	public abstract HashMap<Locus, Integer> getHaplotypeInstanceMap();
	public abstract List<String> getAlleles(Locus locus);
	public abstract void removeAlleles(Locus locus);
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
