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
package org.dash.valid.ars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.xml.sax.SAXException;

public class AntigenRecognitionSiteLoader {
	private static AntigenRecognitionSiteLoader instance = null;
	HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();

    private static final Logger LOGGER = Logger.getLogger(AntigenRecognitionSiteLoader.class.getName());

    private static final String DEFAULT_ARS_FILE = "reference/mmc1.xls";

    // Which hladb the cached instance was built for (null when it's the fixed local ARS_DEFAULT
    // file, which doesn't vary by hladb at all). Phase 8: ld-service can now be asked to analyze
    // against a different hladb per request; without tracking this, a long-running process would
    // silently keep serving whichever hladb's G-groups happened to load first, ignoring every
    // later request's choice. Mirrors the same reload-on-change pattern already used by
    // CommonWellDocumentedLoader for the same reason.
    private static String instanceHladb;

	private AntigenRecognitionSiteLoader() {
	}

	public HashMap<String, HashSet<String>> getArsMap() {
		return this.arsMap;
	}

	public static AntigenRecognitionSiteLoader getInstance() throws IOException, InvalidFormatException {
		String ars = System.getProperty(GLStringConstants.ARS_PROPERTY);
		boolean usesDefaultArs = ars != null && ars.equals(GLStringConstants.ARS_DEFAULT);
		String hladb = usesDefaultArs ? null : System.getProperty(GLStringConstants.HLADB_PROPERTY);

		if (instance == null || !Objects.equals(hladb, instanceHladb)) {
			try {
				if (usesDefaultArs) {
					instance = new AntigenRecognitionSiteLoader();
					instance.init();
				}
				else {
					instance = new AntigenRecognitionSiteLoader();
					instance.init(hladb);
				}
				instanceHladb = hladb;
			}
			catch (IOException | ParserConfigurationException | SAXException e) {
				LOGGER.info("Couldn't find IMGT file in the correct format for hladb: " + hladb);
				instance = new AntigenRecognitionSiteLoader();
				instance.init();
				instanceHladb = null;
			}
		}

		return instance;
	}
	
	private void init(String hladb) throws IOException, ParserConfigurationException, SAXException {
		this.arsMap.putAll(loadGGroups(hladb));
	}
	
	private void init() throws InvalidFormatException, IOException {
		this.arsMap = loadARSData();
	}
	
	// Streams the IMGT/HLA ambiguity XML with StAX (XMLStreamReader) instead of parsing it
	// into a full DOM tree -- same fix, same reason as CommonWellDocumentedLoader.
	// loadFromIMGT: hla_ambigs.xml.zip is even larger than hla.xml.zip (~39MB compressed
	// at the time of writing) and only two attributes per element are actually needed.
	// gGroup/gGroupAllele elements don't nest inside each other (only inside gene, whose
	// own attributes are never used), so tracking "are we inside a gGroup" as a single flag
	// while streaming is enough to reproduce the original nesting-scoped behavior.
	public static HashMap<String, HashSet<String>> loadGGroups(String hladb) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
		if (hladb == null) hladb = GLStringConstants.LATEST_HLADB;
		URL url = new URL("https://raw.githubusercontent.com/ANHIG/IMGTHLA/" + hladb.replace(GLStringConstants.PERIOD, GLStringConstants.EMPTY_STRING) + "/xml/hla_ambigs.xml.zip");

		System.out.println(url.toString());

		HashMap<String, HashSet<String>> gAlleleListMap = new HashMap<String, HashSet<String>>();

		try (ZipInputStream zipStream = new ZipInputStream(url.openStream())) {
			zipStream.getNextEntry();

			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			// StAX defaults to namespace-aware parsing, which would strip the "tns:" prefix
			// off getLocalName(). The original DOM parser here used the JDK default (non
			// namespace-aware), matching "tns:gGroup" etc. as one literal tag name -- kept
			// that same matching behavior rather than resolving the namespace properly, to
			// keep this a pure parsing-strategy change with no behavior difference.
			inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
			XMLStreamReader reader = inputFactory.createXMLStreamReader(zipStream);

			try {
				String[] parts;
				String arsCode = null;
				HashSet<String> gAlleleList = null;

				while (reader.hasNext()) {
					int event = reader.next();

					if (event == XMLStreamConstants.START_ELEMENT && "tns:gGroup".equals(reader.getLocalName())) {
						String gGroup = reader.getAttributeValue(null, "name");
						parts = gGroup.split(GLStringUtilities.COLON);

						if (parts.length < 2) {
							arsCode = null;
							gAlleleList = null;
							continue;
						}

						arsCode = (gGroup.startsWith(GLStringConstants.HLA_DASH)) ? parts[0] + GLStringUtilities.COLON + parts[1] + "g" : GLStringConstants.HLA_DASH + parts[0] + GLStringUtilities.COLON + parts[1] + "g";

						// currently implementing NMDP 'hack' to deal with historical typings associated with re-named allele - consistent with HaploStats
						if (arsCode.equals("HLA-C*02:10g")) arsCode = "HLA-C*02:02g";

						gAlleleList = gAlleleListMap.containsKey(arsCode) ? gAlleleListMap.get(arsCode) : new HashSet<String>();
					}
					else if (event == XMLStreamConstants.START_ELEMENT && "tns:gGroupAllele".equals(reader.getLocalName()) && gAlleleList != null) {
						String fullAllele = reader.getAttributeValue(null, "name");
						parts = fullAllele.split(GLStringUtilities.COLON);
						String allele = (fullAllele.startsWith(GLStringConstants.HLA_DASH)) ? parts[0] + GLStringUtilities.COLON + parts[1] : GLStringConstants.HLA_DASH + parts[0] + GLStringUtilities.COLON + parts[1];

						if (parts.length > 2 && Pattern.matches("[SNLQ]", "" + fullAllele.charAt(fullAllele.length() - 1))) {
							LOGGER.finest("Found an SNLQ during the ARS load: " + fullAllele + " became: " + allele + fullAllele.charAt(fullAllele.length()-1));
							allele += fullAllele.charAt(fullAllele.length()-1);
						}
						gAlleleList.add(allele);
					}
					else if (event == XMLStreamConstants.END_ELEMENT && "tns:gGroup".equals(reader.getLocalName())) {
						if (arsCode != null) {
							gAlleleListMap.put(arsCode, gAlleleList);
						}
						arsCode = null;
						gAlleleList = null;
					}
				}
			}
			finally {
				reader.close();
			}
		}
		catch (XMLStreamException e) {
			throw new IOException("Problem streaming IMGT/HLA ambiguity XML for hladb: " + hladb, e);
		}

		return gAlleleListMap;
	}
	
	private static HashMap<String, HashSet<String>> loadARSData() throws InvalidFormatException, IOException {
		Workbook workbook = null;

		workbook = WorkbookFactory.create(AntigenRecognitionSiteLoader.class.getClassLoader().getResourceAsStream(DEFAULT_ARS_FILE));
	       
        // Return first sheet from the XLSX workbook
        Sheet mySheet = workbook.getSheetAt(0);
       
        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        
        String gCode;
        String alleleString;
        HashSet<String> alleles;
		HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();
                
        // Traversing over each row of XLSX file
        while (rowIterator.hasNext()) {
        	alleles = new HashSet<String>();
            Row row = rowIterator.next();
            gCode = row.getCell(0).getStringCellValue();
            if (gCode.contains(GLStringConstants.ASTERISK)) {
            	alleleString = row.getCell(1).getStringCellValue();
            	String[] parts = alleleString.split(GLStringConstants.COMMA);
            	for (String part : parts) {
            		alleles.add(GLStringConstants.HLA_DASH + part);
            	}
            	
        		arsMap.put(GLStringConstants.HLA_DASH + gCode, alleles);
            }           
        }
        
        workbook.close();
        
		return arsMap;
	}
}
