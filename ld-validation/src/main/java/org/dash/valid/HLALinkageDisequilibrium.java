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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

/**
 * Detects <a href="http://en.wikipedia.org/wiki/Linkage_disequilibrium">linkage
 * disequilibrium</a> (LD) — the non-random association of alleles at two or more loci that
 * descend from a single, ancestral chromosome — for the specific set of HLA locus
 * combinations this project tracks (see {@link Linkages}), by matching candidate
 * haplotypes against reference haplotype frequency data (see
 * {@link org.dash.valid.freq.HLAFrequenciesLoader}).
 */
public class HLALinkageDisequilibrium {

    private static final Logger LOGGER = Logger.getLogger(HLALinkageDisequilibrium.class.getName());

	/**
	 * Detects linkage disequilibrium for an unphased genotype, enumerating every haplotype
	 * combination the genotype's own recorded ambiguity allows (via
	 * {@link LinkageDisequilibriumGenotypeList#getPossibleHaplotypes(EnumSet)}) and matching
	 * each against the reference frequency data for every tracked {@link Linkages} locus
	 * combination.
	 *
	 * @param glString the parsed genotype to detect linkages for
	 * @return a {@link Sample} wrapping the genotype and its {@link DetectedLinkageFindings}
	 */
	public static Sample hasLinkageDisequilibrium(LinkageDisequilibriumGenotypeList glString) {
		Sample sample = new Sample(glString);
		
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());
		
		Set<String> notCommon = GLStringUtilities.checkCommonWellDocumented(glString.getGLString());
		Set<String> notCIWD = GLStringUtilities.checkCommonIntermediateWellDocumented(glString.getGLString());
				
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
		findings.setNonCIWDAlleles(notCIWD);
		findings.setHladb(System.getProperty(GLStringConstants.HLADB_PROPERTY));
		
		sample.setFindings(findings);
		return sample;
	}
	
	/**
	 * Detects linkage disequilibrium for a genotype whose haplotypes are already known
	 * (e.g. already phased, via {@link org.dash.valid.gl.GLStringUtilities#buildHaplotypes}),
	 * instead of enumerating candidates from ambiguity. Each known haplotype is matched
	 * against reference data via {@link #enrichHaplotype}; a linkage is only reported for a
	 * given locus combination when exactly two of the known haplotypes both matched
	 * (a linked pair).
	 *
	 * @param glString the parsed genotype (used for CWD/CIWD checks and to build the result)
	 * @param knownHaplotypes the genotype's already-known haplotypes
	 * @return a {@link Sample} wrapping the genotype and its {@link DetectedLinkageFindings}
	 */
	public static Sample hasLinkageDisequilibrium(LinkageDisequilibriumGenotypeList glString, List<Haplotype> knownHaplotypes) {
		Set<HaplotypePair> linkedPairs = new HaplotypePairSet(new HaplotypePairComparator());

		Set<String> notCommon = GLStringUtilities.checkCommonWellDocumented(glString.getGLString());
		Set<String> notCIWD = GLStringUtilities.checkCommonIntermediateWellDocumented(glString.getGLString());
						
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
		findings.setNonCIWDAlleles(notCIWD);
		findings.setHladb(System.getProperty(GLStringConstants.HLADB_PROPERTY));
		
		sample.setFindings(findings);
		return sample;
	}

	/**
	 * Matches one already-known haplotype against reference frequency data for a single
	 * locus combination, returning a copy annotated with its {@link Haplotype#getLinkage()}
	 * if a match was found (or with no linkage set, if not). Matching is by
	 * {@link DisequilibriumElement#equals}, which compares at the G-group/ARS level rather
	 * than exact allele strings, so a haplotype carrying its own locus-level allele
	 * ambiguity can in principle match more than one distinct reference row; this method
	 * does not (currently) apply any explicit tie-break for that case the way
	 * {@link #hasLinkageDisequilibrium(LinkageDisequilibriumGenotypeList, List)}'s own
	 * candidate-enumeration path does — see the class-internal notes on
	 * {@code findLinkedPairs} for that mechanism.
	 *
	 * @param loci the locus combination to match against
	 * @param disequilibriumElements reference data for that locus combination
	 * @param haplotype the known haplotype to match
	 * @return a copy of {@code haplotype}, annotated with a linkage if one was found
	 */
	public static Haplotype enrichHaplotype(EnumSet<Locus> loci, List<DisequilibriumElement> disequilibriumElements, Haplotype haplotype) {
		MultiLocusHaplotype enrichedHaplotype = new MultiLocusHaplotype(new ConcurrentHashMap<Locus, List<String>>(haplotype.getAlleleMap()),
				new HashMap<Locus, Integer>(haplotype.getHaplotypeInstanceMap()), haplotype.getDrb345Homozygous());
		HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();

		for (Locus locus : enrichedHaplotype.getLoci()) {
			if (loci.contains(locus)) {
				hlaElementMap.put(locus, enrichedHaplotype.getAlleles(locus));
			}
			else {
				enrichedHaplotype.removeAlleles(locus);
			}
		}

		DisequilibriumElement element = new CoreDisequilibriumElement(hlaElementMap, enrichedHaplotype);

		// Was a manual List#contains()/indexOf()/subList() linear scan over the full (possibly
		// 900K+ row, for a real custom-uploaded frequency file) reference list, repeated once per
		// match found. DisequilibriumElementIndex finds the identical set of matches (equals()
		// itself is unchanged and still has the final say -- see its javadoc) without scanning
		// the whole list per lookup. Keeps the original's "last match found wins" behavior: the
		// loop below overwrites the linkage on every iteration, same as the old while-loop did.
		List<DisequilibriumElement> matches = DisequilibriumElementIndex.forElements(disequilibriumElements).findMatches(element);

		for (DisequilibriumElement match : matches) {
			DetectedDisequilibriumElement detectedElement = new DetectedDisequilibriumElement(match);
			detectedElement.setHaplotype(element.getHaplotype());
			enrichedHaplotype.setLinkage(detectedElement);
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

		// Was: Set<MultiLocusHaplotype> linkedHaplotypes = new HashSet<>(), added to via a bare
		// linkedHaplotypes.add(clonedHaplotype). Multiple raw allele combinations enumerated from
		// the genotype's own ambiguity can independently match the SAME reference disequilibrium
		// element (they all belong to the same G-group, and reference lookups are G-group/ARS
		// based, not exact-string), so more than one clonedHaplotype can carry the identical
		// G-group-level getHaplotypeString() -- that's the "value" -- while differing only in
		// getFullHaplotypeString() ("fullValue"). Since equals()/hashCode() are (correctly)
		// based on getHaplotypeString() alone, a plain HashSet.add() silently drops whichever
		// candidate it encounters second, and which one that is depends on HashSet iteration
		// order, which is not guaranteed stable across JVM runs -- confirmed via run-twice-diff.
		// The reference disequilibrium data is loaded with exactly one entry per unique G-group
		// string (see HLAFrequenciesLoader#loadStandardReferenceData), so any two candidates
		// tied at the G-group level necessarily matched that identical single reference entry --
		// the frequency/linkage evidence they carry is the same either way. Keying explicitly by
		// getHaplotypeString() lets us detect the tie and choose deterministically (smaller
		// fullValue wins) instead of leaving it to HashSet iteration order.
		Map<String, MultiLocusHaplotype> linkedHaplotypesByValue = new LinkedHashMap<String, MultiLocusHaplotype>();

		MultiLocusHaplotype clonedHaplotype = null;

		// disequilibriumElements can be huge (900K+ rows for a real custom-uploaded frequency
		// file); the index below is built once (and cached by list identity) rather than once
		// per possibleHaplotype the way the old List#contains()/indexOf() scan effectively was.
		DisequilibriumElementIndex index = DisequilibriumElementIndex.forElements(disequilibriumElements);

		for (MultiLocusHaplotype possibleHaplotype : glString.getPossibleHaplotypes(loci)) {
			HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();

			for (Locus locus : possibleHaplotype.getLoci()) {
				if (loci.contains(locus)) {

					hlaElementMap.put(locus, possibleHaplotype.getAlleles(locus));
				}
			}

			DisequilibriumElement element = new CoreDisequilibriumElement(hlaElementMap, possibleHaplotype);

			// Was a manual List#contains()/indexOf()/subList() linear scan finding matches one at
			// a time. index.findMatches() returns the identical set (equals() itself is
			// unchanged -- see DisequilibriumElementIndex's javadoc for why a naive string-keyed
			// Map isn't safe here) in the same list order, so this loop's tie-break behavior below
			// is unchanged from the original while-loop's.
			for (DisequilibriumElement match : index.findMatches(element)) {
				clonedHaplotype = new MultiLocusHaplotype(new ConcurrentHashMap<Locus, List<String>>(possibleHaplotype.getAlleleMap()), possibleHaplotype.getHaplotypeInstanceMap(), possibleHaplotype.getDrb345Homozygous());
				DetectedDisequilibriumElement detectedElement = new DetectedDisequilibriumElement(match);
				detectedElement.setHaplotype(element.getHaplotype());
				clonedHaplotype.setLinkage(detectedElement);

				String haplotypeValue = clonedHaplotype.getHaplotypeString();
				MultiLocusHaplotype existingHaplotype = linkedHaplotypesByValue.get(haplotypeValue);

				if (existingHaplotype == null) {
					linkedHaplotypesByValue.put(haplotypeValue, clonedHaplotype);
				}
				else if (clonedHaplotype.getFullHaplotypeString().compareTo(existingHaplotype.getFullHaplotypeString()) < 0) {
					LOGGER.fine("Multiple raw allele candidates tie at G-group " + haplotypeValue + ": keeping "
							+ clonedHaplotype.getFullHaplotypeString() + " over " + existingHaplotype.getFullHaplotypeString());
					linkedHaplotypesByValue.put(haplotypeValue, clonedHaplotype);
				}
			}
		}

		Set<MultiLocusHaplotype> linkedHaplotypes = new LinkedHashSet<MultiLocusHaplotype>(linkedHaplotypesByValue.values());

		// Was: a separate Set<DetectedDisequilibriumElement> detectedDisequilibriumElements =
		// new HashSet<>(), added to unconditionally inside the while loop above (once per
		// reference-row match, so once per tied candidate too), then merged into
		// findings.linkages -- a LinkageElementsSet (TreeSet) ordered by
		// DisequilibriumElementComparator, whose compare() starts with
		// DetectedDisequilibriumElement#equals(), which is true for any two matches of the
		// same reference row regardless of which raw-allele candidate matched it. That made
		// the TreeSet treat tied candidates as duplicates and silently keep whichever the
		// *source* HashSet happened to iterate first -- but DetectedDisequilibriumElement
		// overrides equals() without overriding hashCode(), so that HashSet's iteration order
		// was governed by default (identity-based) hashCode(), which is JVM-launch-random.
		// Confirmed via instrumented tracing: the in-memory linkedHaplotypesByValue tie-break
		// above was already fully deterministic and identical across JVM launches, yet the
		// final report still varied -- because this separate, parallel structure was never
		// touched by that fix and re-introduced the same class of bug one level up. Deriving
		// the linkages directly from linkedHaplotypesByValue's already-deterministic winners
		// (each winning clonedHaplotype's own linkage is exactly the DetectedDisequilibriumElement
		// that should represent its reference row) sidesteps the broken equals()/hashCode()
		// pair entirely, and keeps the <linkage> report section consistent with which
		// candidate's fullValue is reported as the haplotype pair.
		Set<DetectedDisequilibriumElement> detectedDisequilibriumElements = new LinkedHashSet<DetectedDisequilibriumElement>();
		for (MultiLocusHaplotype winner : linkedHaplotypes) {
			detectedDisequilibriumElements.add(winner.getLinkage());
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
