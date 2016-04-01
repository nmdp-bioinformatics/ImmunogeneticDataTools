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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.Locus;
import org.dash.valid.LocusComparator;
import org.dash.valid.LocusSet;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.report.DetectedDisequilibriumElement;

public class MultiLocusHaplotype extends Haplotype {
	private static final Logger LOGGER = Logger.getLogger(MultiLocusHaplotype.class.getName());
	
	private HashMap<Locus, List<String>> alleleMap = new HashMap<Locus, List<String>>();
	private HashMap<Locus, Integer> haplotypeInstanceMap = new HashMap<Locus, Integer>();
	
	@Override
	public Set<Locus> getLoci() {
		return alleleMap.keySet();
	}
	
	@Override
	public Integer getHaplotypeInstance(Locus locus) {
		return haplotypeInstanceMap.get(locus);
	}
	
	@Override
	public List<Integer> getHaplotypeInstances() {
		return new ArrayList<Integer>(haplotypeInstanceMap.values());
	}
	
	public HashMap<Locus, Integer> getHaplotypeInstanceMap() {
		return haplotypeInstanceMap;
	}
	
	@Override
	public List<String> getAlleles(Locus locus) {
		List<String> alleles = null;
		switch (locus) {
		case HLA_DRB345:
			alleles = new ArrayList<String>();
			if (alleleMap.containsKey(Locus.HLA_DRB345)) alleles.addAll(alleleMap.get(Locus.HLA_DRB345));
			else {
				if (alleleMap.containsKey(Locus.HLA_DRB3)) alleles.addAll(alleleMap.get(Locus.HLA_DRB3));
				if (alleleMap.containsKey(Locus.HLA_DRB4)) alleles.addAll(alleleMap.get(Locus.HLA_DRB4));
				if (alleleMap.containsKey(Locus.HLA_DRB5)) alleles.addAll(alleleMap.get(Locus.HLA_DRB5));
				if (alleleMap.containsKey(Locus.HLA_DRBX)) alleles.addAll(alleleMap.get(Locus.HLA_DRBX));
			}
			break;
		default:
			alleles = alleleMap.get(locus);
		}
		if (alleles == null) {
			return new ArrayList<String>();
		}
		return alleles;
	}
	
	public HashMap<Locus, List<String>> getAlleleMap() {
		return this.alleleMap;
	}
	
	@Override
	public List<String> getAlleles() {
		List<String> alleleSet = new ArrayList<String>();
		for (List<String> singleLocusAlleles : alleleMap.values()) {
			alleleSet.addAll(singleLocusAlleles);
		}
		return alleleSet;
	}
	
	public MultiLocusHaplotype(HashMap<Locus, SingleLocusHaplotype> singleLocusHaplotypes) {
		for (SingleLocusHaplotype singleLocusHaplotype : singleLocusHaplotypes.values()) {
			alleleMap.put(singleLocusHaplotype.getLocus(), singleLocusHaplotype.getAlleles());	
			haplotypeInstanceMap.put(singleLocusHaplotype.getLocus(), singleLocusHaplotype.getHaplotypeInstance());
		}
	}
	
	public MultiLocusHaplotype(DetectedDisequilibriumElement foundElement, MultiLocusHaplotype haplotype) {
		this.haplotypeInstanceMap = haplotype.getHaplotypeInstanceMap();
		Set<Locus> loci = haplotype.getAlleleMap().keySet();
		
		List<String> alleleSet;
		for (Locus locus : loci) {
			alleleSet = new ArrayList<String>();
			if (foundElement != null && foundElement.getHitDegree(locus) != null) {
				alleleSet.add(foundElement.getHitDegree(locus).getAllele());
				alleleMap.put(locus, alleleSet);
			}
			else {				
				LOGGER.warning("Either the element or the hit degree for locus: " + locus + " was null");
			}
		}
		
		setLinkage(foundElement);
	}
	
	@Override
	public String getHaplotypeString() {
		StringBuffer sb = new StringBuffer();

		Set<Locus> keySet = getAlleleMap().keySet();
		Set<Locus> loci = new LocusSet(new LocusComparator());
		loci.addAll(keySet);

		if (this.linkage != null) {
			for (Locus locus : loci) {
				sb.append(linkage.getHitDegree(locus).getMatchedValue());
				sb.append(GLStringConstants.GENE_PHASE_DELIMITER);
			}
		}
		else {
			for (Locus locus : loci) {
				sb.append(getAlleles(locus));
				sb.append(GLStringConstants.GENE_DELIMITER);
			}
		}
		return sb.substring(0,  sb.length() - 1);
	}
}
