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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.dash.valid.Locus;
import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRace;

@XmlRootElement(name="haplo-pair")
@XmlType(propOrder={"haplotypes", "frequencies", "frequency"})
public class HaplotypePair {
	private List<Haplotype> haplotypes = new ArrayList<Haplotype>();
	private boolean byRace;
	private EnumSet<Locus> loci;
	private Set<RelativeFrequencyByRace> frequencies = new LinkedHashSet<RelativeFrequencyByRace>();
	private String frequency;
	
    private static final Logger LOGGER = Logger.getLogger(HaplotypePair.class.getName());
	
	public Object getPrimaryFrequency() {
		if (frequencies.iterator().hasNext()) {
			return frequencies.iterator().next();
		}
		
		return frequency;
	}
	
	@XmlElement(name="frequency")
	public String getFrequency() {
		return frequency;
	}
	
	public boolean isByRace() {
		return byRace;
	}
	
	private void setByRace(boolean val) {
		this.byRace = val;
	}
	
	public boolean isMatchingLoci(Set<Locus> loci) {
		return this.loci.equals(loci);
	}
	
	private void setLoci(EnumSet<Locus> loci) {
		this.loci = loci;
	}
	
	public EnumSet<Locus> getLoci() {
		return this.loci;
	}
	
	@XmlElement(name="haplotype")
	public List<Haplotype> getHaplotypes() {
		return haplotypes;
	}
		
	@XmlElement(name="frequencies")
	public Set<RelativeFrequencyByRace> getFrequencies() {
		return frequencies;
	}
	
	public HaplotypePair() {
		
	}
	
	public HaplotypePair(Haplotype hap1, Haplotype hap2) {		
		if (new HaplotypeComparator().compare(hap1, hap2) <= 0) {
			hap1.setSequence(1);
			haplotypes.add(hap1);
			hap2.setSequence(2);
			haplotypes.add(hap2);			

		}
		else {
			hap2.setSequence(1);
			haplotypes.add(hap2);
			hap1.setSequence(2);
			haplotypes.add(hap1);
		}
		
		setLoci(Locus.lookup(haplotypes.get(0).getLoci()));
		
		if (haplotypes.get(0).getLinkage() == null || haplotypes.get(1).getLinkage() == null) {
			return;
		}
		
		List<RelativeFrequencyByRace> frequenciesByRaceList = new ArrayList<RelativeFrequencyByRace>();
		double pairFrequency = 0;
				
		if (haplotypes.get(0).getLinkage().getDisequilibriumElement() instanceof DisequilibriumElementByRace) {
			setByRace(true);
			
			RelativeFrequencyByRace freqByRace = null;

			for (FrequencyByRace haplo1Freqs : ((DisequilibriumElementByRace) haplotypes.get(0).getLinkage().getDisequilibriumElement()).getFrequenciesByRace()) {
				for (FrequencyByRace haplo2Freqs : ((DisequilibriumElementByRace) haplotypes.get(1).getLinkage().getDisequilibriumElement()).getFrequenciesByRace()) {
					if (haplo1Freqs.getRace().equals(haplo2Freqs.getRace())) {
						pairFrequency = haplo1Freqs.getFrequency() * haplo2Freqs.getFrequency();
						freqByRace = new RelativeFrequencyByRace(new Double(pairFrequency), haplo1Freqs.getRace(), haplo1Freqs, haplo2Freqs);
								
						frequenciesByRaceList.add(freqByRace);
					}
					else {
						LOGGER.fine("Unusable pair - no overlapping races" + this);
					}				
				}
			}
			
			frequencies.addAll(frequenciesByRaceList);
		}
		else {
			frequency = (((BaseDisequilibriumElement) haplotypes.get(0).getLinkage().getDisequilibriumElement()).getFrequency() + "*" + 
							((BaseDisequilibriumElement) haplotypes.get(1).getLinkage().getDisequilibriumElement()).getFrequency());
		}
	}
	
	@Override
	public boolean equals(Object haplotypePair) {
		if (haplotypePair == null) {
			return false;
		}
		else if ((haplotypes.get(0).equals(((HaplotypePair) haplotypePair).haplotypes.get(0)) && 
				haplotypes.get(1).equals(((HaplotypePair) haplotypePair).haplotypes.get(1))) ||
				(haplotypes.get(0).equals(((HaplotypePair) haplotypePair).haplotypes.get(1)) && 
				haplotypes.get(1).equals(((HaplotypePair) haplotypePair).haplotypes.get(0)))) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(haplotypes.get(0).getHaplotypeString() + GLStringConstants.NEWLINE +
				haplotypes.get(1).getHaplotypeString() + GLStringConstants.NEWLINE);
		
		if (getFrequencies() != null && getFrequencies().size() > 0) {
			for (RelativeFrequencyByRace relativeFrequency : getFrequencies()) {
				sb.append(relativeFrequency + GLStringConstants.NEWLINE);
			}
		}
		else {
			sb.append(frequency);
		}
		
		return sb.toString();
	}
}
