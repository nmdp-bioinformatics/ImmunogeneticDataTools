package org.dash;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.dash.valid.DisequilibriumElementComparator;
import org.dash.valid.base.BaseBCDisequilibriumElement;
import org.dash.valid.race.BCDisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.report.DetectedBCDisequilibriumElement;
import org.junit.Test;

public class DisequilibriumElementComparatorTest extends TestCase {

	@Test
	public void testComparisonByRace() {
		List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
		FrequencyByRace freq = new FrequencyByRace(new Double(.2), "1","AAFA");
		frequenciesByRace.add(freq);
		
		BCDisequilibriumElementByRace element1 = new BCDisequilibriumElementByRace("HLA-C*01:01", "HLA-B*07:01", frequenciesByRace);
		
		frequenciesByRace = new ArrayList<FrequencyByRace>();
		
		freq = new FrequencyByRace(new Double(.3), "2", "CAU");
		frequenciesByRace.add(freq);
		freq = new FrequencyByRace(new Double(.4), "3", "API");
		frequenciesByRace.add(freq);
		
		BCDisequilibriumElementByRace element2 = new BCDisequilibriumElementByRace("HLA-C*04:01", "HLA-B*52:01", frequenciesByRace);
		
		DisequilibriumElementComparator comparator = new DisequilibriumElementComparator();
		assertTrue(comparator.compare(new DetectedBCDisequilibriumElement(element1), new DetectedBCDisequilibriumElement(element2)) > 0);
	}

	@Test
	public void testBaseComparison() {
		BaseBCDisequilibriumElement element1 = new BaseBCDisequilibriumElement("HLA-B*52:01", "HLA-C*04:01", "Some frequency", "Some note");

		BaseBCDisequilibriumElement element2 = new BaseBCDisequilibriumElement("HLA-B*07:01", "HLA-C*01:01", "Another frequency", "Another note");
				
		DisequilibriumElementComparator comparator = new DisequilibriumElementComparator();
		assertTrue(comparator.compare(new DetectedBCDisequilibriumElement(element1), new DetectedBCDisequilibriumElement(element2)) > 0);
	}
}
