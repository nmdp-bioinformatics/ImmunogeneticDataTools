package org.dash.valid;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

public enum Locus {
	HLA_A ("HLA-A", "A", "A"),
	HLA_B ("HLA-B", "B", "B"),
	HLA_C ("HLA-C", "C", "C"),
	HLA_DRB1 ("HLA-DRB1", "DRB1", "DRB1"),
	HLA_DRB3 ("HLA-DRB3", "DRB3", null),
	HLA_DRB4 ("HLA-DRB4", "DRB4", null),
	HLA_DRB5 ("HLA-DRB5", "DRB5", null),
	HLA_DRB345 ("HLA-DRB345", "DRB345", "DRB3-4-5"),
	HLA_DRBX ("HLA-DRBX", "DRBX", null),
	HLA_DQB1 ("HLA-DQB1", "DQB1", "DQB1"),
	HLA_DQA1 ("HLA-DQA1", "DQA1", null),
	HLA_DPB1 ("HLA-DPB1", "DPB1", null),
	HLA_DPA1 ("HLA-DPA1", "DPA1", null);

	
	String fullName;
	String shortName;
	String freqName;
	
	public static final EnumSet<Locus> A_B_C_LOCI = EnumSet.of(Locus.HLA_A, Locus.HLA_B, Locus.HLA_C);
	public static final EnumSet<Locus> B_C_LOCI = EnumSet.of(Locus.HLA_B, Locus.HLA_C);	
	public static final EnumSet<Locus> DRB1_DQB1_LOCI = EnumSet.of(Locus.HLA_DRB1, Locus.HLA_DQB1);
	public static final EnumSet<Locus> DRB_DQB_LOCI = EnumSet.of(Locus.HLA_DRB1, Locus.HLA_DRB345, Locus.HLA_DQB1);
	public static final EnumSet<Locus> DRB_DQ_LOCI = EnumSet.of(Locus.HLA_DRB1, Locus.HLA_DRB345, Locus.HLA_DQB1, Locus.HLA_DQA1);
	
	private static final EnumSet<?>[] LOCI_ARRAY = new EnumSet<?>[] {A_B_C_LOCI, B_C_LOCI, DRB1_DQB1_LOCI, DRB_DQB_LOCI, DRB_DQ_LOCI};
	
	private static final Logger LOGGER = Logger.getLogger(Locus.class.getName());
	
	private Locus(String fullName, String shortName, String freqName) {
		this.fullName = fullName;
		this.shortName = shortName;
		this.freqName = freqName;
	}
	
	@SuppressWarnings("unchecked")
	public static EnumSet<Locus> lookup(Set<Locus> loci) {
		for (int i=0;i<LOCI_ARRAY.length;i++) {
			if (loci.containsAll(LOCI_ARRAY[i]) && LOCI_ARRAY[i].containsAll(loci)) {
				return (EnumSet<Locus>) LOCI_ARRAY[i];
			}
		}
		
		return null;
	}

	public String getFullName() {
		return fullName;
	}

	public String getShortName() {
		return shortName;
	}
	
	public String getFrequencyName() {
		return freqName;
	}
	
	public boolean hasIndividualFrequencies() {
		if (freqName != null) {
			return true;
		}
		
		return false;
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
