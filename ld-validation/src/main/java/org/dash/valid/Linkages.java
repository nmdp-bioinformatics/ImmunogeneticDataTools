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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.freq.Frequencies;

public enum Linkages {
	A_B_C ("abc", Locus.A_B_C_LOCI), 
	B_C ("bc", Locus.B_C_LOCI), 
	DRB_DQB ("drb_dqb", Locus.DRB_DQB_LOCI), 
	DRB_DQ ("drb_dq", Locus.DRB_DQ_LOCI),
	DRB1_DQB1 ("drb1_dqb1", Locus.DRB1_DQB1_LOCI),
	FIVE_LOCUS ("five_loc", Locus.FIVE_LOCUS);
	
	private String shortName;
	private EnumSet<Locus> loci;
	
	public static final String LINKAGES_PROPERTY = "org.dash.linkages";
	
	private static final Logger LOGGER = Logger.getLogger(Linkages.class.getName());
	
	private Linkages(String shortName, EnumSet<Locus> loci) {
		this.shortName = shortName;
		this.loci = loci;
	}
	
	public EnumSet<Locus> getLoci() {
		return this.loci;
	}
	
	public String getShortName() {
		return this.shortName;
	}
	
	private static EnumSet<Linkages> getDefault() {
		if (Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY)).equals(Frequencies.NMDP)) {
			return EnumSet.of(Linkages.FIVE_LOCUS);
		}
		else if (Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY)).equals(Frequencies.NMDP_2007)) {
			return EnumSet.of(Linkages.FIVE_LOCUS);
		}
		else {
			return EnumSet.of(Linkages.B_C, Linkages.DRB_DQ);
		}
	}
	
	public static Set<Linkages> lookup(Set<String> shortNames) {
		Set<Linkages> set = new HashSet<Linkages>();
		for (Linkages linkages : values()) {
			if (shortNames.contains(linkages.getShortName())) {
				set.add(linkages);
			}
		}
		
		if (set.size() == 0) {
			LOGGER.warning("None of the specified linkages: " + shortNames + " are supported.  Defaulting to : " + getDefault());

			set = Linkages.getDefault();
		}
		
		return set;
	}
}
