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

public enum Locus {
	HLA_A ("HLA-A", "A", "A"),
	HLA_C ("HLA-C", "C", "C"),
	HLA_B ("HLA-B", "B", "B"),
	HLA_DRB3 ("HLA-DRB3", "DRB3", null),
	HLA_DRB4 ("HLA-DRB4", "DRB4", null),
	HLA_DRB5 ("HLA-DRB5", "DRB5", null),
	HLA_DRB345 ("HLA-DRB345", "DRB345", "DRB3-4-5"),
	HLA_DRBX ("HLA-DRBX", "DRBX", null),
	HLA_DRB1 ("HLA-DRB1", "DRB1", "DRB1"),
	HLA_DQB1 ("HLA-DQB1", "DQB1", "DQB1"),
	HLA_DQA1 ("HLA-DQA1", "DQA1", null),
	HLA_DPB1 ("HLA-DPB1", "DPB1", null),
	HLA_DPA1 ("HLA-DPA1", "DPA1", null);

	
	String fullName;
	String shortName;
	String freqName;
	
	public static final EnumSet<Locus> A_C_B_LOCI = EnumSet.of(Locus.HLA_A, Locus.HLA_C, Locus.HLA_B);
	public static final EnumSet<Locus> C_B_LOCI = EnumSet.of(Locus.HLA_C, Locus.HLA_B);	
	public static final EnumSet<Locus> DRB1_DQB1_LOCI = EnumSet.of(Locus.HLA_DRB1, Locus.HLA_DQB1);
	public static final EnumSet<Locus> DRB_DQB_LOCI = EnumSet.of(Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQB1);
	public static final EnumSet<Locus> DRB_DQ_LOCI = EnumSet.of(Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQB1, Locus.HLA_DQA1);
	public static final EnumSet<Locus> FIVE_LOCUS = EnumSet.of(Locus.HLA_A, Locus.HLA_C, Locus.HLA_B, Locus.HLA_DRB1, Locus.HLA_DQB1);
	public static final EnumSet<Locus> SIX_LOCUS = EnumSet.of(Locus.HLA_A, Locus.HLA_C, Locus.HLA_B, Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQB1);
	
	private static final EnumSet<?>[] LOCI_ARRAY = new EnumSet<?>[] {A_C_B_LOCI, C_B_LOCI, DRB1_DQB1_LOCI, DRB_DQB_LOCI, DRB_DQ_LOCI, FIVE_LOCUS, SIX_LOCUS};
	
	private static final Logger LOGGER = Logger.getLogger(Locus.class.getName());
	
	private Locus(String fullName, String shortName, String freqName) {
		this.fullName = fullName;
		this.shortName = shortName;
		this.freqName = freqName;
	}
	
	@SuppressWarnings("unchecked")
	public static EnumSet<Locus> lookup(Set<Locus> loci) {
		Set<Locus> normalizedLoci = normalizeLoci(loci);
		for (int i=0;i<LOCI_ARRAY.length;i++) {
			if (normalizedLoci.containsAll(LOCI_ARRAY[i]) && LOCI_ARRAY[i].containsAll(normalizedLoci)) {
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
	
	public static Set<Locus> normalizeLoci(Set<Locus> loci) {
		Set<Locus> normalizedLoci = new HashSet<Locus>();
		for (Locus locus : loci) {
			normalizedLoci.add(normalizeLocus(locus));
		}
		
		return normalizedLoci;
	}
	
	public static Locus normalizeLocus(Locus locus) {
		return isDRB345(locus) ? HLA_DRB345 : locus;
	}
	
	public static boolean isDRB345(Locus locus) {
		switch (locus) {
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRBX:
		case HLA_DRB345:
			return true;
		default:
			return false;
		}
	}
	
	public boolean hasIndividualFrequencies() {
		if (freqName != null) {
			return true;
		}
		
		return false;
	}
	
	public static Locus lookup(String value) {
		for (Locus locus : values()) {
			if (locus.getFullName().equals(value) || locus.getShortName().equals(value)) {
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
