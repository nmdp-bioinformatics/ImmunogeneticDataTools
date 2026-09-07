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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

// Issues #9/#46: Locus/Linkages already recognized DQA1, DPB1, and DPA1 individually -- what
// was missing was a named combination covering them, which is what HLAFrequenciesLoader's
// auto-detection (see HLAFrequenciesLoaderTest) actually depends on. These tests exercise the
// enum-level lookup logic in isolation; the integration-level regression coverage (a real
// reference file loaded end to end) lives in HLAFrequenciesLoaderTest.
public class LinkagesTest {

	@Test
	public void testNineLocusRoundTripsThroughLocusLookup() {
		assertEquals(Locus.NINE_LOCUS, Locus.lookup(Locus.NINE_LOCUS));
	}

	@Test
	public void testDpa1Dpb1RoundTripsThroughLocusLookup() {
		assertEquals(Locus.DPA1_DPB1_LOCI, Locus.lookup(Locus.DPA1_DPB1_LOCI));
	}

	@Test
	public void testLinkagesLookupFindsNineLocus() {
		Set<Linkages> found = Linkages.lookup(EnumSet.copyOf(Locus.NINE_LOCUS));
		assertTrue(found.contains(Linkages.NINE_LOCUS), found.toString());
	}

	@Test
	public void testLinkagesLookupFindsDpa1Dpb1() {
		Set<Linkages> found = Linkages.lookup(EnumSet.copyOf(Locus.DPA1_DPB1_LOCI));
		assertTrue(found.contains(Linkages.DPA1_DPB1), found.toString());
	}

	// Regression: adding NINE_LOCUS/DPA1_DPB1 must not change how the pre-existing named
	// combinations resolve.
	@Test
	public void testExistingCombinationsStillResolveUnchanged() {
		assertEquals(Locus.FIVE_LOCUS, Locus.lookup(Locus.FIVE_LOCUS));
		assertEquals(Locus.SIX_LOCUS, Locus.lookup(Locus.SIX_LOCUS));
		assertTrue(Linkages.lookup(EnumSet.copyOf(Locus.FIVE_LOCUS)).contains(Linkages.FIVE_LOCUS));
		assertTrue(Linkages.lookup(EnumSet.copyOf(Locus.SIX_LOCUS)).contains(Linkages.SIX_LOCUS));
	}

	// Documents the actual (still-unsupported) behavior for a combination that matches none of
	// the named sets: Locus#lookup(Set) returns null, same as before this change. This is the
	// exact condition that used to reach Linkages#lookup(EnumSet) as a null argument and throw
	// a NullPointerException -- callers must still not pass a null loci EnumSet into
	// Linkages.lookup() directly, this test only confirms Locus.lookup() itself is unchanged
	// for a genuinely unrecognized combination.
	@Test
	public void testUnrecognizedCombinationStillReturnsNull() {
		EnumSet<Locus> notANamedCombination = EnumSet.of(Locus.HLA_A, Locus.HLA_DQA1);
		assertNull(Locus.lookup(notANamedCombination));
	}
}
