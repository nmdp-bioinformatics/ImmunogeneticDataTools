package org.dash.valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;

public class DetectedLinkageFindings {
	public static final int EXPECTED_LINKAGES = 2;

	private LinkageDisequilibriumGenotypeList genotypeList;
	private Map<Object, Boolean> linkages = new LinkageElementsMap(new DisequilibriumElementComparator());
	private List<String> nonCWDAlleles;
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
	public List<String> getNonCWDAlleles() {
		return nonCWDAlleles;
	}
	public void setNonCWDAlleles(List<String> nonCWDAlleles) {
		this.nonCWDAlleles = nonCWDAlleles;
	}
	public LinkageDisequilibriumGenotypeList getGenotypeList() {
		return genotypeList;
	}
	public void setGenotypeList(LinkageDisequilibriumGenotypeList genotypeList) {
		this.genotypeList = genotypeList;
	}
	public Map<Object, Boolean> getLinkages() {
		return linkages;
	}
	
	public void addLinkages(Map<Object, Boolean> linkages) {
		this.linkages.putAll(linkages);
		
		int raceLoop = 0;
		
		for (Object linkage : this.linkages.keySet()) {
			if (linkage instanceof BCDisequilibriumElement) {
				this.bcLinkageCount++;
			}
			else if (linkage instanceof DRDQDisequilibriumElement) {
				this.drdqLinkageCount++;
			}
			
			if (linkage instanceof DisequilibriumElementByRace) {	
				List<String> raceElementsFound = new ArrayList<String>();

				for (FrequencyByRace frequencyByRace : ((DisequilibriumElementByRace)linkage).getFrequenciesByRace()) {
					raceElementsFound.add(frequencyByRace.getRace());
				}
				
				if (raceLoop == 0) {
					this.commonRaceElements.addAll(raceElementsFound);
				}
				else {
					this.commonRaceElements.retainAll(raceElementsFound);
				}
			}
			raceLoop++;
		}
	}
	
	public boolean hasLinkages() {
		return this.linkages != null && this.linkages.size() > 0;
	}
	
}
