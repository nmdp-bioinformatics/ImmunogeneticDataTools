package org.dash.valid;

import java.io.IOException;

import junit.framework.TestCase;

import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.junit.Test;

public class HLAFrequenciesLoaderTest extends TestCase {
	@Test
	public void testLoadNMDPLinkageReferenceData() throws IOException {
		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.NMDP_2007.getShortName());
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		assertNotNull(freqLoader);
		freqLoader.reloadFrequencies();
		assertTrue(freqLoader.getDisequilibriumElements(Locus.FIVE_LOCUS) != null && freqLoader.getDisequilibriumElements(Locus.FIVE_LOCUS).size() > 0);
	}
}
