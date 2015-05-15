package org.dash.valid.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.LinkageElementsSet;
import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRaceComparator;
import org.dash.valid.race.RelativeFrequencyByRaceSet;

public class DetectedLinkageFindings {
	public static final int EXPECTED_LINKAGES = 2;

	private LinkageDisequilibriumGenotypeList genotypeList;
	private Set<DetectedDisequilibriumElement> linkages = new LinkageElementsSet(new DisequilibriumElementComparator());
	private Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
	private Set<String> nonCWDAlleles;
	private HLADatabaseVersion hladb;
	private int bcLinkageCount;
	private int drdqLinkageCount;
	private boolean bcLinkedPairs;
	private boolean drdqLinkedPairs;
	private HaplotypePair firstBCPair;
	private HaplotypePair firstDRDQPair;
	private List<String> commonRaceElements = new ArrayList<String>();
	
	public boolean hasBcLinkedPairs() {
		return bcLinkedPairs;
	}
	public void setBcLinkedPairs(boolean bcLinkedPairs) {
		this.bcLinkedPairs = bcLinkedPairs;
	}
	public boolean hasDrdqLinkedPairs() {
		return drdqLinkedPairs;
	}
	public void setDrdqLinkedPairs(boolean drdqLinkedPairs) {
		this.drdqLinkedPairs = drdqLinkedPairs;
	}
	
	public HaplotypePair getFirstBCPair() {
		if (firstBCPair == null) {
			for (HaplotypePair pair : linkedPairs) {
				if (pair.isBCPair()) {
					firstBCPair = pair;
					return firstBCPair;
				}
			}
		}
		
		return firstBCPair;
	}
	
	public HaplotypePair getFirstDRDQPair() {
		if (firstDRDQPair == null) {
			for (HaplotypePair pair : linkedPairs) {
				if (!pair.isBCPair()) {
					firstDRDQPair = pair;
					return firstDRDQPair;
				}
			}
		}
		
		return firstDRDQPair;
	}
	
	public Set<HaplotypePair> getLinkedPairs() {
		return linkedPairs;
	}
	
	public void setLinkedPairs(Set<HaplotypePair> linkedPairs) {	
		if (linkedPairs.iterator().hasNext() && linkedPairs.iterator().next().isByRace()) {
			HashMap<String, Double> bcRaceTotalFreqs = new HashMap<String, Double>();
			HashMap<String, Double> drdqRaceTotalFreqs = new HashMap<String, Double>();
			Set<HaplotypePair> noRaceOverlapPairs = new HashSet<HaplotypePair>();

			RelativeFrequencyByRace relativeRaceFreq;
			String race;
			Double totalFreq;
			
			for (HaplotypePair pair : linkedPairs) {
				if (pair.getPrimaryFrequency() == null) {
					noRaceOverlapPairs.add(pair);
					continue;
				}
				for (Object freqByRace : pair.getFrequencies()) {
					relativeRaceFreq = (RelativeFrequencyByRace) freqByRace;
					race = relativeRaceFreq.getRace();

					if (pair.isBCPair()) {
						bcRaceTotalFreqs = calculateTotalFrequency(bcRaceTotalFreqs, relativeRaceFreq,
								race);
					}
					else {
						drdqRaceTotalFreqs = calculateTotalFrequency(drdqRaceTotalFreqs, relativeRaceFreq,
								race);
					}
					
				}
			}
			
			linkedPairs.removeAll(noRaceOverlapPairs);
			
			for (HaplotypePair pair : linkedPairs) {
				Set<RelativeFrequencyByRace> freqsByRace = new RelativeFrequencyByRaceSet(new RelativeFrequencyByRaceComparator());
				for (Object freqByRace : pair.getFrequencies()) {
					relativeRaceFreq = (RelativeFrequencyByRace) freqByRace;
					if (pair.isBCPair()) {
						totalFreq = bcRaceTotalFreqs.get(relativeRaceFreq.getRace());
					}
					else {
						totalFreq = drdqRaceTotalFreqs.get(relativeRaceFreq.getRace());
					}
					
					relativeRaceFreq.setRelativeFrequency(new Float((relativeRaceFreq.getFrequency() * 100) / totalFreq));
					freqsByRace.add(relativeRaceFreq);
				}
				pair.setFrequencies(new LinkedHashSet<Object>(freqsByRace));
				this.linkedPairs.add(pair);
			}
		}
		else {
			this.linkedPairs = linkedPairs;
		}
	}
	/**
	 * @param raceTotalFreqs
	 * @param relativeRaceFreq
	 * @param race
	 */
	private HashMap<String, Double> calculateTotalFrequency(
			HashMap<String, Double> raceTotalFreqs,
			RelativeFrequencyByRace relativeRaceFreq, String race) {
		Double totalFreq;
		
		if (raceTotalFreqs.containsKey(race)) {
			totalFreq = raceTotalFreqs.get(race);
			totalFreq = new Double(totalFreq + relativeRaceFreq.getFrequency());
		}
		else {
			totalFreq = new Double(relativeRaceFreq.getFrequency());
		}

		raceTotalFreqs.put(race, totalFreq);
		
		return raceTotalFreqs;
	}
	
	public int getDrdqLinkageCount() {
		return drdqLinkageCount;
	}
	public boolean hasCommonRaceElements() {
		return getCommonRaceElements().size() > 0;
	}
	public List<String> getCommonRaceElements() {
		return commonRaceElements;
	}
	public int getBcLinkageCount() {
		return bcLinkageCount;
	}
	public HLADatabaseVersion getHladb() {
		return hladb;
	}
	public void setHladb(HLADatabaseVersion hladb) {
		this.hladb = hladb;
	}
	public Set<String> getNonCWDAlleles() {
		return nonCWDAlleles;
	}
	public void setNonCWDAlleles(Set<String> nonCWDAlleles) {
		this.nonCWDAlleles = nonCWDAlleles;
	}
	public LinkageDisequilibriumGenotypeList getGenotypeList() {
		return genotypeList;
	}
	public void setGenotypeList(LinkageDisequilibriumGenotypeList genotypeList) {
		this.genotypeList = genotypeList;
	}
	public Set<DetectedDisequilibriumElement> getLinkages() {
		return linkages;
	}
	
	public void addLinkages(Set<DetectedDisequilibriumElement> linkages) {
		this.linkages.addAll(linkages);
		
		for (DetectedDisequilibriumElement linkage : this.linkages) {
			if (linkage instanceof DetectedBCDisequilibriumElement) {
				this.bcLinkageCount++;
			}
			else if (linkage instanceof DetectedDRDQDisequilibriumElement) {
				this.drdqLinkageCount++;
			}
		}
	}
	
	/**
	 * @param raceLoop
	 * @param linkage
	 * 
	 * This logic is not needed near term - but is a candidate to revisit and re-factor for implementation
	 */
	@SuppressWarnings(value = { "unused" })
	private void detectCommonRaceElements(int raceLoop,
			DetectedDisequilibriumElement linkage) {
		if (linkage.getDisequilibriumElement() instanceof DisequilibriumElementByRace) {	
			List<String> raceElementsFound = new ArrayList<String>();

			for (FrequencyByRace frequencyByRace : ((DisequilibriumElementByRace)linkage.getDisequilibriumElement()).getFrequenciesByRace()) {
				raceElementsFound.add(frequencyByRace.getRace());
			}
			
			if (raceLoop == 0) {
				this.commonRaceElements.addAll(raceElementsFound);
			}
			else {
				this.commonRaceElements.retainAll(raceElementsFound);
			}
		}
	}
	
	public boolean hasLinkages() {
		return this.linkages != null && this.linkages.size() > 0;
	}
	
	public boolean hasAnomalies() {
		return !hasLinkages() || !hasBcLinkedPairs() || !hasDrdqLinkedPairs();
	}
	
	@Override
	public String toString() {
		return getLinkedPairs().toString();
	}
}
