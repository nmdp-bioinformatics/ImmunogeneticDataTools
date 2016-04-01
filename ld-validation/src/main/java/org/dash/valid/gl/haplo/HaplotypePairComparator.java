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
package org.dash.valid.gl.haplo;

import java.util.Comparator;

import org.dash.valid.race.RelativeFrequencyByRace;
import org.dash.valid.race.RelativeFrequencyByRaceComparator;

public class HaplotypePairComparator implements Comparator<HaplotypePair> {

	@Override
	public int compare(HaplotypePair element1, HaplotypePair element2) {
		int ret = 0;
		if (element1.equals(element2)) {
			return 0;
		}
		else if (element1.getPrimaryFrequency() != null && element2.getPrimaryFrequency() == null) {
			return -1;
		}
		else if (element1.getPrimaryFrequency() == null && element2.getPrimaryFrequency() != null) {
			return 1;
		}
		else if (element1.getPrimaryFrequency() != null && element1.getPrimaryFrequency() instanceof RelativeFrequencyByRace &&
				element2.getPrimaryFrequency() != null) {
			ret =  new RelativeFrequencyByRaceComparator().compare((RelativeFrequencyByRace) element1.getPrimaryFrequency(), 
					(RelativeFrequencyByRace) element2.getPrimaryFrequency());
		}
		
		if (ret != 0) {
			return ret;
		}
		
		// else sort alphabetically
		ret = element1.toString().compareTo(element2.toString());
		return ret;
	}
}
