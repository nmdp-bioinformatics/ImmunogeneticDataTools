package org.dash.valid.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.LinkageElementsSet;
import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;

public class DetectedLinkageFindings {
	public static final int EXPECTED_LINKAGES = 2;

	private LinkageDisequilibriumGenotypeList genotypeList;
	private Set<DetectedDisequilibriumElement> linkages = new LinkageElementsSet(new DisequilibriumElementComparator());
	private Set<String> nonCWDAlleles;
	private HLADatabaseVersion hladb;
	private int bcLinkageCount;
	private int drdqLinkageCount;
	private List<String> commonRaceElements = new ArrayList<String>();
	
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
		
		// This logic is not needed near term - but is a candidate to revisit and re-factor for implementation
		//int raceLoop = 0;
		
		for (DetectedDisequilibriumElement linkage : this.linkages) {
			if (linkage instanceof DetectedBCDisequilibriumElement) {
				this.bcLinkageCount++;
			}
			else if (linkage instanceof DetectedDRDQDisequilibriumElement) {
				this.drdqLinkageCount++;
			}
			
			// This logic is not needed near term - but is a candidate to revisit and re-factor for implementation
			//detectCommonRaceElements(raceLoop, linkage);
			//raceLoop++;
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
		return !hasLinkages() || getBcLinkageCount() < EXPECTED_LINKAGES || getDrdqLinkageCount() < EXPECTED_LINKAGES;
	}
	
}
