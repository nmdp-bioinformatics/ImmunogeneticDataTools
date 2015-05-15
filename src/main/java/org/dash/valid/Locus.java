package org.dash.valid;

import java.util.logging.Logger;

public enum Locus {
	HLA_A ("HLA-A", "A"),
	HLA_B ("HLA-B", "B"),
	HLA_C ("HLA-C", "C"),
	HLA_DRB1 ("HLA-DRB1", "DRB1"),
	HLA_DRB3 ("HLA-DRB3", "DRB3"),
	HLA_DRB4 ("HLA-DRB4", "DRB4"),
	HLA_DRB5 ("HLA-DRB5", "DRB5"),
	HLA_DQB1 ("HLA-DQB1", "DQB1"),
	HLA_DQA1 ("HLA-DQA1", "DQA1"),
	HLA_DPB1 ("HLA-DPB1", "DPB1"),
	HLA_DPA1 ("HLA-DPA1", "DPA1"),
	HLA_DRB345 ("HLA-DRB345", "DRB345"),
	HLA_DRBX ("HLA-DRBX", "DRBX");
	
	String fullName;
	String shortName;
	
	private static final Logger LOGGER = Logger.getLogger(Locus.class.getName());
	
	private Locus(String fullName, String shortName) {
		this.fullName = fullName;
		this.shortName = shortName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
	
	public static Locus lookup(String value) {
		for (Locus locus : values()) {
			if (locus.getFullName().equals(value)) {
				return locus;
			}			
		}
		
		LOGGER.warning("The specified locus: " + value + " is not recognized.");
		return null;
	}
	
	public String toString() {
		return getFullName();
	}
}
