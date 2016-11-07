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
package org.dash.valid.gl;

import java.io.BufferedReader;
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.dash.valid.Locus;
import org.dash.valid.ars.AntigenRecognitionSiteLoader;
import org.dash.valid.cwd.CommonWellDocumentedLoader;
import org.nmdp.gl.MultilocusUnphasedGenotype;
import org.nmdp.gl.client.GlClient;
import org.nmdp.gl.client.GlClientException;
import org.nmdp.gl.client.local.LocalGlClient;

public class GLStringUtilities {
	private static final String ALPHA_REGEX = "[A-Z]";
	static final String GL_STRING_DELIMITER_REGEX = "[\\^\\|\\+~/]";
	private static final String FILE_DELIMITER_REGEX = "[\t,]";
	public static final String ESCAPED_ASTERISK = "\\*";
	public static final String VARIANTS_REGEX = "[SNLQ]";
	public static final String COLON = ":";
	public static final int P_GROUP_LEVEL = 2;

	private static final Logger LOGGER = Logger
			.getLogger(GLStringUtilities.class.getName());

	public static List<String> parse(String value, String delimiter) {
		List<String> elements = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(value, delimiter);
		while (st.hasMoreTokens()) {
			elements.add(st.nextToken());
		}

		return elements;
	}

	public static boolean validateGLStringFormat(String glString) {
		StringTokenizer st = new StringTokenizer(glString,
				GL_STRING_DELIMITER_REGEX);
		String token;
		while (st.hasMoreTokens()) {
			token = st.nextToken();
			String[] parts = token.split(COLON);
			LOGGER.finest(token);
			if (!token.startsWith(GLStringConstants.HLA_DASH)) {
				LOGGER.warning("GLString is invalid: " + glString);
				LOGGER.warning("Locus not qualified with "
						+ GLStringConstants.HLA_DASH + " for segment: " + token);
				return false;
			}
			if (parts.length < P_GROUP_LEVEL && !GLStringConstants.NNNN.equals(parts)) {
				LOGGER.warning("GLString is invalid: " + glString);
				LOGGER.warning("Unexpected allele: " + token);
				return false;
			}
		}

		return true;
	}

	public static Set<String> checkCommonWellDocumented(String glString) {
		Set<String> notCommon = new HashSet<String>();

		Set<String> cwdAlleles = CommonWellDocumentedLoader.getInstance()
				.getCwdAlleles();

		StringTokenizer st = new StringTokenizer(glString,
				GL_STRING_DELIMITER_REGEX);
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
	private static boolean checkCommonWellDocumented(Set<String> cwdAlleles,
			String allele) {
		if (cwdAlleles.contains(allele)) {
			return true;
		}

		for (String cwdAllele : cwdAlleles) {
			if (allele.equals(cwdAllele)) {
				return true;
			}
		}

		return false;
	}

	public static boolean fieldLevelComparison(String allele,
			String referenceAllele) {
		if (allele == null || referenceAllele == null) {
			return false;
		}
		
		String[] alleleParts = allele.split(COLON);
		String[] referenceAlleleParts = referenceAllele.split(COLON);

		int comparisonLength = (alleleParts.length < referenceAlleleParts.length) ? alleleParts.length
				: referenceAlleleParts.length;

		StringBuffer alleleBuffer = new StringBuffer();
		StringBuffer referenceAlleleBuffer = new StringBuffer();

		for (int i = 0; i < comparisonLength; i++) {
			alleleBuffer.append(alleleParts[i]);
			referenceAlleleBuffer.append(referenceAlleleParts[i]);
			if (i < comparisonLength - 1) {
				alleleBuffer.append(COLON);
				referenceAlleleBuffer.append(COLON);
			}
		}

		boolean match = alleleBuffer.toString().equals(
				referenceAlleleBuffer.toString());

		return match;
	}

	/**
	 * @param locus
	 * @param alleleBuffer
	 * @param match
	 * @return
	 * @throws UnexpectedAlleleException
	 */
	public static boolean checkAntigenRecognitionSite(String allele,
			String referenceAllele) {
		String matchedValue = convertToProteinLevel(allele);
				
		int partLength = allele.split(COLON).length;
		AntigenRecognitionSiteLoader instance = null;
		
		try {
			instance = AntigenRecognitionSiteLoader.getInstance();
		}
		catch (IOException | InvalidFormatException e) {
			LOGGER.warning("Could not load ars data.");
			e.printStackTrace();
		}
		
		HashMap<String, List<String>> arsMap = new HashMap<String, List<String>>();
		
		arsMap = instance.getArsMap();

		for (String arsCode : arsMap.keySet()) {
			if (arsCode.equals(referenceAllele)
					&& arsMap.get(arsCode).contains(matchedValue)) {
				return true;
			}
			else if (arsCode.substring(0, arsCode.length() - 1).equals(referenceAllele)
					&& arsMap.get(arsCode).contains(matchedValue)) {
				// TODO:  Revisit for proper handling / stripping of little g
				return true;
			}
		}

		return false;
	}

	public static String convertToProteinLevel(String allele) {
		String[] parts = allele.split(COLON);

		String matchedValue = null;
		if (parts.length > P_GROUP_LEVEL
				&& Pattern.matches(VARIANTS_REGEX,
						"" + allele.charAt(allele.length() - 1))) {
			matchedValue = parts[0] + COLON + parts[1]
					+ allele.charAt(allele.length() - 1);
			LOGGER.finest("Found an SNLQ while comparing ARS: " + allele);
		} else if (parts.length < P_GROUP_LEVEL) {
			 if (!allele.equals(GLStringConstants.NNNN)) {
				 LOGGER.warning("Unexpected allele: " + allele);
			 }
		} else {
			matchedValue = parts[0] + COLON + parts[1];
		}
		return matchedValue;
	}

	// TODO:  Fix homozygous checker - not dealing with genotypic ambiguity appropriately (S2 - DRB4 example)
	public static boolean checkHomozygous(List<List<String>> alleles) {
		if (alleles == null) {
			return false;
		}
		
		if (alleles.size() <= 1) {
			return true;
		}

		int i=0;
		int j=0;
		
		for (List<String> haplotypeAlleles : alleles) {
			j=0;
			for (List<String> haplotypeAllelesLoop : alleles) {
				if (i != j && haplotypeAlleles.containsAll(haplotypeAllelesLoop)) {
					return true;
				}
				j++;
			}
			i++;
		}

		return false;
	}

	public static String fullyQualifyGLString(String shorthand) {
		StringTokenizer st = new StringTokenizer(shorthand,
				GL_STRING_DELIMITER_REGEX, true);
		StringBuffer sb = new StringBuffer();
		String part;
		String locus = null;

		while (st.hasMoreTokens()) {
			part = st.nextToken();
			if (part.substring(0, 1).matches(ALPHA_REGEX)) {
				if (!part.startsWith(GLStringConstants.HLA_DASH)) {
					part = GLStringConstants.HLA_DASH + part;
				}

				String[] splitString = part.split(ESCAPED_ASTERISK);
				locus = splitString[0];
			} else if (part.substring(0, 1).matches(GL_STRING_DELIMITER_REGEX)) {
				sb.append(part);
				continue;
			} else {
				part = fillLocus(Locus.lookup(locus), part);
			}

			sb.append(part);
		}

		return sb.toString();
	}

	public static String fillLocus(Locus locus, String segment) {
		if (!segment.substring(0, 1).matches(ALPHA_REGEX)) {
			segment = locus + GLStringConstants.ASTERISK + segment;
		}
		return segment;
	}
	
	public static LinkedHashMap<String, String> readGLStringFile(String name, BufferedReader reader) {
		LinkedHashMap<String, String> glStrings = null;
		
		try {
			glStrings = parseGLStringFile(name, reader);
		} catch (IOException e) {
			LOGGER.severe("Problem reading GL String file: " + name);
			e.printStackTrace();
		}
		
		return glStrings;
	}

	public static LinkedHashMap<String, String> readGLStringFile(String filename) {
		BufferedReader reader = null;
		LinkedHashMap<String, String> glStrings = null;

		try {
			InputStream stream = GLStringUtilities.class.getClassLoader()
					.getResourceAsStream(filename);
			if (stream == null) {
				stream = new FileInputStream(filename);
			}
			
			reader = new BufferedReader(new InputStreamReader(stream));

			glStrings = parseGLStringFile(filename, reader);
			
		} catch (FileNotFoundException e) {
			LOGGER.severe("Couldn't find GL String file: " + filename);
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.severe("Problem opening GL String file: " + filename);
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				LOGGER.severe("Problem closing reader/stream.");
				e.printStackTrace();
			}
		}

		return glStrings;
	}

	private static LinkedHashMap<String, String> parseGLStringFile(String filename,
			BufferedReader reader)
			throws IOException {
		LinkedHashMap<String, String> glStrings = new LinkedHashMap<String, String>();
		String line;
		String[] parts = null;
		int lineNumber = 0;
		while ((line = reader.readLine()) != null) {
			parts = line.split(FILE_DELIMITER_REGEX);
			if (parts.length == 1) {
				glStrings.put(filename + "-" + lineNumber, parts[0]);
			} else if (parts.length == 2) {
				glStrings.put(parts[0], parts[1]);
			} else {
				LOGGER.warning("Unexpected line format at line "
						+ lineNumber + ": " + filename);
			}

			lineNumber++;
		}
		
		return glStrings;
	}

	public static MultilocusUnphasedGenotype convertToMug(String glString) {
		MultilocusUnphasedGenotype mug = null;

		try {
			// TODO: should use strict but example GL Strings are missing intron
			// variants in some cases (HLA-DQB1*02:02)
			// GlClient glClient = LocalGlClient.createStrict();

			GlClient glClient = LocalGlClient.create();
			mug = glClient.createMultilocusUnphasedGenotype(glString);
		} catch (GlClientException e) {
			LOGGER.severe("Couldn't convert GLString to MultiLocusUnphasedGenotype");
			e.printStackTrace();
		}

		return mug;
	}
}
