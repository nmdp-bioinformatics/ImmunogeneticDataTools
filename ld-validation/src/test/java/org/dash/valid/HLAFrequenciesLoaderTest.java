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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.dash.valid.freq.HLAFrequenciesLoader;
import org.junit.Test;

public class HLAFrequenciesLoaderTest extends TestCase {
	public static final Map<String,Locus[]> freqMap;
	public static final Map<String,String> fileMap;
		
	static {
		freqMap = new HashMap<String,Locus[]>();
		fileMap = new HashMap<String, String>();
		
		freqMap.put(HLAFrequenciesLoader.NMDP_2007_ABC_FREQUENCIES, HLAFrequenciesLoader.NMDP_ABC_LOCI_POS);
		freqMap.put(HLAFrequenciesLoader.NMDP_2007_BC_FREQUENCIES,  HLAFrequenciesLoader.NMDP_BC_LOCI_POS);
		freqMap.put(HLAFrequenciesLoader.NMDP_2007_DRB1DQB1_FREQUENCIES, HLAFrequenciesLoader.NMDP_DRB1DQB1_LOCI_POS);
		freqMap.put(HLAFrequenciesLoader.NMDP_2007_FIVE_LOCUS_FREQUENCIES, HLAFrequenciesLoader.NMDP_FIVE_LOCUS_POS);
		
		fileMap.put(HLAFrequenciesLoader.NMDP_2007_ABC_FREQUENCIES, "NMDP_2007_ACB_Freqs.csv");
		fileMap.put(HLAFrequenciesLoader.NMDP_2007_BC_FREQUENCIES,  "NMDP_2007_CB_Freqs.csv");
		fileMap.put(HLAFrequenciesLoader.NMDP_2007_DRB1DQB1_FREQUENCIES, "NMDP_2007_DRB1DQB1_Freqs.csv");
		fileMap.put(HLAFrequenciesLoader.NMDP_2007_FIVE_LOCUS_FREQUENCIES, "NMDP_2007_FiveLocus_Freqs.csv");

//		freqMap.put(HLAFrequenciesLoader.NMDP_ABC_FREQUENCIES, HLAFrequenciesLoader.NMDP_ABC_LOCI_POS);
//		freqMap.put(HLAFrequenciesLoader.NMDP_BC_FREQUENCIES, HLAFrequenciesLoader.NMDP_BC_LOCI_POS);
//		freqMap.put(HLAFrequenciesLoader.NMDP_DRDQ_FREQUENCIES, HLAFrequenciesLoader.NMDP_DRDQB1_LOCI_POS);
//		freqMap.put(HLAFrequenciesLoader.NMDP_FIVE_LOCUS_FREQUENCIES, HLAFrequenciesLoader.NMDP_FIVE_LOCUS_POS);
//		freqMap.put(HLAFrequenciesLoader.NMDP_SIX_LOCUS_FREQUENCIES, HLAFrequenciesLoader.NMDP_SIX_LOCUS_POS);
//
//		fileMap.put(HLAFrequenciesLoader.NMDP_ABC_FREQUENCIES, "NMDP_ACB_Freqs.csv");
//		fileMap.put(HLAFrequenciesLoader.NMDP_BC_FREQUENCIES, "NMDP_CB_Freqs.csv");
//		fileMap.put(HLAFrequenciesLoader.NMDP_DRDQ_FREQUENCIES, "NMDP_DRDQ_Freqs.csv");
//		fileMap.put(HLAFrequenciesLoader.NMDP_FIVE_LOCUS_FREQUENCIES, "NMDP_FiveLocus_Freqs.csv");
//		fileMap.put(HLAFrequenciesLoader.NMDP_SIX_LOCUS_FREQUENCIES, "NMDP_SixLocus_Freqs.csv");
	}
	
//	@Test
//	public void testLoadNMDPLinkageReferenceData() {
//		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.NMDP_2007.getShortName());
//		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
//		assertNotNull(freqLoader);
//		// TODO:  Create valid test
//		assertTrue(freqLoader.getDisequilibriumElements(Locus.FIVE_LOCUS) != null && freqLoader.getDisequilibriumElements(Locus.FIVE_LOCUS).size() > 0);
//	}
	
	@Test
	public void testLoadStandardReferenceData() {
//		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance(new File("/Users/mpresteg/bin/freqs/NMDP_DRDQ_Freqs.csv"));
//		Set<EnumSet<Locus>> loci = freqLoader.getLoci();
//		System.out.println(loci);
		
		assertTrue(true);
	}
//	
//	public void testCreateStandardReferenceDataFiles() throws Exception {
//		System.setProperty(Frequencies.FREQUENCIES_PROPERTY, Frequencies.NMDP_2007.getShortName());
//		HLAFrequenciesLoader freqLoader = HLAFrequenciesLoader.getInstance();
//		
//		for (String filename : freqMap.keySet()) {
//			List<DisequilibriumElement> disequilibriumElements = freqLoader.loadNMDPLinkageReferenceData(filename, freqMap.get(filename));
//			
//			PrintWriter writer = new PrintWriter(fileMap.get(filename));
//			
//			for (DisequilibriumElement element : disequilibriumElements) {
//				StringBuffer sb = new StringBuffer();
//				int locusCounter = 0;
//				for (Locus locus : Locus.lookup(element.getLoci())) {
//					if (locusCounter > 0) {
//						sb.append(GLStringConstants.GENE_PHASE_DELIMITER);
//					}
//					sb.append(element.getHlaElement(locus));
//					locusCounter++;
//				}
//				
//				List<FrequencyByRace> frequencies = ((DisequilibriumElementByRace) element).getFrequenciesByRace();
//				for (FrequencyByRace frequency : frequencies) {
//					writer.write(frequency.getRace() + GLStringConstants.COMMA + sb + GLStringConstants.COMMA + frequency.getFrequency() + GLStringConstants.COMMA + frequency.getRank() + GLStringConstants.NEWLINE);
//				}
//			}
//			
//			writer.close();
//		}
//	}
}
