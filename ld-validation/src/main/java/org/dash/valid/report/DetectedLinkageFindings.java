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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.LinkageElementsSet;
import org.dash.valid.Locus;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.race.RelativeFrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRaceComparator;
import org.dash.valid.race.RelativeFrequencyByRaceSet;


@XmlRootElement(name="gl-freq")
@XmlType(propOrder={"GLString", "nonCWDAlleles", "linkedPairs"})
public class DetectedLinkageFindings {
	public static final int EXPECTED_LINKAGES = 2;

	private LinkageDisequilibriumGenotypeList genotypeList;
	private Set<DetectedDisequilibriumElement> linkages = new LinkageElementsSet(new DisequilibriumElementComparator());
	private Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
	private Set<String> nonCWDAlleles;
	private String hladb;
	private String frequencies;
	
	private HashMap<Set<Locus>, Integer> linkageCountsMap = new HashMap<Set<Locus>, Integer>();
	private HashMap<EnumSet<Locus>, Boolean> linkedPairsMap = new HashMap<EnumSet<Locus>, Boolean>();
	private HashMap<EnumSet<Locus>, HaplotypePair> firstPairsMap = new HashMap<EnumSet<Locus>, HaplotypePair>();
	
	private List<String> commonRaceElements = new ArrayList<String>();
	
	private Set<EnumSet<Locus>> findingsSought = new HashSet<EnumSet<Locus>>();
	
	private HashMap<EnumSet<Locus>, HashMap<String, List<Float>>> minimumDifferenceMapOfMaps = new HashMap<EnumSet<Locus>, HashMap<String, List<Float>>>();
	
	public DetectedLinkageFindings() {
		
	}
	
	public DetectedLinkageFindings(String frequencies) {
		this.frequencies = frequencies;
	}
	
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
	
	@XmlTransient
	public Collection<HaplotypePair> getFirstPairs() {
		return firstPairsMap.values();
	}
	
	@XmlElement(name="haplo-pair")
	public Set<HaplotypePair> getLinkedPairs() {
		return linkedPairs;
	}
	
	public void setLinkedPairs(Set<HaplotypePair> linkedPairs) {	
		EnumSet<Locus> loci = null;
		
		if (linkedPairs.iterator().hasNext() && linkedPairs.iterator().next().isByRace()) {
			HashMap<Set<Locus>, HashMap<String, Double>> raceTotalFreqsMap = new HashMap<Set<Locus>, HashMap<String, Double>>();
			
			Set<HaplotypePair> noRaceOverlapPairs = new HashSet<HaplotypePair>();

			RelativeFrequencyByRace relativeRaceFreq;
			String race;
			Double totalFreq;
									
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
				setLinkedPairs(loci, true);

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
			for (HaplotypePair pair : linkedPairs) {
				loci = Locus.lookup(pair.getLoci());
				setLinkedPairs(loci, true);
			}
			
			this.linkedPairs = linkedPairs;
		}
		
		Set<EnumSet<Locus>> lociSet = this.linkedPairsMap.keySet();
		for (EnumSet<Locus> lociInSet : lociSet) {
			getFirstPair(lociInSet);
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
	
	@XmlAttribute(name="frequency-set")
	public String getFrequencies() {
		return frequencies;
	}

	@XmlAttribute(name="hladb")
	public String getHladb() {
		return this.hladb;
	}
	
	public void setHladb(String hladb) {
		this.hladb = hladb;
	}
	
	@XmlElement(name="non-cwd")
	public Set<String> getNonCWDAlleles() {
		return nonCWDAlleles;
	}
	
	public void setNonCWDAlleles(Set<String> nonCWDAlleles) {
		this.nonCWDAlleles = nonCWDAlleles;
	}
	
	@XmlAttribute(name="id")
	public String getGLId() {
		return getGenotypeList().getId();
	}
	
	@XmlElement(name="gl-string")
	public String getGLString() {
		return getGenotypeList().getGLString();
	}
	
	
	public int getAlleleCount(Locus locus) {
		return getGenotypeList().getAlleleCount(locus);
	}
	
	private LinkageDisequilibriumGenotypeList getGenotypeList() {
		return genotypeList;
	}
	
	public void setGenotypeList(LinkageDisequilibriumGenotypeList genotypeList) {
		this.genotypeList = genotypeList;
	}
	
	public Set<DetectedDisequilibriumElement> getLinkages() {
		return linkages;
	}
	
	public void addLinkage(DetectedDisequilibriumElement linkage) {
		this.linkages.add(linkage);
		incrementLinkageCount(linkage);
	}
	
	public void addLinkages(Set<DetectedDisequilibriumElement> linkages) {
		this.linkages.addAll(linkages);
		
		for (DetectedDisequilibriumElement linkage : this.linkages) {
			incrementLinkageCount(linkage);
		}
	}

	private void incrementLinkageCount(DetectedDisequilibriumElement linkage) {
		int linkageCount = getLinkageCount(Locus.lookup(linkage.getDisequilibriumElement().getLoci()));
		linkageCount++;
		this.linkageCountsMap.put(linkage.getDisequilibriumElement().getLoci(), linkageCount);
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
