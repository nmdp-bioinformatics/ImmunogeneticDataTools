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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
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
}
