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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.gl.haplo.HaplotypePairComparator;
import org.dash.valid.gl.haplo.HaplotypePairSet;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.dash.valid.report.DetectedDisequilibriumElement;
import org.dash.valid.report.DetectedLinkageFindings;

/*
 * Linkage disequilibrium
 * 
 * Non-random association of alleles at two or more loci that descend from a single,
 * ancestral chromosome
 * 
 * http://en.wikipedia.org/wiki/Linkage_disequilibrium
 * 
 * This class leverages a specific set of linkage disequilibrium associations relevant in the context
 * of HLA (http://en.wikipedia.org/wiki/Human_leukocyte_antigen) and immunogenetics.
 * 
 */

public class HLALinkageDisequilibrium {

    private static final Logger LOGGER = Logger.getLogger(HLALinkageDisequilibrium.class.getName());
			
	public static Sample hasLinkageDisequilibrium(LinkageDisequilibriumGenotypeList glString) {	
		Sample sample = new Sample(glString);
		
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
		
		Set<String> notCommon = GLStringUtilities.checkCommonWellDocumented(glString.getGLString());
				
		DetectedLinkageFindings findings = new DetectedLinkageFindings(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
		Set<Linkages> linkages = LinkagesLoader.getInstance().getLinkages();
		if (linkages == null) {
			sample.setFindings(findings);
			return sample;
		}
						
		for (Linkages linkage : linkages) {
			EnumSet<Locus> loci = linkage.getLoci();
			findings.addFindingSought(loci);
			List<DisequilibriumElement> disequilibriumElements = HLAFrequenciesLoader.getInstance().getDisequilibriumElements(loci);
			
			linkedPairs.addAll(findLinkedPairs(glString, loci, disequilibriumElements, findings));
		}		
		
		LOGGER.info(linkedPairs.size() + " linkedPairs");
		
		findings.setGenotypeList(glString);
		findings.setLinkedPairs(linkedPairs);
		findings.setNonCWDAlleles(notCommon);
		findings.setHladb(System.getProperty(GLStringConstants.HLADB_PROPERTY));
		
		sample.setFindings(findings);
		return sample;
	}
	
	public static Sample hasLinkageDisequilibrium(LinkageDisequilibriumGenotypeList glString, List<Haplotype> knownHaplotypes) {		
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());

		Set<String> notCommon = GLStringUtilities.checkCommonWellDocumented(glString.getGLString());
						
		Sample sample = new Sample(glString);
		
		DetectedLinkageFindings findings = new DetectedLinkageFindings(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
		Set<Linkages> linkages = LinkagesLoader.getInstance().getLinkages();
		if (linkages == null) {
			sample.setFindings(findings);
			return sample;
		}
						
		for (Linkages linkage : linkages) {
			EnumSet<Locus> loci = linkage.getLoci();
			findings.addFindingSought(loci);
			List<DisequilibriumElement> disequilibriumElements = HLAFrequenciesLoader.getInstance().getDisequilibriumElements(loci);
			List<Haplotype> enrichedHaplotypes = new ArrayList<Haplotype>();
									
			for (Haplotype haplotype : knownHaplotypes) {
				Haplotype enrichedHaplotype = enrichHaplotype(loci, disequilibriumElements, haplotype);

				if (enrichedHaplotype.getLinkage() != null) {
					findings.addLinkage(enrichedHaplotype.getLinkage());
					enrichedHaplotypes.add(enrichedHaplotype);
				}
			}
			
			if (enrichedHaplotypes.size() == 2) {
				linkedPairs.add(new HaplotypePair(enrichedHaplotypes.get(0), enrichedHaplotypes.get(1)));
			}
		}		
		
		LOGGER.info(linkedPairs.size() + " linkedPairs");
		
		findings.setGenotypeList(glString);
		findings.setLinkedPairs(linkedPairs);
		findings.setNonCWDAlleles(notCommon);
		findings.setHladb(System.getProperty(GLStringConstants.HLADB_PROPERTY));
		
		sample.setFindings(findings);
		return sample;
	}

	public static Haplotype enrichHaplotype(EnumSet<Locus> loci, List<DisequilibriumElement> disequilibriumElements, Haplotype haplotype) {
		MultiLocusHaplotype enrichedHaplotype = new MultiLocusHaplotype(new ConcurrentHashMap<Locus, List<String>>(haplotype.getAlleleMap()), 
				new HashMap<Locus, Integer>(haplotype.getHaplotypeInstanceMap()), haplotype.getDrb345Homozygous());
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
		List<DisequilibriumElement> shortenedList = new ArrayList<DisequilibriumElement>(disequilibriumElements);

		for (Locus locus : enrichedHaplotype.getLoci()) {
			if (loci.contains(locus)) {
				hlaElementMap.put(locus, enrichedHaplotype.getAlleles(locus));
			}
			else {
				enrichedHaplotype.removeAlleles(locus);
			}
		}
		
		DisequilibriumElement element = new CoreDisequilibriumElement(hlaElementMap, enrichedHaplotype);
		DetectedDisequilibriumElement detectedElement = null;
					
		while (shortenedList.contains(element)) {
			int index = shortenedList.indexOf(element);
			detectedElement = new DetectedDisequilibriumElement(shortenedList.get(index));
			detectedElement.setHaplotype(element.getHaplotype());
			enrichedHaplotype.setLinkage(detectedElement);
			
			shortenedList = shortenedList.subList(index + 1, shortenedList.size());
		}
		
		enrichedHaplotype.setSequence(haplotype.getSequence());
		
		return enrichedHaplotype;
	}
	
	private static Set<HaplotypePair> findLinkedPairs(
			LinkageDisequilibriumGenotypeList glString,
			EnumSet<Locus> loci,
			List<DisequilibriumElement> disequilibriumElements,
			DetectedLinkageFindings findings) {
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());

		Set<MultiLocusHaplotype> linkedHaplotypes = new HashSet<MultiLocusHaplotype>();
		
		Set<DetectedDisequilibriumElement> detectedDisequilibriumElements = new HashSet<DetectedDisequilibriumElement>();
		
		MultiLocusHaplotype clonedHaplotype = null;
				
		for (MultiLocusHaplotype possibleHaplotype : glString.getPossibleHaplotypes(loci)) {
			List<DisequilibriumElement> shortenedList = new ArrayList<DisequilibriumElement>(disequilibriumElements);

			HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();

			for (Locus locus : possibleHaplotype.getLoci()) {
				if (loci.contains(locus)) {
					
					hlaElementMap.put(locus, possibleHaplotype.getAlleles(locus));
				}
			}
			
			DisequilibriumElement element = new CoreDisequilibriumElement(hlaElementMap, possibleHaplotype);
			DetectedDisequilibriumElement detectedElement = null;
						
			while (shortenedList.contains(element)) {
				int index = shortenedList.indexOf(element);
				clonedHaplotype = new MultiLocusHaplotype(new ConcurrentHashMap<Locus, List<String>>(possibleHaplotype.getAlleleMap()), possibleHaplotype.getHaplotypeInstanceMap(), possibleHaplotype.getDrb345Homozygous());
				detectedElement = new DetectedDisequilibriumElement(shortenedList.get(index));
				detectedElement.setHaplotype(element.getHaplotype());
				clonedHaplotype.setLinkage(detectedElement);
				linkedHaplotypes.add(clonedHaplotype);
				detectedDisequilibriumElements.add(detectedElement);
				
				shortenedList = shortenedList.subList(index + 1, shortenedList.size());
			}
		}
		
		findings.addLinkages(detectedDisequilibriumElements);
		
		for (Haplotype haplotype1 : linkedHaplotypes) {	
			for (Haplotype haplotype2 : linkedHaplotypes) {
				int idx = 0;
				for (Locus locus : loci) {
					if ((!glString.hasHomozygous(locus) && haplotype1.getHaplotypeInstance(locus) == haplotype2.getHaplotypeInstance(locus))) {
						// move on to next haplotype2
						break;
					}
					
					if (idx == loci.size() - 1) {
						linkedPairs.add(new HaplotypePair(haplotype1, haplotype2));
					}
					
					idx++;
				}
			}
		}
		
		return linkedPairs;
	}
}
