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
package org.dash.valid.race;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FrequencyByRaceComparatorTest {

	@Test
	public void testFrequencyByRaceComparator() {
		FrequencyByRaceComparator comparator = new FrequencyByRaceComparator();
		
		FrequencyByRace freq1 = new FrequencyByRace(.3, "2", BroadRace.CAU.toString());
		FrequencyByRace freq2 = new FrequencyByRace(.4, "3", BroadRace.API.toString());
		FrequencyByRace freq3 = new FrequencyByRace(.5, "4", "AAFA");
		
		assertTrue(comparator.compare(freq1, freq2) > 0);
		assertTrue(comparator.compare(freq2, freq3) < 0);
		assertTrue(comparator.compare(freq1, freq3) < 0);
	}
}
