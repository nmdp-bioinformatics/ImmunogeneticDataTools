package org.dash;

import java.util.List;

import org.dash.valid.GLStringConstants;
import org.dash.valid.GLStringUtilities;
import org.junit.Assert;
import org.junit.Test;

public class GLStringUtilitiesTest {
	private static final String HLA_A0101 = "HLA-A*0101";
	private static final String HLA_A0102 = "HLA-A*0102";
	private static final String A0101 = "0101";

	@Test
	public void testParse() {
		String alleleList = HLA_A0101 + GLStringConstants.ALLELE_AMBIGUITY_DELIMITER + HLA_A0102;
		List<String> elements = GLStringUtilities.parse(alleleList, GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
		Assert.assertTrue(HLA_A0101.equals(elements.get(0)));
		Assert.assertTrue(HLA_A0102.equals(elements.get(1)));
	}

	@Test
	public void testFillLocus() {
		Assert.assertTrue(HLA_A0101.equals(GLStringUtilities.fillLocus(GLStringConstants.HLA_A, A0101)));
	}
}
