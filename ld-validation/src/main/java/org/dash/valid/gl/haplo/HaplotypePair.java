package org.dash.valid.gl.haplo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.Locus;
import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRace;


public class HaplotypePair {
	private Haplotype haplotype1;
	private Haplotype haplotype2;
	private boolean byRace;
	private EnumSet<Locus> loci;
	
    private static final Logger LOGGER = Logger.getLogger(HaplotypePair.class.getName());
	
	public Object getPrimaryFrequency() {
		if (frequencies.iterator().hasNext()) {
			return frequencies.iterator().next();
		}
		
		return null;
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
	
	Set<Object> frequencies = new LinkedHashSet<Object>();
	
	public Set<Object> getFrequencies() {
		return frequencies;
	}
	
	public void setFrequencies(Set<Object> frequencies) {
		this.frequencies = frequencies;
	}

	public Haplotype getHaplotype1() {
		return haplotype1;
	}

	public Haplotype getHaplotype2() {
		return haplotype2;
	}

	
	public HaplotypePair(Haplotype haplotype1, Haplotype haplotype2) {		
		if (new HaplotypeComparator().compare(haplotype1, haplotype2) <= 0) {
			this.haplotype1 = haplotype1;
			this.haplotype2 = haplotype2;
		}
		else {
			this.haplotype2 = haplotype1;
			this.haplotype1 = haplotype2;
		}
		
		setLoci(Locus.lookup(haplotype1.getLoci()));
		
		if (haplotype1.getLinkage() == null || haplotype2.getLinkage() == null) {
			return;
		}
		
		List<RelativeFrequencyByRace> frequenciesByRaceList = new ArrayList<RelativeFrequencyByRace>();
		double pairFrequency = 0;
				
		if (haplotype1.getLinkage().getDisequilibriumElement() instanceof DisequilibriumElementByRace) {
			setByRace(true);
			
			RelativeFrequencyByRace freqByRace = null;

			for (FrequencyByRace haplo1Freqs : ((DisequilibriumElementByRace) haplotype1.getLinkage().getDisequilibriumElement()).getFrequenciesByRace()) {
				for (FrequencyByRace haplo2Freqs : ((DisequilibriumElementByRace) haplotype2.getLinkage().getDisequilibriumElement()).getFrequenciesByRace()) {
					if (haplo1Freqs.getRace().equals(haplo2Freqs.getRace())) {
						pairFrequency = haplo1Freqs.getFrequency() * haplo2Freqs.getFrequency();
						freqByRace = new RelativeFrequencyByRace(new Double(pairFrequency), haplo1Freqs.getRace());
								
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
			frequencies.add(((BaseDisequilibriumElement) haplotype1.getLinkage().getDisequilibriumElement()).getFrequency() + "*" + 
							((BaseDisequilibriumElement) haplotype2.getLinkage().getDisequilibriumElement()).getFrequency());
		}
	}
	
	@Override
	public boolean equals(Object haplotypePair) {
		if (haplotypePair == null) {
			return false;
		}
		else if ((getHaplotype1().equals(((HaplotypePair) haplotypePair).getHaplotype1()) && 
				getHaplotype2().equals(((HaplotypePair) haplotypePair).getHaplotype2())) ||
				(getHaplotype1().equals(((HaplotypePair) haplotypePair).getHaplotype2()) && 
				getHaplotype2().equals(((HaplotypePair) haplotypePair).getHaplotype1()))) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(haplotype1.getHaplotypeString() + GLStringConstants.NEWLINE +
		haplotype2.getHaplotypeString() + GLStringConstants.NEWLINE);
		
		for (Object frequency : getFrequencies()) {
			sb.append(frequency + GLStringConstants.NEWLINE);
		}
		
		return sb.toString();
	}
}
