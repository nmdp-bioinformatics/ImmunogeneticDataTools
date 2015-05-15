package org.dash.valid.ars;

import java.util.logging.Logger;

public enum HLADatabaseVersion {
	HLADB3180 ("3.18.0", "3180"), 
	HLADB3150 ("3.15.0", "3150"), 
	HLADB3120 ("3.12.0", "3120"),
	HLADB3110 ("3.11.0", "3110"),
	HLADB3100 ("3.10.0", "3100");
	 
	private String arsName;
	private String cwdName;
	 
	public static final String HLADB_PROPERTY = "org.dash.hladb";
	 
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
		return HLADatabaseVersion.HLADB3180;
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
