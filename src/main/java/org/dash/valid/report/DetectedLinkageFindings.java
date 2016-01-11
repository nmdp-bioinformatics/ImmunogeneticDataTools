package org.dash.valid.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.LinkageElementsSet;
import org.dash.valid.Locus;
import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
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
	
	private HashMap<Set<Locus>, Integer> linkageCountsMap = new HashMap<Set<Locus>, Integer>();
	private HashMap<EnumSet<Locus>, Boolean> linkedPairsMap = new HashMap<EnumSet<Locus>, Boolean>();
	private HashMap<EnumSet<Locus>, HaplotypePair> firstPairsMap = new HashMap<EnumSet<Locus>, HaplotypePair>();
	
	private List<String> commonRaceElements = new ArrayList<String>();
	
	private Set<EnumSet<Locus>> findingsSought = new HashSet<EnumSet<Locus>>();
	
	HashMap<EnumSet<Locus>, HashMap<String, List<Float>>> minimumDifferenceMapOfMaps = new HashMap<EnumSet<Locus>, HashMap<String, List<Float>>>();
		
	private Float getMinimumDifference(HashMap<String, List<Float>> minimumDifferenceMap) {
		List<Float> mins = new ArrayList<Float>();
		
		if (minimumDifferenceMap == null) {
			return null;
		}
		
		for (List<Float> relativeFrequencies : minimumDifferenceMap.values()) {
			Float[] a = new Float[relativeFrequencies.size()];
			a = relativeFrequencies.toArray(a);
			Arrays.sort(a);
			float minDiff;
			
			if (a.length == 1) {
				minDiff = a[0];
			}
			else {
				minDiff = a[1]-a[0];
			}
			
			for (int i = 2 ; i < a.length ; i++) {
			    minDiff = Math.min(minDiff, a[i]-a[i-1]);
			}
			
			mins.add(minDiff);
		}
		
		Collections.sort(mins);
		
		return mins.get(0);
	}
	
	public Float getMinimumDifference(EnumSet<Locus> loci) {		
		return getMinimumDifference(minimumDifferenceMapOfMaps.get(loci));
	}
	
	public void addFindingSought(EnumSet<Locus> findingSought) {
		this.findingsSought.add(findingSought);
	}
	
	public boolean hasLinkedPairs(Set<Locus> loci) {
		if (linkedPairsMap.get(loci) == null) {
			return false;
		}
		
		return linkedPairsMap.get(loci);
	}
	
	public Set<EnumSet<Locus>> getFindingsSought() {
		return this.findingsSought;
	}
	
	public void setLinkedPairs(EnumSet<Locus> loci, boolean linkedPairs) {
		linkedPairsMap.put(loci, linkedPairs);
	}
	
	public HaplotypePair getFirstPair(EnumSet<Locus> loci) {
		if (firstPairsMap.containsKey(loci)) {
			return firstPairsMap.get(loci);
		}
		else {
			for (HaplotypePair pair : linkedPairs) {
				if (Locus.lookup(pair.getLoci()).equals(loci)) {
					firstPairsMap.put(loci, pair);
					return pair;
				}
			}
		}
		
		return null;
	}
	
	public Collection<HaplotypePair> getFirstPairs() {
		return firstPairsMap.values();
	}
	
	public Set<HaplotypePair> getLinkedPairs() {
		return linkedPairs;
	}
	
	public void setLinkedPairs(Set<HaplotypePair> linkedPairs) {	
		if (linkedPairs.iterator().hasNext() && linkedPairs.iterator().next().isByRace()) {
			HashMap<Set<Locus>, HashMap<String, Double>> raceTotalFreqsMap = new HashMap<Set<Locus>, HashMap<String, Double>>();
			
			Set<HaplotypePair> noRaceOverlapPairs = new HashSet<HaplotypePair>();

			RelativeFrequencyByRace relativeRaceFreq;
			String race;
			Double totalFreq;
			EnumSet<Locus> loci = null;
									
			for (HaplotypePair pair : linkedPairs) {
				loci = Locus.lookup(pair.getLoci());

				if (pair.getPrimaryFrequency() == null) {
					noRaceOverlapPairs.add(pair);
					continue;
				}
				
				for (Object freqByRace : pair.getFrequencies()) {
					relativeRaceFreq = (RelativeFrequencyByRace) freqByRace;
					race = relativeRaceFreq.getRace();
					
					raceTotalFreqsMap = calculateTotalFrequency(loci, raceTotalFreqsMap, relativeRaceFreq,
							race);
				}
			}
			
			linkedPairs.removeAll(noRaceOverlapPairs);
			
			HashMap<String, List<Float>> minimumDifferenceMap;

			for (HaplotypePair pair : linkedPairs) {
				loci = Locus.lookup(pair.getLoci());
				minimumDifferenceMap = minimumDifferenceMapOfMaps.get(loci) != null ? minimumDifferenceMapOfMaps.get(loci) : new HashMap<String, List<Float>>();
				
				Set<RelativeFrequencyByRace> freqsByRace = new RelativeFrequencyByRaceSet(new RelativeFrequencyByRaceComparator());
				for (Object freqByRace : pair.getFrequencies()) {
					relativeRaceFreq = (RelativeFrequencyByRace) freqByRace;
										
					totalFreq = raceTotalFreqsMap.get(loci).get(relativeRaceFreq.getRace());
					
					relativeRaceFreq.setRelativeFrequency(new Float((relativeRaceFreq.getFrequency() * 100) / totalFreq));
					freqsByRace.add(relativeRaceFreq);
					
					List<Float> relativeFrequencies;
					if (minimumDifferenceMap.containsKey(relativeRaceFreq.getRace())) {
						relativeFrequencies = minimumDifferenceMap.get(relativeRaceFreq.getRace());
					}
					else {
						relativeFrequencies = new ArrayList<Float>();
					}
					relativeFrequencies.add(relativeRaceFreq.getRelativeFrequency());
					minimumDifferenceMap.put(relativeRaceFreq.getRace(), relativeFrequencies);
				}

				minimumDifferenceMapOfMaps.put(loci, minimumDifferenceMap);
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
	private HashMap<Set<Locus>, HashMap<String, Double>> calculateTotalFrequency(
			EnumSet<Locus> loci, HashMap<Set<Locus>, HashMap<String, Double>> raceTotalFreqsMap,
			RelativeFrequencyByRace relativeRaceFreq, String race) {
		Double totalFreq;
		
		HashMap<String, Double> raceTotalFreqs = raceTotalFreqsMap.get(loci);
		if (raceTotalFreqs == null) {
			raceTotalFreqs = new HashMap<String, Double>();
		}
		
		if (raceTotalFreqs != null && raceTotalFreqs.containsKey(race)) {
			totalFreq = raceTotalFreqs.get(race);
			totalFreq = new Double(totalFreq + relativeRaceFreq.getFrequency());
		}
		else {
			totalFreq = new Double(relativeRaceFreq.getFrequency());
		}

		raceTotalFreqs.put(race, totalFreq);
		
		raceTotalFreqsMap.put(loci, raceTotalFreqs);
		
		return raceTotalFreqsMap;
	}
	
	public int getLinkageCount(EnumSet<Locus> loci) {
		if (linkageCountsMap.get(loci) == null) {
			return 0;
		}
	
		return linkageCountsMap.get(loci);
	}
	
	public boolean hasCommonRaceElements() {
		return getCommonRaceElements().size() > 0;
	}
	public List<String> getCommonRaceElements() {
		return commonRaceElements;
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
		
		int linkageCount = 0;
		for (DetectedDisequilibriumElement linkage : this.linkages) {
			linkageCount = getLinkageCount(Locus.lookup(linkage.getDisequilibriumElement().getLoci()));
			linkageCount++;
			this.linkageCountsMap.put(linkage.getDisequilibriumElement().getLoci(), linkageCount);
		}
	}
	
	public boolean hasLinkages() {
		return this.linkages != null && this.linkages.size() > 0;
	}
	
	public boolean hasAnomalies() {
		if (!hasLinkages()) {
			return true;
		}
		
		for (EnumSet<Locus> findingSought : this.findingsSought) {
			if (!hasLinkedPairs(findingSought)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return getLinkedPairs().toString();
	}
}
