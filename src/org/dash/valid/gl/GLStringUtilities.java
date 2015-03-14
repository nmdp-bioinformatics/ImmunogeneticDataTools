package org.dash.valid.gl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.immunogenomics.gl.MultilocusUnphasedGenotype;
import org.immunogenomics.gl.client.GlClient;
import org.immunogenomics.gl.client.GlClientException;
import org.immunogenomics.gl.client.local.LocalGlClient;

public class GLStringUtilities {
	private static final String ALPHA_REGEX = "[A-Z]";
	private static final String GL_STRING_DELIMITER_REGEX = "[\\^\\|\\+~/]";
	public static final String ESCAPED_ASTERISK = "\\*";
	public static final String COLON = ":";
	
    private static final Logger LOGGER = Logger.getLogger(GLStringUtilities.class.getName());
    
    static {
    	try {
			LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
    	}
    	catch (IOException ioe) {
    		LOGGER.severe("Could not add file handler to logger");
    	}
    }
	
	public static List<String> parse(String value, String delimiter) {
		List<String> elements = new ArrayList<String>();
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
			LOGGER.finest(token);
			if (!token.startsWith(GLStringConstants.HLA_DASH)) {
				LOGGER.warning("GLString is invalid: " + glString);
				LOGGER.warning("Locus not qualified with " + GLStringConstants.HLA_DASH + " for segment: " + token);
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean fieldLevelComparison(String allele, String referenceAllele) {
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
		
		return (alleleBuffer.toString().equals(referenceAlleleBuffer.toString()));
	}
	
	// TODO:  Remove after confirming deprecation and replacement by fieldLevelComparison
	@Deprecated
	public static String shortenAllele(String allele) {
		String[] parts = allele.split(COLON);
		String shortenedAllele = null;
		
		if (parts.length > 3) {
			shortenedAllele =  parts[0] + COLON + parts[1] + COLON + parts[2];
		}
		else if (parts.length >= 2) {
			shortenedAllele = parts[0] + COLON + parts[1];
		}
		else {
			shortenedAllele = parts[0];
		}
		
		LOGGER.finest("ShortenedAllele = " + shortenedAllele);
		
		return shortenedAllele;
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
	
	public static List<String> readGLStringFile(String filename) {
		File glStringFile = new File(filename);
		BufferedReader reader = null;
		String glString;
		List<String> glStrings = new ArrayList<String>();
		
		try {
			InputStream in = new FileInputStream(glStringFile);
			reader = new BufferedReader(new InputStreamReader(in));
			
			while ((glString = reader.readLine()) != null) {
				glStrings.add(glString);
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
