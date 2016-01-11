package org.dash.valid;

import java.io.IOException;

import junit.framework.TestCase;

import org.dash.valid.Locus;
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
		assertTrue(freqLoader.getDisequilibriumElements(Locus.B_C_LOCI) != null && freqLoader.getDisequilibriumElements(Locus.B_C_LOCI).size() > 0);
		assertTrue(freqLoader.getDisequilibriumElements(Locus.DRB1_DQB1_LOCI) != null && freqLoader.getDisequilibriumElements(Locus.DRB1_DQB1_LOCI).size() > 0);
	}

	@Test
	public void testLoadBaseLinkageReferenceData() throws IOException {
		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.WIKIVERSITY.getShortName());
		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
		assertNotNull(freqLoader);
		freqLoader.reloadFrequencies();
		assertTrue(freqLoader.getDisequilibriumElements(Locus.B_C_LOCI) != null && freqLoader.getDisequilibriumElements(Locus.B_C_LOCI).size() > 0);
		assertTrue(freqLoader.getDisequilibriumElements(Locus.DRB_DQ_LOCI) != null && freqLoader.getDisequilibriumElements(Locus.DRB_DQ_LOCI).size() > 0);
	}
}
