package org.dash.valid.race;

import junit.framework.TestCase;

import org.junit.Test;

public class FrequencyByRaceComparatorTest extends TestCase {

	@Test
	public void testFrequencyByRaceComparator() {
		FrequencyByRaceComparator comparator = new FrequencyByRaceComparator();
		
		FrequencyByRace freq1 = new FrequencyByRace(new Double(.3), "2", BroadRace.CAU.toString());
		FrequencyByRace freq2 = new FrequencyByRace(new Double(.4), "3", BroadRace.API.toString());
		FrequencyByRace freq3 = new FrequencyByRace(new Double(.5), "4", "AAFA");
		
		assertTrue(comparator.compare(freq1, freq2) > 0);
		assertTrue(comparator.compare(freq2, freq3) < 0);
		assertTrue(comparator.compare(freq1, freq3) < 0);
	}
}
