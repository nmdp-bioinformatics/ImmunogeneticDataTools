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
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class AntigenRecognitionSiteLoader {
	private static AntigenRecognitionSiteLoader instance = null;
	HashMap<String, HashSet<String>> arsMap = new HashMap<String, HashSet<String>>();

    private static final Logger LOGGER = Logger.getLogger(AntigenRecognitionSiteLoader.class.getName());
    
    private static final String DEFAULT_ARS_FILE = "reference/mmc1.xls";
	
	private AntigenRecognitionSiteLoader() {
	}
	
	public HashMap<String, HashSet<String>> getArsMap() {
		return this.arsMap;
	}
	
	public static AntigenRecognitionSiteLoader getInstance() throws IOException, InvalidFormatException {
		String hladb = null;
		if (instance == null) {
			try {
				String ars = System.getProperty(GLStringConstants.ARS_PROPERTY);
				if (ars != null && ars.equals(GLStringConstants.ARS_DEFAULT)) {
					instance = new AntigenRecognitionSiteLoader();
					instance.init();
				}
				else {
					instance = new AntigenRecognitionSiteLoader();
					hladb = System.getProperty(GLStringConstants.HLADB_PROPERTY);
	
					instance.init(hladb);
				}
			}
			catch (IOException | ParserConfigurationException | SAXException e) {
				LOGGER.info("Couldn't find IMGT file in the correct format for hladb: " + hladb);
				instance.init();
				
				// TODO:  Make final determination - commenting this in messes up the CWD logic currently
				//System.setProperty(GLStringConstants.HLADB_PROPERTY, "Default");
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
	
	public static HashMap<String, HashSet<String>> loadGGroups(String hladb) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
		if (hladb == null) hladb = GLStringConstants.LATEST_HLADB;
		URL url = new URL("https://raw.githubusercontent.com/ANHIG/IMGTHLA/" + hladb.replace(GLStringConstants.PERIOD, GLStringConstants.EMPTY_STRING) + "/xml/hla_ambigs.xml.zip");
				
		ZipInputStream zipStream = new ZipInputStream(url.openStream());
		zipStream.getNextEntry();
		BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream));
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(reader);
	    Document doc = builder.parse(is);
	    
	    HashMap<String, HashSet<String>> gAlleleListMap = new HashMap<String, HashSet<String>>();
	    String[] parts;
	    String arsCode;

	    NodeList nList = doc.getElementsByTagName("tns:gene");
	    for (int i=0;i<nList.getLength();i++) {
	    	NodeList gGroups = ((Element) nList.item(i)).getElementsByTagName("tns:gGroup");
	    	
	    	for (int j=0;j<gGroups.getLength();j++) {
		    	HashSet<String> gAlleleList = new HashSet<String>();
	    		String gGroup = gGroups.item(j).getAttributes().getNamedItem("name").getNodeValue();
	    		parts = gGroup.split(GLStringUtilities.COLON);
	    		arsCode = (gGroup.startsWith(GLStringConstants.HLA_DASH)) ? parts[0] + GLStringUtilities.COLON + parts[1] + "g" : GLStringConstants.HLA_DASH + parts[0] + GLStringUtilities.COLON + parts[1] + "g";

	    		NodeList gGroupAlleles = ((Element) gGroups.item(j)).getElementsByTagName("tns:gGroupAllele");
	    		for (int k=0;k<gGroupAlleles.getLength();k++) {
	    			String fullAllele = gGroupAlleles.item(k).getAttributes().getNamedItem("name").getNodeValue();
	    			parts = fullAllele.split(GLStringUtilities.COLON);
	    			String allele = (fullAllele.startsWith(GLStringConstants.HLA_DASH)) ? parts[0] + GLStringUtilities.COLON + parts[1] : GLStringConstants.HLA_DASH + parts[0] + GLStringUtilities.COLON + parts[1];
	    			
	    			// TODO:  Not sure this accomplished anything...keep
	    			if (parts.length > 2 && Pattern.matches("[SNLQ]", "" + fullAllele.charAt(fullAllele.length() - 1))) {
	    				LOGGER.finest("Found an SNLQ during the ARS load: " + fullAllele + " became: " + allele + fullAllele.charAt(fullAllele.length()-1));
	    				allele += fullAllele.charAt(fullAllele.length()-1);
	    			}
	    			gAlleleList.add(allele);
	    		}	
		    	gAlleleListMap.put(arsCode, gAlleleList);

	    	}
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
