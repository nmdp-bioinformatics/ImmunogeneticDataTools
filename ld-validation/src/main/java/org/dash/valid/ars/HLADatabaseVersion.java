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
package org.dash.valid.ars;

import java.util.logging.Logger;

public enum HLADatabaseVersion {
	HLAD3200 ("3.20.0", "3200"),
	HLAD3190 ("3.19.0", "3190"),
	HLADB3180 ("3.18.0", "3180"), 
	HLADB3150 ("3.15.0", "3150"), 
	HLADB3120 ("3.12.0", "3120"),
	HLADB3110 ("3.11.0", "3110"),
	HLADB3100 ("3.10.0", "3100"),
	HLADB390 ("3.9.0", "390"),
	HLADB380 ("3.8.0", "380"),
	HLADB370 ("3.7.0", "370"),
	HLADB360 ("3.6.0", "360"),
	HLADB350 ("3.5.0", "350"),
	HLADB340 ("3.4.0", "340"),
	HLADB330 ("3.3.0", "330"),
	HLADB320 ("3.2.0", "320"),
	HLADB310 ("3.1.0", "310"),
	HLADB300 ("3.0.0", "300");
	 
	private String arsName;
	private String cwdName;
	 
	public static final String HLADB_PROPERTY = "org.dash.hladb";
	public static final String ARS_PROPERTY = "org.dash.ars";
	
	public static final String ARS_BY_HLADB = "hladb";
	 
	private static final Logger LOGGER = Logger.getLogger(HLADatabaseVersion.class.getName());
	 
	HLADatabaseVersion(String arsName, String cwdName) {
		 this.arsName = arsName;
		 this.cwdName = cwdName;
	}

	public String getArsName() {
		return arsName;
	}

	public String getCwdName() {
		return cwdName;
	}
	
	public static HLADatabaseVersion getLatest() {
		return HLADatabaseVersion.HLAD3200;
	}
	
	public static HLADatabaseVersion lookup(String arsName) {
		for (HLADatabaseVersion hladb : values()) {
			if (hladb.getArsName().equals(arsName)) {
				return hladb;
			}			
		}
		
		LOGGER.warning("The specified HLA DB version: " + arsName + " is not supported.  Defaulting to latest: " + getLatest());
		return HLADatabaseVersion.getLatest();
	}
}
