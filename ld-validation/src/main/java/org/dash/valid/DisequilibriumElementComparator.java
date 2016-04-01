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

import java.util.Comparator;

import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.report.DetectedDisequilibriumElement;

public class DisequilibriumElementComparator implements Comparator<DetectedDisequilibriumElement> {

	@Override
	public int compare(DetectedDisequilibriumElement element1, DetectedDisequilibriumElement element2) {
		int ret;
		
		if (element1.equals(element2)) {
			return 0;
		}
		else if (element1.getDisequilibriumElement() instanceof DisequilibriumElementByRace && element2.getDisequilibriumElement() instanceof DisequilibriumElementByRace) {
			// those with more linkages should sort first
			ret = ((DisequilibriumElementByRace) element2.getDisequilibriumElement()).getFrequenciesByRace().size() - 
					((DisequilibriumElementByRace) element1.getDisequilibriumElement()).getFrequenciesByRace().size();
			if (ret != 0) {
				return ret;
			}
		}
		
		// else sort alphabetically
		ret = element1.toString().compareTo(element2.toString());	
		return ret;
	}
}
