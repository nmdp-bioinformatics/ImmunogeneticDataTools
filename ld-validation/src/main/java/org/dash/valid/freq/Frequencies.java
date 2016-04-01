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
package org.dash.valid.freq;

import java.util.logging.Logger;

public enum Frequencies {
	NMDP ("nmdp"), NMDP_2007 ("nmdp-2007"), WIKIVERSITY ("wiki");
	
	private String shortName;
	public static final String FREQUENCIES_PROPERTY = "org.dash.frequencies";
	
	private static final Logger LOGGER = Logger.getLogger(Frequencies.class.getName());
	
	private Frequencies(String shortName) {
		this.shortName = shortName;
	}
	
	public String getShortName() {
		return this.shortName;
	}
	
	private static Frequencies getDefault() {
		return NMDP_2007;
	}
	
	public static Frequencies lookup(String shortName) {
		for (Frequencies freq : values()) {
			if (freq.getShortName().equals(shortName)) {
				return freq;
			}
		}
		
		LOGGER.warning("The specified frequencies: " + shortName + " are not supported.  Defaulting to : " + getDefault());
		return Frequencies.getDefault();
	}
	
	@Override
	public String toString() {
		return getShortName();
	}
}
