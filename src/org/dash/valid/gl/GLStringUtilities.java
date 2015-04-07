package org.dash.valid.gl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.dash.valid.ars.AntigenRecognitionSiteLoader;
import org.dash.valid.cwd.CommonWellDocumentedLoader;
import org.immunogenomics.gl.MultilocusUnphasedGenotype;
import org.immunogenomics.gl.client.GlClient;
import org.immunogenomics.gl.client.GlClientException;
import org.immunogenomics.gl.client.local.LocalGlClient;

public class GLStringUtilities {
	private static final String ALPHA_REGEX = "[A-Z]";
	private static final String GL_STRING_DELIMITER_REGEX = "[\\^\\|\\+~/]";
	private static final String FILE_DELIMITER_REGEX = "[\t,]";
	public static final String ESCAPED_ASTERISK = "\\*";
	public static final String VARIANTS_REGEX = "[SNLQ]";
	public static final String COLON = ":";
	
    private static final Logger LOGGER = Logger.getLogger(GLStringUtilities.class.getName());
	
	public static Set<String> parse(String value, String delimiter) {
		Set<String> elements = new HashSet<String>();
		StringTokenizer st = new StringTokenizer(value, delimiter);
		while (st.hasMoreTokens()) {
			elements.add(st.nextToken());
		}
		
		return elements;
	}
	
	public static boolean validateGLStringFormat(String glString) {
		StringTokenizer st = new StringTokenizer(glString, GL_STRING_DELIMITER_REGEX);
		String token;
		while (st.hasMoreTokens()) {
			token = st.nextToken();
			String[] parts = token.split(COLON);
			LOGGER.finest(token);
			if (!token.startsWith(GLStringConstants.HLA_DASH)) {
				LOGGER.warning("GLString is invalid: " + glString);
				LOGGER.warning("Locus not qualified with " + GLStringConstants.HLA_DASH + " for segment: " + token);
				return false;
			}
			if (parts.length < 2) {
				LOGGER.warning("GLString is invalid: " + glString);
				LOGGER.warning("Unexpected allele: " + token);
				return false;
			}
		}
		
		return true;
	}
	
	public static List<String> checkCommonWellDocumented(String glString){
		List<String> notCommon = new ArrayList<String>();
		
		Set<String> cwdAlleles = CommonWellDocumentedLoader.getInstance().getCwdAlleles();
		
		StringTokenizer st = new StringTokenizer(glString, GL_STRING_DELIMITER_REGEX);
		String token;
		while (st.hasMoreTokens()) {
			token = st.nextToken();
			
			if (!checkCommonWellDocumented(cwdAlleles, token)) {
				notCommon.add(token);
			}

		}
		
		return notCommon;
	}

	/**
	 * @param cwdAlleles
	 * @param token
	 */
	private static boolean checkCommonWellDocumented(Set<String> cwdAlleles, String allele) {		
		if (cwdAlleles.contains(allele)) {
			return true;
		}
		
		for (String cwdAllele : cwdAlleles) {
			// TODO:  validate behavior
			//if (GLStringUtilities.fieldLevelComparison(allele, cwdAllele) >= 0) {
			if (allele.equals(cwdAllele)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static int fieldLevelComparison(String allele, String referenceAllele) {
		String[] alleleParts = allele.split(COLON);
		String[] referenceAlleleParts = referenceAllele.split(COLON);
		
		int comparisonLength = (alleleParts.length < referenceAlleleParts.length) ? alleleParts.length : referenceAlleleParts.length;
		
		StringBuffer alleleBuffer = new StringBuffer();
		StringBuffer referenceAlleleBuffer = new StringBuffer();
		
		for (int i=0;i<comparisonLength;i++) {
			alleleBuffer.append(alleleParts[i]);
			referenceAlleleBuffer.append(referenceAlleleParts[i]);
			if (i<comparisonLength - 1) {
				alleleBuffer.append(COLON);
				referenceAlleleBuffer.append(COLON);
			}
		}
		
		boolean match = alleleBuffer.toString().equals(referenceAlleleBuffer.toString());

		if (match) {
			return comparisonLength;
		}

		return -1;
	}

	/**
	 * @param locus
	 * @param alleleBuffer
	 * @param match
	 * @return
	 * @throws UnexpectedAlleleException 
	 */
	public static boolean checkAntigenRecognitionSite(String allele, String referenceAllele) {
		String[] parts = allele.split(ESCAPED_ASTERISK);
		String locus = parts[0];
		AntigenRecognitionSiteLoader instance = AntigenRecognitionSiteLoader.getInstance();
		HashMap<String, Set<String>> arsMap = new HashMap<String, Set<String>>();
		
		switch (locus) {
		case GLStringConstants.HLA_B:
			arsMap = instance.getbArsMap();
			break;
		case GLStringConstants.HLA_C:
			arsMap = instance.getcArsMap();
			break;
		case GLStringConstants.HLA_DRB1:
			arsMap = instance.getDrb1ArsMap();
			break;
		case GLStringConstants.HLA_DRB3:
			arsMap = instance.getDrb3ArsMap();
			break;
		case GLStringConstants.HLA_DRB4:
			arsMap = instance.getDrb4ArsMap();
			break;
		case GLStringConstants.HLA_DRB5:
			arsMap = instance.getDrb5ArsMap();
			break;
		case GLStringConstants.HLA_DQB1:
			arsMap = instance.getDqb1ArsMap();
			break;
		default:
			return false;
		}
		
		parts = allele.split(COLON);
		
		if (parts.length > 2 && Pattern.matches(VARIANTS_REGEX, "" + allele.charAt(allele.length() - 1))) {
			allele = parts[0] + COLON + parts[1] + allele.charAt(allele.length() - 1);
			LOGGER.finest("Found an SNLQ while comparing ARS: " + allele);
		}
		else if (parts.length < 2) {
			LOGGER.warning("Unexpected allele: " + allele);
		}
		else {
			allele = parts[0] + COLON + parts[1];
		}
		
		for (String arsCode : arsMap.keySet()) {
			if (arsCode.equals(referenceAllele) && arsMap.get(arsCode).contains(allele)) {
				return true;
			}
		}
			
		return false;
	}
	
	public static String fullyQualifyGLString(String shorthand) {
		StringTokenizer st = new StringTokenizer(shorthand, GL_STRING_DELIMITER_REGEX, true);
		StringBuffer sb = new StringBuffer();
		String part;
		String locus = null;
		
		while (st.hasMoreTokens()) {
			part = st.nextToken();
			if (part.substring(0,1).matches(ALPHA_REGEX)) {
				if (!part.startsWith(GLStringConstants.HLA_DASH)){
					part = GLStringConstants.HLA_DASH + part;
				}

				String[] splitString = part.split(ESCAPED_ASTERISK);
				locus = splitString[0];
			}
			else if (part.substring(0,1).matches(GL_STRING_DELIMITER_REGEX)) {
				sb.append(part);
				continue;
			}
			else {
				part = fillLocus(locus, part);
			}
			
			sb.append(part);
		}
		
		return sb.toString();
	}
	
	public static String fillLocus(String locus, String segment) {
		if (!segment.substring(0,1).matches(ALPHA_REGEX)) {
			segment = locus + GLStringConstants.ASTERISK + segment;
		}
		return segment;
	}
	
	public static LinkedHashMap<String, String> readGLStringFile(String filename) {
		File glStringFile = new File(filename);
		BufferedReader reader = null;
		String line;
		LinkedHashMap<String, String> glStrings = new LinkedHashMap<String, String>();
		
		try {
			InputStream in = new FileInputStream(glStringFile);
			reader = new BufferedReader(new InputStreamReader(in));
			
			String[] parts = null;
			int lineNumber = 0;
			while ((line = reader.readLine()) != null) {
				parts = line.split(FILE_DELIMITER_REGEX);
				if (parts.length == 1) {
					glStrings.put(filename + "-" + lineNumber, parts[0]);
				}
				else if (parts.length == 2) {
					glStrings.put(parts[0], parts[1]);
				}
				else {
					LOGGER.warning("Unexpected line format at line " + lineNumber + ": " + filename);
				}
				
				lineNumber++;
			}
		}
		catch (FileNotFoundException e) {
			LOGGER.severe("Couldn't find GL String file: " + filename);
			e.printStackTrace();
		}
		catch (IOException e) {
			LOGGER.severe("Problem opening GL String file: " + filename);
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				LOGGER.severe("Problem closing reader/stream.");
				e.printStackTrace();
			}
		}
		
		return glStrings;
	}
	
	public static MultilocusUnphasedGenotype convertToMug(String glString) {
		MultilocusUnphasedGenotype mug = null;
		
		try {
			//TODO: should use strict but example GL Strings are missing intron variants in some cases (HLA-DQB1*02:02)
			//GlClient glClient = LocalGlClient.createStrict();
			
			GlClient glClient = LocalGlClient.create();
			mug = glClient.createMultilocusUnphasedGenotype(glString);			
		}
		catch (GlClientException e) {
			LOGGER.severe("Couldn't convert GLString to MultiLocusUnphasedGenotype");
			e.printStackTrace();
		}
		
		return mug;
	}
}
