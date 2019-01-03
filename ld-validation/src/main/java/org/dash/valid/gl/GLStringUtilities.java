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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.dash.valid.Locus;
import org.dash.valid.ars.AntigenRecognitionSiteLoader;
import org.dash.valid.cwd.CommonWellDocumentedLoader;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.MultiLocusHaplotype;
import org.dash.valid.gl.haplo.SingleLocusHaplotype;
import org.nmdp.gl.MultilocusUnphasedGenotype;
import org.nmdp.gl.client.GlClient;
import org.nmdp.gl.client.GlClientException;
import org.nmdp.gl.client.local.LocalGlClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
	
	public static String getLatestImgtRelease() {
		HttpURLConnection connection = null;
		String imgtRelease = null;

		try {
			URL url = new URL("https://hml.nmdp.org/mac/api/imgtHlaReleases");
			
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			
			InputStream xml = connection.getInputStream();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(xml));
			imgtRelease = reader.readLine().split(GLStringConstants.SPACE)[0];
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			connection.disconnect();
		}
		
		return imgtRelease;
	}
	
	public static String decodeMAC(String typing) {
		String decodedValue = null;
		HttpURLConnection connection = null;
		
		try {
			String uri = "https://hml.nmdp.org/mac/api/decode/?";
			String imgtRelease = System.getProperty(GLStringConstants.HLADB_PROPERTY);
			if (imgtRelease == null || GLStringConstants.LATEST_HLADB.equals(imgtRelease)) {
				imgtRelease = getLatestImgtRelease();
				//System.setProperty(GLStringConstants.HLADB_PROPERTY, imgtRelease);
			}
			URL url = new URL(uri + "imgtHlaRelease=" + imgtRelease + "&typing=" + typing + "&expand=false");
			
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			
			InputStream xml = connection.getInputStream();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(xml));
			decodedValue = reader.readLine();			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			connection.disconnect();
		}
		
		return decodedValue;
	}
	
	public static List<Haplotype> buildHaplotypes(LinkageDisequilibriumGenotypeList linkedGlString) {
		String glString = linkedGlString.getGLString();
		List<Haplotype> knownHaplotypes = new CopyOnWriteArrayList<Haplotype>();
		HashMap<String, Locus> locusMap = new HashMap<String, Locus>();
		Locus locus = null;
		
		if (StringUtils.countMatches(glString, GLStringConstants.GENE_PHASE_DELIMITER) > 1 && StringUtils.countMatches(glString,  GLStringConstants.GENE_COPY_DELIMITER) == 1) {
			List<String> genes = GLStringUtilities.parse(glString,
					GLStringConstants.GENE_DELIMITER);
			for (String gene : genes) {
				List<String> genotypeAmbiguities = GLStringUtilities.parse(gene,
						GLStringConstants.GENOTYPE_AMBIGUITY_DELIMITER);
				for (String genotypeAmbiguity : genotypeAmbiguities) {
					List<String> geneCopies = GLStringUtilities.parse(
							genotypeAmbiguity,
							GLStringConstants.GENE_COPY_DELIMITER);
					
					int i=0;

					for (String geneCopy : geneCopies) {
						HashMap<Locus,SingleLocusHaplotype> singleLocusHaplotypes = new HashMap<Locus, SingleLocusHaplotype>();

						List<String> genePhases = GLStringUtilities.parse(geneCopy,
								GLStringConstants.GENE_PHASE_DELIMITER);
						for (String genePhase : genePhases) {
							String[] splitString = genePhase
									.split(GLStringUtilities.ESCAPED_ASTERISK);
							String locusVal = splitString[0];
							
							List<String> alleleAmbiguities = GLStringUtilities
									.parse(genePhase,
											GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
							
							if (locusMap.containsKey(locusVal)) {
								locus = locusMap.get(locusVal);
							}
							else {
								locus = Locus.normalizeLocus(Locus.lookup(locusVal));
								locusMap.put(locusVal, locus);
							}

							SingleLocusHaplotype haplotype = new SingleLocusHaplotype(locus, alleleAmbiguities, i);
							singleLocusHaplotypes.put(locus, haplotype);

						}
						
						MultiLocusHaplotype multiLocusHaplotype = new MultiLocusHaplotype(singleLocusHaplotypes, linkedGlString.hasHomozygous(Locus.HLA_DRB345));
						multiLocusHaplotype.setSequence(i + 1);
						knownHaplotypes.add(multiLocusHaplotype);
						
						i++;
					}
				}
			}			
		}
		
		return knownHaplotypes;
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
			
			if (parts[1].substring(0,1).matches(ALPHA_REGEX)) {
				LOGGER.info("GLString contains allele codes.  These will be decoded.");
				return false;
			}
		}

		return true;
	}

	public static Set<String> checkCommonWellDocumented(String glString) {
		Set<String> notCommon = new HashSet<String>();
		
		CommonWellDocumentedLoader loader = CommonWellDocumentedLoader.getInstance();
		
		Set<String> cwdAlleles = loader.getCwdAlleles();

		if (cwdAlleles.size() == 0) return new HashSet<String>();
		
		HashMap<String, String> accessionMap = loader.getAccessionMap();

		StringTokenizer st = new StringTokenizer(glString,
				GL_STRING_DELIMITER_REGEX);
		String token;
		while (st.hasMoreTokens()) {
			token = st.nextToken();
			
			if (!cwdAlleles.contains(accessionMap.get(token))) {
				notCommon.add(token);
			}
		}

		return notCommon;
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
				
		AntigenRecognitionSiteLoader instance = null;
		
		try {
			instance = AntigenRecognitionSiteLoader.getInstance();
		}
		catch (IOException | InvalidFormatException e) {
			LOGGER.warning("Could not load ars data.");
			e.printStackTrace();
		}
		
		HashMap<String, HashSet<String>> arsMap = instance.getArsMap();

		if (arsMap.containsKey(referenceAllele)) {
			for (String arsCode : arsMap.keySet()) {
				if (arsCode.equals(referenceAllele)
						&& arsMap.get(arsCode).contains(matchedValue)) {
					return true;
				}
				
    			// TODO:  Not sure this accomplished anything...remove?

//				else if (arsCode.substring(0, arsCode.length() - 1).equals(referenceAllele)
//						&& arsMap.get(arsCode).contains(matchedValue)) {
//					// TODO:  Does this ever happen?
//					return true;
//				}
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
		String[] segments;
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
			
			segments = part.split(COLON);
			
			if (segments.length > 1 && segments[1].substring(0,1).matches(ALPHA_REGEX)) {
				part = decodeMAC(part);
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
	
	public static List<LinkageDisequilibriumGenotypeList> readGLStringFile(String name, BufferedReader reader) {
		List<LinkageDisequilibriumGenotypeList> linkedGLStrings = null;
		
		try {
			linkedGLStrings = parseGLStringFile(name, reader);
		} catch (IOException e) {
			LOGGER.severe("Problem reading GL String file: " + name);
			e.printStackTrace();
		}
		 catch (ParserConfigurationException | SAXException e) {
				LOGGER.severe("Couldn't parse xml file: " + name);
				e.printStackTrace();
		 }
		
		return linkedGLStrings;
	}

	public static List<LinkageDisequilibriumGenotypeList> readGLStringFile(String filename) {
		BufferedReader reader = null;
		List<LinkageDisequilibriumGenotypeList> linkedGLStrings = null;

		try {
			InputStream stream = GLStringUtilities.class.getClassLoader()
					.getResourceAsStream(filename);
			if (stream == null) {
				stream = new FileInputStream(filename);
			}
			
			reader = new BufferedReader(new InputStreamReader(stream));

			linkedGLStrings = parseGLStringFile(filename, reader);
			
		} catch (FileNotFoundException e) {
			LOGGER.severe("Couldn't find GL String file: " + filename);
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.severe("Problem opening GL String file: " + filename);
			e.printStackTrace();
		} catch (SAXException | ParserConfigurationException e) {
			LOGGER.severe("Couldn't parse xml file: " + filename);
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				LOGGER.severe("Problem closing reader/stream.");
				e.printStackTrace();
			}
		}
		
		return linkedGLStrings;
	}

	private static List<LinkageDisequilibriumGenotypeList> parseGLStringFile(String filename,
			BufferedReader reader)
			throws IOException, ParserConfigurationException, SAXException {
		List<LinkageDisequilibriumGenotypeList> linkedGLStrings = new ArrayList<LinkageDisequilibriumGenotypeList>();


		if (filename.endsWith(GLStringConstants.XML) || filename.endsWith(GLStringConstants.HML)) {
		    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = factory.newDocumentBuilder();
		    InputSource is = new InputSource(reader);
		    Document doc = builder.parse(is);
		    String sampleId;
		    Element alleleAssignment;

		    NodeList nList = doc.getElementsByTagName(GLStringConstants.SAMPLE_ELEMENT);
		    for (int i=0;i<nList.getLength();i++) {
		    	sampleId = nList.item(i).getAttributes().getNamedItem(GLStringConstants.ID_ATTRIBUTE).getNodeValue();
			    StringBuffer glString = new StringBuffer();
		    	NodeList typingElements = ((Element) nList.item(i)).getElementsByTagName(GLStringConstants.TYPING_ELEMENT);
		    	for (int j=0;j<typingElements.getLength();j++) {
		    		alleleAssignment = (Element) ((Element) typingElements.item(j)).getElementsByTagName(GLStringConstants.ALLELE_ASSIGNMENT_ELEMENT).item(0);
		    		if (j > 0) glString.append(GLStringConstants.GENE_DELIMITER);
		    		glString.append(((Element) alleleAssignment.getElementsByTagName(GLStringConstants.GL_STRING_ELEMENT).item(0)).getTextContent().trim());
		    	}
		    	
		    	linkedGLStrings.add(inflateGenotypeList(sampleId, glString.toString(), null));
		    }
		}
		else {
			String line;
			String[] parts = null;
			int lineNumber = 0;
			String glString;
			String id;
			String note = null;			
			
			while ((line = reader.readLine()) != null) {
				lineNumber++;

				parts = line.split(FILE_DELIMITER_REGEX);
				
				if (parts.length == 1) {
					id = filename + "-" + (lineNumber - 1);
					glString = parts[0];
				} else if (parts.length >= 2) {
					id = parts[0];
					glString = parts[1];
					
					if (parts.length == 3) note = parts[2];
				}
				else {
					LOGGER.warning("Unexpected line format at line "
							+ (lineNumber - 1) + ": " + filename);
					
					continue;
				}
				
				linkedGLStrings.add(inflateGenotypeList(id, glString, note));
					
			}
		}
				
		return linkedGLStrings;
	}
	
	private static LinkageDisequilibriumGenotypeList inflateGenotypeList(String id, String glString, String note) {
		LinkageDisequilibriumGenotypeList linkedGLString;
		
		String submittedGlString = glString;
		
		if (!GLStringUtilities.validateGLStringFormat(glString)) {
			glString = GLStringUtilities.fullyQualifyGLString(glString);
		}
		
		MultilocusUnphasedGenotype mug = GLStringUtilities.convertToMug(glString);
		linkedGLString = new LinkageDisequilibriumGenotypeList(id, mug);
		
		linkedGLString.setSubmittedGlString(submittedGlString);
		linkedGLString.setNote(note);
		
		return linkedGLString;
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
