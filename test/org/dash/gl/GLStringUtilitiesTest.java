package org.dash.gl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;
import org.junit.Before;
import org.junit.Test;

public class GLStringUtilitiesTest {
	private static final String HLA_A0101 = "HLA-A*01:01";
	private static final String HLA_A0102 = "HLA-A*01:02";
	private static final String A0101 = "01:01";
	private static final String HLA_A01010101 = "HLA-A*01:01:01:01";
	private static final String HLA_A010101 = "HLA-A*01:01:01";
	private String INVALID_GL_STRING;
	private String VALID_GL_STRING;

	@Before
	public void setUp() {
		List<String> invalidGLStrings = GLStringUtilities.readGLStringFile("resources/test/invalidShorthandExample.txt");
		INVALID_GL_STRING = invalidGLStrings.get(0);
		
		List<String> validGLStrings = GLStringUtilities.readGLStringFile("resources/test/fullyQualifiedExample.txt");
		VALID_GL_STRING = validGLStrings.get(0);
	}
	
	@Test
	public void testParse() {
		String alleleList = HLA_A0101 + GLStringConstants.ALLELE_AMBIGUITY_DELIMITER + HLA_A0102;
		List<String> elements = GLStringUtilities.parse(alleleList, GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
		assertTrue(HLA_A0101.equals(elements.get(0)));
		assertTrue(HLA_A0102.equals(elements.get(1)));
	}
	
	@Test
	public void testShortenAllele() {
		String allele = "01:01:01:01";
		
		String shortenedAllele = GLStringUtilities.shortenAllele(allele);
		
		assertTrue(shortenedAllele.equals("01:01:01"));
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
