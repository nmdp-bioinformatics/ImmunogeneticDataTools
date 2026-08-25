package org.dash.valid;

import java.util.HashMap;
import java.util.List;

import org.dash.valid.gl.haplo.Haplotype;

/**
 * A minimal {@link DisequilibriumElement} used to represent a <em>candidate</em> haplotype
 * being matched against reference data — not a reference row itself. Built directly from a
 * candidate's alleles so {@link DisequilibriumElement#equals} can be used to search the
 * reference list (see {@link org.dash.valid.HLALinkageDisequilibrium}) without needing a
 * real frequency of its own.
 */
public class CoreDisequilibriumElement extends DisequilibriumElement {
	/**
	 * Builds a candidate element from one haplotype's own alleles.
	 *
	 * @param hlaElementMap this candidate's alleles, by locus
	 * @param haplotype the haplotype this element represents
	 */
	public CoreDisequilibriumElement(HashMap<Locus, List<String>> hlaElementMap, Haplotype haplotype) {
		setHlaElementMap(hlaElementMap);
		setHaplotype(haplotype);
	}
	
	public CoreDisequilibriumElement() {
		
	}
	
	@Override
	public String getFrequencyInfo() {
		// TODO:  What happens if call getFrequencyInfo() here?
		return null;
	}
}
