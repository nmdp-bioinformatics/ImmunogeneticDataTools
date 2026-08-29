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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.dash.valid.ars.AntigenRecognitionSiteLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;

/**
 * Accelerates {@link DisequilibriumElement#equals(Object)} lookups against a (potentially very
 * large, e.g. 900K+ rows for a real custom-uploaded frequency file) reference list, without
 * changing what counts as a match.
 * <p>
 * {@code equals()}'s notion of "match" per locus is the OR of two independent, non-exact-string
 * comparisons -- {@link GLStringUtilities#fieldLevelComparison} (colon-field prefix truncation,
 * to whichever side has fewer fields) and {@link GLStringUtilities#checkAntigenRecognitionSite}
 * (ARS/G-group membership) -- so a plain {@code Map<String, DisequilibriumElement>} keyed by
 * allele string would silently miss real matches. This class instead builds, per locus:
 * <ul>
 * <li>a field-prefix index, split into two maps so a query never has to consult a bucket sized
 *     by "every row that happens to share a short prefix" -- {@code sameLengthIndex} holds each
 *     row once, keyed by its own exact field count and full string; {@code longerRowPrefixIndex}
 *     holds a row's prefix at length K only for K strictly less than the row's own length (i.e.
 *     only where it's actually needed, to satisfy a query shorter than the row itself). A query
 *     of length Lq checks {@code sameLengthIndex} at every K from 1 to Lq (catching every row
 *     whose own length is <= Lq) plus {@code longerRowPrefixIndex} at exactly K=Lq (catching
 *     rows longer than the query) -- never a generic "everyone sharing my first field" bucket;
 *     and</li>
 * <li>an ARS-code index, inverting {@link AntigenRecognitionSiteLoader}'s existing map against
 *     the reference rows actually present.</li>
 * </ul>
 * A query's per-locus candidate sets (built from these indexes) are intersected across the
 * query's own locus set, exactly mirroring {@code equals()}'s per-locus AND -- including the
 * DRB345 homozygous dash/{@code NNNN} special case, precomputed once since it only ever depends
 * on the reference data, not the query. The surviving candidates are then confirmed with the
 * real {@code equals()} before being returned, so an indexing mistake can only cost speed, never
 * silently produce a wrong match.
 * <p>
 * Built once per distinct reference list (identity-cached, not equals()/hashCode()-cached --
 * {@code DisequilibriumElement} intentionally has no {@code hashCode()}, see its {@code equals()}
 * javadoc) and reused across every candidate haplotype and every genotype checked against that
 * same list within a run. {@link org.dash.valid.freq.HLAFrequenciesLoader#reset()} always
 * installs a brand new list rather than mutating the old one in place, so a plain identity cache
 * here needs no explicit invalidation -- a reset simply makes the old cache entry unreachable.
 */
class DisequilibriumElementIndex {
	private static final Logger LOGGER = Logger.getLogger(DisequilibriumElementIndex.class.getName());

	private static final Map<List<DisequilibriumElement>, DisequilibriumElementIndex> CACHE =
			new IdentityHashMap<List<DisequilibriumElement>, DisequilibriumElementIndex>();

	private final List<DisequilibriumElement> elements;

	// locus -> exact field count -> row's own full string at that field count -> row indices.
	// Each row appears exactly once here, at its own natural length -- unlike a single map
	// covering every truncation depth, this never lets a short, widely-shared prefix (e.g. every
	// 4-field allele's first field) sweep in rows that don't actually belong at that length.
	private final Map<Locus, Map<Integer, Map<String, List<Integer>>>> sameLengthIndex =
			new HashMap<Locus, Map<Integer, Map<String, List<Integer>>>>();

	// locus -> K -> a row's own first-K-fields -> row indices, but ONLY for rows whose own length
	// is strictly greater than K. Exists so a query *shorter* than some reference row can still
	// find it (comparisonLength == the query's own length, the row truncates down to match) --
	// looked up at exactly K == the query's own length, not swept in in bulk the way
	// sameLengthIndex's per-length buckets are.
	private final Map<Locus, Map<Integer, Map<String, List<Integer>>>> longerRowPrefixIndex =
			new HashMap<Locus, Map<Integer, Map<String, List<Integer>>>>();

	private final Map<Locus, Map<String, List<Integer>>> arsIndex =
			new HashMap<Locus, Map<String, List<Integer>>>();

	// Rows whose DRB345 allele list contains the "no DRB345 gene" placeholder (DASH or NNNN) --
	// the only rows the DRB345-homozygous special case in equals() can ever bypass a match for.
	// Reference DisequilibriumElements never carry a Haplotype of their own (HLAFrequenciesLoader
	// never calls setHaplotype() when building them -- confirmed by reading it), so the special
	// case's *other* clause (checking element1's own haplotype) never applies to reference rows;
	// only the query's own getDrb345Homozygous() flag matters here.
	private final Set<Integer> drb345PlaceholderRows = new LinkedHashSet<Integer>();

	private DisequilibriumElementIndex(List<DisequilibriumElement> elements) {
		this.elements = elements;
		build();
	}

	static synchronized DisequilibriumElementIndex forElements(List<DisequilibriumElement> elements) {
		DisequilibriumElementIndex index = CACHE.get(elements);

		if (index == null) {
			index = new DisequilibriumElementIndex(elements);
			CACHE.put(elements, index);
		}

		return index;
	}

	private void build() {
		HashMap<String, HashSet<String>> arsMap;

		try {
			arsMap = AntigenRecognitionSiteLoader.getInstance().getArsMap();
		}
		catch (IOException | InvalidFormatException e) {
			LOGGER.warning("Could not load ars data while building DisequilibriumElementIndex.");
			arsMap = new HashMap<String, HashSet<String>>();
		}

		for (int rowIndex = 0; rowIndex < elements.size(); rowIndex++) {
			DisequilibriumElement row = elements.get(rowIndex);

			for (Locus locus : row.getLoci()) {
				List<String> alleles = row.getHlaElement(locus);

				if (alleles == null) {
					continue;
				}

				for (String allele : alleles) {
					indexFieldPrefixes(locus, allele, rowIndex);
					indexArsCodes(arsMap, locus, allele, rowIndex);

					if (Locus.isDRB345(locus) && (GLStringConstants.DASH.equals(allele) || GLStringConstants.NNNN.equals(allele))) {
						drb345PlaceholderRows.add(rowIndex);
					}
				}
			}
		}
	}

	private void indexFieldPrefixes(Locus locus, String allele, int rowIndex) {
		String[] fields = allele.split(GLStringUtilities.COLON);
		int ownLength = fields.length;

		StringBuilder prefix = new StringBuilder();
		for (int fieldCount = 1; fieldCount <= ownLength; fieldCount++) {
			if (fieldCount > 1) {
				prefix.append(GLStringUtilities.COLON);
			}
			prefix.append(fields[fieldCount - 1]);

			if (fieldCount == ownLength) {
				addIndexEntry(bucketFor(sameLengthIndex, locus, fieldCount), prefix.toString(), rowIndex);
			}
			else {
				addIndexEntry(bucketFor(longerRowPrefixIndex, locus, fieldCount), prefix.toString(), rowIndex);
			}
		}
	}

	private static Map<String, List<Integer>> bucketFor(Map<Locus, Map<Integer, Map<String, List<Integer>>>> index, Locus locus, int fieldCount) {
		Map<Integer, Map<String, List<Integer>>> byLength = index.get(locus);
		if (byLength == null) {
			byLength = new HashMap<Integer, Map<String, List<Integer>>>();
			index.put(locus, byLength);
		}

		Map<String, List<Integer>> byPrefix = byLength.get(fieldCount);
		if (byPrefix == null) {
			byPrefix = new HashMap<String, List<Integer>>();
			byLength.put(fieldCount, byPrefix);
		}

		return byPrefix;
	}

	// Read-only counterpart to bucketFor() -- never creates a bucket, just looks one up.
	private static List<Integer> bucketAt(Map<Integer, Map<String, List<Integer>>> byLength, int fieldCount, String key) {
		Map<String, List<Integer>> byPrefix = byLength.get(fieldCount);
		return byPrefix == null ? null : byPrefix.get(key);
	}

	private void indexArsCodes(HashMap<String, HashSet<String>> arsMap, Locus locus, String allele, int rowIndex) {
		HashSet<String> codes = arsMap.get(allele);

		if (codes == null) {
			return;
		}

		Map<String, List<Integer>> byCode = arsIndex.get(locus);
		if (byCode == null) {
			byCode = new HashMap<String, List<Integer>>();
			arsIndex.put(locus, byCode);
		}

		for (String code : codes) {
			addIndexEntry(byCode, code, rowIndex);
		}
	}

	private static void addIndexEntry(Map<String, List<Integer>> index, String key, int rowIndex) {
		List<Integer> rowIndices = index.get(key);
		if (rowIndices == null) {
			rowIndices = new ArrayList<Integer>();
			index.put(key, rowIndices);
		}
		// Reference data is loaded with one entry per unique G-group string (see
		// HLAFrequenciesLoader#loadStandardReferenceData), so in practice this list rarely grows
		// past one -- but nothing here assumes that.
		if (rowIndices.isEmpty() || !rowIndices.get(rowIndices.size() - 1).equals(rowIndex)) {
			rowIndices.add(rowIndex);
		}
	}

	/**
	 * All elements of the original list that {@code query.equals(element)} would return true
	 * for, in the original list's order -- exactly what a forward scan with
	 * {@code List#indexOf}/{@code List#subList} would have found, just without doing one.
	 * Every candidate the index surfaces is confirmed with the real {@code equals()} before
	 * being included.
	 */
	List<DisequilibriumElement> findMatches(DisequilibriumElement query) {
		Set<Integer> candidateIndices = null;

		for (Locus locus : query.getLoci()) {
			Set<Integer> locusMatches = matchesAtLocus(query, locus);

			if (candidateIndices == null) {
				candidateIndices = locusMatches;
			}
			else {
				candidateIndices.retainAll(locusMatches);
			}

			if (candidateIndices.isEmpty()) {
				return Collections.emptyList();
			}
		}

		if (candidateIndices == null) {
			return Collections.emptyList();
		}

		List<Integer> sortedIndices = new ArrayList<Integer>(candidateIndices);
		Collections.sort(sortedIndices);

		List<DisequilibriumElement> verified = new ArrayList<DisequilibriumElement>();
		for (Integer rowIndex : sortedIndices) {
			DisequilibriumElement candidate = elements.get(rowIndex);
			// Defense in depth: the index is only ever used to narrow down which rows are worth
			// checking. The real equals() -- the actual, unmodified source of truth -- still
			// decides whether each one really matches.
			if (query.equals(candidate)) {
				verified.add(candidate);
			}
		}

		return verified;
	}

	private Set<Integer> matchesAtLocus(DisequilibriumElement query, Locus locus) {
		Set<Integer> locusMatches = new LinkedHashSet<Integer>();
		List<String> queryAlleles = query.getHlaElement(locus);

		if (queryAlleles != null) {
			Map<Integer, Map<String, List<Integer>>> sameLengthByLength = sameLengthIndex.get(locus);
			Map<Integer, Map<String, List<Integer>>> longerByLength = longerRowPrefixIndex.get(locus);
			Map<String, List<Integer>> byArsCode = arsIndex.get(locus);

			for (String queryAllele : queryAlleles) {
				String[] fields = queryAllele.split(GLStringUtilities.COLON);
				int queryLength = fields.length;

				if (sameLengthByLength != null) {
					StringBuilder prefix = new StringBuilder();
					for (int fieldCount = 1; fieldCount <= queryLength; fieldCount++) {
						if (fieldCount > 1) {
							prefix.append(GLStringUtilities.COLON);
						}
						prefix.append(fields[fieldCount - 1]);

						// Rows whose own natural length is exactly fieldCount -- comparisonLength
						// == fieldCount == that row's length, so its full string must equal this
						// prefix. Checked at every fieldCount up to the query's own length, since
						// any of those shorter-or-equal natural lengths could be the shorter side.
						Map<String, List<Integer>> byPrefix = sameLengthByLength.get(fieldCount);
						if (byPrefix != null) {
							List<Integer> hits = byPrefix.get(prefix.toString());
							if (hits != null) {
								locusMatches.addAll(hits);
							}
						}
					}
				}

				if (longerByLength != null) {
					// Rows longer than the query -- comparisonLength == the query's own length,
					// so only rows whose prefix at exactly that length equals the query's full
					// (untruncated) string are candidates. A single lookup, not swept in via the
					// loop above, since a shorter-than-query fieldCount here would only match
					// rows that are also longer than *that* fieldCount but no claim is made about
					// their remaining fields matching the query at all.
					List<Integer> hits = bucketAt(longerByLength, queryLength, queryAllele);
					if (hits != null) {
						locusMatches.addAll(hits);
					}
				}

				if (byArsCode != null) {
					String matchedValue = GLStringUtilities.convertToProteinLevel(queryAllele);
					if (matchedValue != null) {
						List<Integer> hits = byArsCode.get(matchedValue);
						if (hits != null) {
							locusMatches.addAll(hits);
						}
					}
				}
			}
		}

		if (Locus.isDRB345(locus) && query.getHaplotype() != null && query.getHaplotype().getDrb345Homozygous()) {
			locusMatches.addAll(drb345PlaceholderRows);
		}

		return locusMatches;
	}
}
