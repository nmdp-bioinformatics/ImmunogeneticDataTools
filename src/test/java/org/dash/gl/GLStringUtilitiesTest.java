package org.dash.gl;

import java.util.LinkedHashMap;
import java.util.Set;

import junit.framework.TestCase;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.nmdp.gl.MultilocusUnphasedGenotype;
import org.junit.Test;

public class GLStringUtilitiesTest extends TestCase {
	private static final String BOGUS_ALLELE = "HLA-A*QI:UD";
	private static final String HLA_DQB10202 = "HLA-DQB1*02:02";
	private static final String HLA_A0101 = "HLA-A*01:01";
	private static final String HLA_A0102 = "HLA-A*01:02";
	private static final String A0101 = "01:01";
	private static final String HLA_A01010101 = "HLA-A*01:01:01:01";
	private static final String HLA_A010101 = "HLA-A*01:01:01";
	private static final String HLA_B15010102N = "HLA-B*15:01:01:02N";
	private static final String HLA_B1501g = "HLA-B*15:01g";
	private static final String HLA_C0702g = "HLA-C*07:02g";
	private static final String HLA_C07020101 = "HLA-C*07:02:01:01";
	private static final String HLA_C0401g = "HLA-C*04:01g";
	private static final String HLA_C04010101 = "HLA-C*04:01:01:01";
	private static final String INVALID_GL_STRING = "A*01:01:01:01+26:01:01^B*38:01:01/38:27+44:03:01/44:03:10/44:125^C*04:01:01:01/04:01:01:02/04:01:01:03/04:01:01:04/04:01:01:05/04:20/04:117+12:03:01:01/12:03:01:02/12:34^DPA1*01:03:01:01/01:03:01:02/01:03:01:03/01:03:01:04/01:03:01:05+01:03:01:01/01:03:01:02/01:03:01:03/01:03:01:04/01:03:01:05^DPB1*04:01:01:01/04:01:01:02+04:01:01:01/04:01:01:02^DQA1*02:01+05:05:01:01/05:05:01:02/05:05:01:03/05:09/05:11^DQB1*02:02+03:01:01:01/03:01:01:02/03:01:01:03^DRB1*07:01:01:01/07:01:01:02+11:01:01^DRB3*02:02:01:01/02:02:01:02^DRB4*01:01:01:01/03:01N";
	private static final String VALID_GL_STRING = "HLA-A*01:01:01:01+HLA-A*26:01:01^HLA-B*38:01:01/HLA-B*38:27+HLA-B*44:03:01/HLA-B*44:03:10/HLA-B*44:125^HLA-C*04:01:01:01/HLA-C*04:01:01:02/HLA-C*04:01:01:03/HLA-C*04:01:01:04/HLA-C*04:01:01:05/HLA-C*04:20/HLA-C*04:117+HLA-C*12:03:01:01/HLA-C*12:03:01:02/HLA-C*12:34^HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05+HLA-DPA1*01:03:01:01/HLA-DPA1*01:03:01:02/HLA-DPA1*01:03:01:03/HLA-DPA1*01:03:01:04/HLA-DPA1*01:03:01:05^HLA-DPB1*04:01:01:01/HLA-DPB1*04:01:01:02+HLA-DPB1*04:01:01:01/HLA-DPB1*04:01:01:02^HLA-DQA1*02:01+HLA-DQA1*05:05:01:01/HLA-DQA1*05:05:01:02/HLA-DQA1*05:05:01:03/HLA-DQA1*05:09/HLA-DQA1*05:11^HLA-DQB1*02:02+HLA-DQB1*03:01:01:01/HLA-DQB1*03:01:01:02/HLA-DQB1*03:01:01:03^HLA-DRB1*07:01:01:01/HLA-DRB1*07:01:01:02+HLA-DRB1*11:01:01^HLA-DRB3*02:02:01:01/HLA-DRB3*02:02:01:02^HLA-DRB4*01:01:01:01/HLA-DRB4*03:01N";
	private static final String TAB_DELIMITED = "TAB_DELIMITED";
	private static final String COMMA_DELIMITED = "COMMA_DELIMITED";
	
	@Test
	public void testParse() {
		String alleleList = HLA_A0101 + GLStringConstants.ALLELE_AMBIGUITY_DELIMITER + HLA_A0102;
		Set<String> elements = GLStringUtilities.parse(alleleList, GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
		assertTrue(elements.contains(HLA_A0101));
		assertTrue(elements.contains(HLA_A0102));
	}
	
	@Test
	public void testTabDelimitedGLStringFile() {
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile("tabDelimitedExample.txt");
		for (String key : glStrings.keySet()) {
			assertTrue(TAB_DELIMITED.equals(key));
		}
	}
	
	@Test
	public void testCommaDelimitedGLStringFile() {
		LinkedHashMap<String, String> glStrings = GLStringUtilities.readGLStringFile("commaDelimitedExample.txt");
		for (String key : glStrings.keySet()) {
			assertTrue(COMMA_DELIMITED.equals(key));
		}
	}
	
	@Test
	public void testCheckAntigenRecognitionSite() {
		assertNotNull(GLStringUtilities.checkAntigenRecognitionSite(HLA_C07020101, HLA_C0702g));
		
		assertNotNull(GLStringUtilities.checkAntigenRecognitionSite(HLA_C04010101, HLA_C0401g));
		
		assertNotNull(GLStringUtilities.checkAntigenRecognitionSite(HLA_B15010102N, HLA_B1501g));
	}
	
	@Test
	public void testFieldLevelComparison() {		
		assertTrue(GLStringUtilities.fieldLevelComparison(HLA_A01010101, HLA_A010101) != null);
		
		assertTrue(GLStringUtilities.fieldLevelComparison(HLA_A01010101, HLA_A0101) != null);
		
		assertTrue(GLStringUtilities.fieldLevelComparison(HLA_A010101, HLA_A01010101) != null);
		
		assertFalse(GLStringUtilities.fieldLevelComparison(HLA_A01010101, HLA_A0102) != null);	
	}

	@Test
	public void testFillLocus() {
		assertTrue(HLA_A0101.equals(GLStringUtilities.fillLocus(Locus.HLA_A, A0101)));
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
	
	@Test
	public void testCommonWellDocumented() {
		Set<String> notCommon = GLStringUtilities.checkCommonWellDocumented(HLA_DQB10202 + GLStringConstants.ALLELE_AMBIGUITY_DELIMITER + BOGUS_ALLELE + GLStringConstants.ALLELE_AMBIGUITY_DELIMITER + HLA_A01010101);
		assertTrue(notCommon.contains(BOGUS_ALLELE));
		assertTrue(notCommon.contains(HLA_DQB10202));
		assertFalse(notCommon.contains(HLA_A01010101));
	}
}
