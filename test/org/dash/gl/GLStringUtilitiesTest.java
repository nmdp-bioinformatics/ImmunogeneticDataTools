package org.dash.gl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;

import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;
import org.junit.Test;

public class GLStringUtilitiesTest {
	private static final String HLA_A0101 = "HLA-A*01:01";
	private static final String HLA_A0102 = "HLA-A*01:02";
	private static final String A0101 = "01:01";
	private static final String HLA_A01010101 = "HLA-A*01:01:01:01";
	private static final String HLA_A010101 = "HLA-A*01:01:01";
	private static final String INVALID_GL_STRING = "A*01:01:01:01+26:01:01^B*38:01:01/38:27+44:03:01/44:03:10/44:125^C*04:01:01:01/04:01:01:02/04:01:01:03/04:01:01:04/04:01:01:05/04:20/04:117+12:03:01:01/12:03:01:02/12:34^DPA1*01:03:01:01/01:03:01:02/01:03:01:03/01:03:01:04/01:03:01:05+01:03:01:01/01:03:01:02/01:03:01:03/01:03:01:04/01:03:01:05^DPB1*04:01:01:01/04:01:01:02+04:01:01:01/04:01:01:02^DQA1*02:01+05:05:01:01/05:05:01:02/05:05:01:03/05:09/05:11^DQB1*02:02+03:01:01:01/03:01:01:02/03:01:01:03^DRB1*07:01:01:01/07:01:01:02+11:01:01^DRB3*02:02:01:01/02:02:01:02^DRB4*01:01:01:01/03:01N";
	private static final String VALID_GL_STRING = "HLA-A*01:01:01:01+HLA-A*26:01:01^HLA-B*38:01:01/HLA-B*38:27+HLA-B*44:03:01/HLA-B*44:03:10/HLA-B*44:125^HLA-C*04:01:01:01/HLA-C*04:01:01:02/HLA-C*04:01:01:03/HLA-C*04:01:01:04/HLA-C*04:01:01:05/HLA-C*04:20/HLA-C*04:117+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34^HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05+HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05^HLA-DPB1*04:01:01:01/HLA-DPB1*04:01:01:02+HLA-DPB1*04:01:01:01/HLA-DPB1*04:01:01:02^HLA-DQA1*02:01+HLA-DQA1*05:05:01:01/HLA-DQA1*05:05:01:02/HLA-DQA1*05:05:01:03/HLA-DQA1*05:09/HLA-DQA1*05:11^HLA-DQB1*02:02+HLA-DQB1*03:01:01:01/HLA-DQB1*03:01:01:02/HLA-DQB1*03:01:01:03^HLA-DRB1*07:01:01:01/HLA-DRB1*07:01:01:02+HLA-DRB1*11:01:01^HLA-DRB3*02:02:01:01/HLA-DRB3*02:02:01:02^HLA-DRB4*01:01:01:01/HLA-DRB4*03:01N";
	private static final String TAB_DELIMITED = "TAB_DELIMITED";
	private static final String COMMA_DELIMITED = "COMMA_DELIMITED";
	
	@Test
	public void testParse() {
		String alleleList = HLA_A0101 + GLStringConstants.ALLELE_AMBIGUITY_DELIMITER + HLA_A0102;
		List<String> elements = GLStringUtilities.parse(alleleList, GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
		assertTrue(HLA_A0101.equals(elements.get(0)));
		assertTrue(HLA_A0102.equals(elements.get(1)));
	}
	
	@Test
	@Deprecated
	public void testShortenAllele() {
		String allele = "01:01:01:01";
		
		String shortenedAllele = GLStringUtilities.shortenAllele(allele);
		
		assertTrue(shortenedAllele.equals("01:01:01"));
	}
	
	@Test
	public void testTabDelimitedGLStringFile() {
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile("resources/test/tabDelimitedExample.txt");
		for (String key : glStrings.keySet()) {
			assertTrue(TAB_DELIMITED.equals(key));
		}
	}
	
	@Test
	public void testCommaDelimitedGLStringFile() {
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile("resources/test/commaDelimitedExample.txt");
		for (String key : glStrings.keySet()) {
			assertTrue(COMMA_DELIMITED.equals(key));
		}
	}
	
	@Test
	public void testFieldLevelComparison() {
		String allele = HLA_A01010101;
		String referenceAllele = HLA_A010101;
		
		assertTrue(GLStringUtilities.fieldLevelComparison(allele, referenceAllele));
		
		allele = HLA_A01010101;
		referenceAllele = HLA_A0101;
		
		assertTrue(GLStringUtilities.fieldLevelComparison(allele, referenceAllele));
		
		allele = HLA_A010101;
		referenceAllele = HLA_A01010101;
		
		assertTrue(GLStringUtilities.fieldLevelComparison(allele, referenceAllele));
		
		allele = HLA_A01010101;
		referenceAllele = HLA_A0102;
		
		assertFalse(GLStringUtilities.fieldLevelComparison(allele, referenceAllele));
	}

	@Test
	public void testFillLocus() {
		assertTrue(HLA_A0101.equals(GLStringUtilities.fillLocus(GLStringConstants.HLA_A, A0101)));
	}
	
	@Test
	public void testValidateGLStringFormatNegative() {
		assertFalse(GLStringUtilities.validateGLStringFormat(INVALID_GL_STRING));
	}
	
	@Test
	public void testValidateGLStringFormatPostive() {
		assertTrue(GLStringUtilities.validateGLStringFormat(GLStringUtilities.fullyQualifyGLString(INVALID_GL_STRING)));
	}
	
	@Test
	public void testFullyQualifyGLString() {		
		String fullyQualifiedGLString = GLStringUtilities.fullyQualifyGLString(INVALID_GL_STRING);
		
		assertTrue(VALID_GL_STRING.equals(fullyQualifiedGLString));
	}
	
	@Test
	public void testConvertToMug() {
		MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(VALID_GL_STRING);
		
		assertNotNull(mug);
	}
}
