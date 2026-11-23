package org.dash.valid.freq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.EnumSet;
import java.util.Set;

import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
import org.junit.jupiter.api.Test;

// supportedLinkages() is hand-mirrored from the switch in HLAFrequenciesLoader#init(Frequencies); a
// service uses it to reject linkages a set has no data for. This loads each bundled set once per
// linkage and checks the two agree, so adding a case to one without the other fails here.
// (nmdp-2026 needs a download and nmdp-std/nmdp are not bundled, so they are not loaded here.)
public class HLAFrequenciesLoaderSupportedLinkagesTest {

	@Test
	public void supportedLinkagesMatchWhatEachBundledSetActuallyLoads() {
		String previous = System.getProperty(Frequencies.FREQUENCIES_PROPERTY);
		try {
			for (Frequencies freq : new Frequencies[] {Frequencies.NMDP_2007, Frequencies.NMDP_2007_STD, Frequencies.WIKIVERSITY}) {
				for (Linkages linkage : Linkages.values()) {
					System.setProperty(Frequencies.FREQUENCIES_PROPERTY, freq.getShortName());
					LinkagesLoader.reset();
					LinkagesLoader.getInstance(EnumSet.of(linkage));
					HLAFrequenciesLoader.reset();

					Set<?> loaded = HLAFrequenciesLoader.getInstance().getLoci();
					assertEquals(HLAFrequenciesLoader.supportedLinkages(freq).contains(linkage), !loaded.isEmpty(),
							freq.getShortName() + " / " + linkage.getShortName() + " loaded " + loaded);
				}
			}
		}
		finally {
			if (previous == null) {
				System.clearProperty(Frequencies.FREQUENCIES_PROPERTY);
			}
			else {
				System.setProperty(Frequencies.FREQUENCIES_PROPERTY, previous);
			}
			LinkagesLoader.reset();
			HLAFrequenciesLoader.reset();
		}
	}

	@Test
	public void nmdp2026OffersExactlyTheLinkagesItHasFilesFor() {
		assertFalse(HLAFrequenciesLoader.supportedLinkages(Frequencies.NMDP_2026).contains(Linkages.B_C));
		assertFalse(HLAFrequenciesLoader.supportedLinkages(Frequencies.NMDP_2026).contains(Linkages.DRB_DQB));
	}
}
