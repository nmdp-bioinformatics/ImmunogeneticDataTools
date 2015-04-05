package org.dash.gl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.junit.Test;

public class HLALinkageDisequilibriumTest {
	@Test
	public void testLoadNMDPLinkageReferenceData() {
		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.NMDP.getShortName());
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		assertNotNull(freqLoader);
		freqLoader.reloadFrequencies();
		assertTrue(freqLoader.getBCDisequilibriumElements() != null && freqLoader.getBCDisequilibriumElements().size() > 0);
		assertTrue(freqLoader.getDRDQDisequilibriumElements() != null && freqLoader.getDRDQDisequilibriumElements().size() > 0);
	}

	@Test
	public void testLoadBaseLinkageReferenceData() {
		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.WIKIVERSITY.getShortName());
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		assertNotNull(freqLoader);
		freqLoader.reloadFrequencies();
		assertTrue(freqLoader.getBCDisequilibriumElements() != null && freqLoader.getBCDisequilibriumElements().size() > 0);
		assertTrue(freqLoader.getDRDQDisequilibriumElements() != null && freqLoader.getDRDQDisequilibriumElements().size() > 0);
	}
}
