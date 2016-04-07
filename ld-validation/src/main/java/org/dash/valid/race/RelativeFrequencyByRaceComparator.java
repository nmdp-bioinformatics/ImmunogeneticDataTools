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

import java.util.Comparator;


public class RelativeFrequencyByRaceComparator implements Comparator<RelativeFrequencyByRace> {

	@Override
	public int compare(RelativeFrequencyByRace o1, RelativeFrequencyByRace o2) {
		if (BroadRace.contains(o1.getRace()) && !BroadRace.contains(o2.getRace())) {
			return -1;
		}
		else if (BroadRace.contains(o2.getRace()) && !BroadRace.contains(o1.getRace())) {
			return 1;
		}
		
		if (o2.getRace().equals(o1.getRace()) && o2.getRelativeFrequency() != null && o1.getRelativeFrequency() != null) {
			return o2.getRelativeFrequency().compareTo(o1.getRelativeFrequency());
		}
		
		return o1.toString().compareTo(o2.toString());
	}
}
