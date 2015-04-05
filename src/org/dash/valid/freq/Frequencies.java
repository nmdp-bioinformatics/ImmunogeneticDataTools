package org.dash.valid.freq;

import java.util.logging.Logger;

public enum Frequencies {
	NMDP ("nmdp"), WIKIVERSITY ("wiki");
	
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
		return WIKIVERSITY;
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
}
