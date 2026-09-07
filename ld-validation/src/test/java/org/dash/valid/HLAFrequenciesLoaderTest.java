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
package org.dash.valid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dash.valid.freq.HLAFrequenciesLoader;
import org.junit.jupiter.api.Test;

public class HLAFrequenciesLoaderTest {

	@Test
	public void testLoadNMDPLinkageReferenceData() throws Exception {		
		List<DisequilibriumElement> disElements = HLAFrequenciesLoader.getInstance().loadNMDPLinkageReferenceData(HLAFrequenciesLoader.NMDP_2007_FIVE_LOCUS_FREQUENCIES, HLAFrequenciesLoader.NMDP_FIVE_LOCUS_POS);
		assertTrue(disElements != null && disElements.size() > 0);
	}
	
	@Test
	public void testLoadStandardFrequenciesNoRank() throws Exception {
		URI uri = HLAFrequenciesLoaderTest.class.getClassLoader().getResource("frequencies/NMDP_2007_FiveLocus_Freqs_NoRank.csv").toURI();
		Set<File> noRankFreqs = new HashSet<File>();
		noRankFreqs.add(new File(uri));

		List<DisequilibriumElement> disElements = HLAFrequenciesLoader.getInstance(noRankFreqs, null).getDisequilibriumElements(Linkages.FIVE_LOCUS.getLoci());
		assertTrue(disElements != null && disElements.size() > 0);
	}

	// Issues #9/#46 regression coverage. Before Locus.NINE_LOCUS/Linkages.NINE_LOCUS existed,
	// loading a real standard-format nine-locus reference file through this exact path threw an
	// unhandled NullPointerException (Locus#lookup(Set) returned null for an unrecognized
	// combination, which Linkages#lookup(EnumSet) then called containsAll() on) -- confirmed via
	// a real reproduction against synthetic data shaped like the real NMDP nine-locus release,
	// not a hypothetical. This test failing with that same NPE is exactly the regression to
	// watch for.
	@Test
	public void testLoadNineLocusStandardFrequencies() throws Exception {
		URI uri = HLAFrequenciesLoaderTest.class.getClassLoader().getResource("frequencies/NineLocus_Freqs.csv").toURI();
		Set<File> nineLocusFreqs = new HashSet<File>();
		nineLocusFreqs.add(new File(uri));

		List<DisequilibriumElement> disElements = HLAFrequenciesLoader.getInstance(nineLocusFreqs, null).getDisequilibriumElements(Locus.NINE_LOCUS);
		assertTrue(disElements != null && disElements.size() > 0);
	}

	// Same regression, for the narrower DPA1~DPB1-only combination issue #9 asked for by name.
	@Test
	public void testLoadDpa1Dpb1StandardFrequencies() throws Exception {
		URI uri = HLAFrequenciesLoaderTest.class.getClassLoader().getResource("frequencies/DPA1DPB1_Freqs.csv").toURI();
		Set<File> dpa1Dpb1Freqs = new HashSet<File>();
		dpa1Dpb1Freqs.add(new File(uri));

		List<DisequilibriumElement> disElements = HLAFrequenciesLoader.getInstance(dpa1Dpb1Freqs, null).getDisequilibriumElements(Locus.DPA1_DPB1_LOCI);
		assertTrue(disElements != null && disElements.size() > 0);
	}

	// Heap-footprint optimization (perf/large-frequency-file-heap): a real custom reference
	// file (the NMDP nine-locus release: ~6.4M rows, ~900K distinct haplotypes) has to stay
	// fully resident afterwards, so loadStandardReferenceData collapses each repeated per-locus
	// allele token onto ONE shared, immutable singleton list rather than allocating a fresh
	// ArrayList per row per locus. This is only safe because nothing downstream mutates a
	// reference row's allele list. This test locks in both halves of that: the sharing (a
	// regression that went back to per-row lists would still pass every other test, just use
	// more heap) and the immutability contract that makes the sharing safe.
	@Test
	public void testReferenceRowsShareImmutableCanonicalAlleleLists() throws Exception {
		String data =
				"AFA,HLA-C*07:01~HLA-B*08:01,0.0100000000,1\n" +
				"CAU,HLA-C*07:01~HLA-B*07:02,0.0200000000\n"; // second row deliberately has no rank column
		List<DisequilibriumElement> elements =
				HLAFrequenciesLoader.loadStandardReferenceData(new BufferedReader(new StringReader(data)));

		assertEquals(2, elements.size());

		List<String> firstRowC = null;
		List<String> secondRowC = null;
		for (DisequilibriumElement element : elements) {
			List<String> c = element.getHlaElement(Locus.HLA_C);
			assertEquals(1, c.size());
			assertEquals("HLA-C*07:01", c.get(0));
			if (firstRowC == null) {
				firstRowC = c;
			}
			else {
				secondRowC = c;
			}
		}

		// Both rows carry HLA-C*07:01 -- they must hand back the very same list instance.
		assertSame(firstRowC, secondRowC);
		// And that shared instance must not be mutable, or one row's edit would corrupt the other.
		final List<String> shared = firstRowC;
		assertThrows(UnsupportedOperationException.class, () -> shared.set(0, "HLA-C*99:99"));
	}
}
