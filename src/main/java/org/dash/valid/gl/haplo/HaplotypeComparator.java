package org.dash.valid.gl.haplo;

import java.util.Arrays;
import java.util.Comparator;

public class HaplotypeComparator implements Comparator<Haplotype> {

	@Override
	public int compare(Haplotype element1, Haplotype element2) {
		if (element1.equals(element2)) {
			return 0;
		}
		
		// else sort alphabetically
		int ret = element1.toString().compareTo(element2.toString());
		
		if (ret == 0) {
			ret = Arrays.equals(element1.getHaplotypeInstances(), element2.getHaplotypeInstances()) ? 0 : 1;
		}
		return ret;
	}
}
