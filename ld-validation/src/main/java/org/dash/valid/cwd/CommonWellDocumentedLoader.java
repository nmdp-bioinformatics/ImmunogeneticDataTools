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
package org.dash.valid.cwd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CommonWellDocumentedLoader {
    private static final Logger LOGGER = Logger.getLogger(CommonWellDocumentedLoader.class.getName());

	private static CommonWellDocumentedLoader instance = null;
	
	private Set<String> cwdAlleles = new HashSet<String>();
	private Set<String> ciwdAlleles = new HashSet<String>();

	private HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();
	private HashMap<String, HashMap<String, String>> ciwdMap = new HashMap<String, HashMap<String, String>>();

	private static String instanceHladb;

	private CommonWellDocumentedLoader(String hladb) {
		init(hladb);
	}
	
	public static CommonWellDocumentedLoader getInstance() {
		String hladb = System.getProperty(GLStringConstants.HLADB_PROPERTY);
		if (hladb == null) hladb = GLStringConstants.LATEST_HLADB;

		if (instance == null || !hladb.equals(instanceHladb)) {
			instanceHladb = hladb;
			instance = new CommonWellDocumentedLoader(hladb);
		}
		
		return instance;
	}
	
	private void init(String hladb) {
		try {
			loadCommonWellDocumentedAlleles(hladb);
			loadCommonIntermediateWellDocumentedAlleles();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public HashMap<String, List<String>> loadFromIMGT(String hladb) throws IOException, ParserConfigurationException, SAXException {
		HashMap<String, List<String>> accessionMap = new HashMap<String, List<String>>();

		URL url = new URL("https://raw.githubusercontent.com/ANHIG/IMGTHLA/" + hladb.replace(GLStringConstants.PERIOD, GLStringConstants.EMPTY_STRING) + "/xml/hla.xml.zip");
				
		ZipInputStream zipStream = new ZipInputStream(url.openStream());
		zipStream.getNextEntry();
		BufferedReader reader = new BufferedReader(new InputStreamReader(zipStream));
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(reader);
	    Document doc = builder.parse(is);
	    
	    String name;
	    String accession;
	    List<String> accessionList;

	    NodeList nList = doc.getElementsByTagName("allele");
	    for (int i=0;i<nList.getLength();i++) {
	    	name = nList.item(i).getAttributes().getNamedItem("name").getNodeValue();
	    	accession = nList.item(i).getAttributes().getNamedItem("id").getNodeValue();
	    	
	    	if (accessionMap.containsKey(GLStringUtilities.convertToProteinLevel(name))) {
	    		accessionList = accessionMap.get(GLStringUtilities.convertToProteinLevel(name));
	    	}
	    	else {
	    		accessionList = new ArrayList<String>();
	    	}

	    	accessionList.add(accession);
	    	accessionMap.put(GLStringUtilities.convertToProteinLevel(name), accessionList);	    	
	    }
	    
	    return accessionMap;
	}
	
	public void loadCommonWellDocumentedAlleles(String hladb) throws IOException, FileNotFoundException {
		Set<String> cwdSet = new HashSet<String>();
		HashMap<String, List<String>> accessionMap = null;
		boolean accessionLoaded = false;
				
		// TODO:  Why bailing on LATEST HLADB here?
		if (hladb == null || GLStringConstants.LATEST_HLADB.equals(hladb)) return;

		try {
			accessionMap = loadFromIMGT(hladb);
		}
		catch (IOException | ParserConfigurationException | SAXException e) {
			LOGGER.info("Could not load file from IMGT for hladb: " + hladb);	
		}	
		
		if (accessionMap != null && accessionMap.size() > 0) {
			accessionLoaded = true;
		}
		else {
			accessionMap = new HashMap<String, List<String>>();
		}
		
		String filename = "reference/CWD.txt";
				
		BufferedReader reader = new BufferedReader(new InputStreamReader(CommonWellDocumentedLoader.class.getClassLoader().getResourceAsStream(filename)));
		String row;
		String[] columns;
		int idx = 0;
		
		int hladbIdx = -1;
		List<String> headers = null;
		List<String> accessionList;		
		
		while ((row = reader.readLine()) != null) {
			columns = row.split(GLStringConstants.TAB);

			if (idx < 1) {
				headers = Arrays.asList(columns);

				hladbIdx = headers.indexOf(hladb.replace(GLStringConstants.PERIOD, GLStringConstants.EMPTY_STRING));	
				
				if (hladbIdx == -1) {
					hladbIdx = 1;
					LOGGER.warning("CWD reference file is not updated with the specified HLADB.  Defaulting to the latest HLADB specified in the reference file: " + columns[hladbIdx]);
				}
			}
			else {
				cwdSet.add(columns[0]);
				// TODO:  if (accessionLoaded) continue??
				if (!accessionLoaded) {
					
			    	if (accessionMap.containsKey(GLStringUtilities.convertToProteinLevel(GLStringConstants.HLA_DASH + columns[hladbIdx]))) {
			    		accessionList = accessionMap.get(GLStringUtilities.convertToProteinLevel(GLStringConstants.HLA_DASH + columns[hladbIdx]));
			    	}
			    	else {
			    		accessionList = new ArrayList<String>();
			    	}
			    	
			    	accessionList.add(columns[0]);
			    	accessionMap.put(GLStringUtilities.convertToProteinLevel(GLStringConstants.HLA_DASH + columns[hladbIdx]), accessionList);
				}
			}
			
			idx++;
		}
		
		setCwdAlleles(cwdSet);
		setAccessionMap(accessionMap);
				
		reader.close();
	}
	
	public void loadCommonIntermediateWellDocumentedAlleles() throws IOException, FileNotFoundException {
		HashMap<String, HashMap<String, String>> ciwdMap = new HashMap<String, HashMap<String, String>>();
		Set<String> ciwdAlleles = new HashSet<String>();
		
		String filename = "reference/CIWD.txt";
				
		BufferedReader reader = new BufferedReader(new InputStreamReader(CommonWellDocumentedLoader.class.getClassLoader().getResourceAsStream(filename)));
		String row;
		String[] columns;
		int idx = 0;
		
		String[] headers = null;
		HashMap<String, String> singleAlleleCiwdMap;
		
		while ((row = reader.readLine()) != null) {
			columns = row.split(GLStringConstants.COMMA);

			if (idx < 1) {
				headers = columns;
			}
			else {
				singleAlleleCiwdMap = new HashMap<String, String>();
				
				for (int i=1;i<columns.length;i++) {
					singleAlleleCiwdMap.put(headers[i], columns[i]);	
				}
				
				// This map will be populated with all alleles and the columns associated - in case fuller reporting is desired - not currently implemented...
				ciwdMap.put(GLStringConstants.HLA_DASH + columns[0], singleAlleleCiwdMap);
				
				// This set will be populated with just the protein level values
				ciwdAlleles.add(GLStringUtilities.convertToProteinLevel(GLStringConstants.HLA_DASH + columns[0]));
			}
			
			idx++;
		}
		
		setCiwdMap(ciwdMap);
		setCiwdAlleles(ciwdAlleles);
				
		reader.close();
	}
	
	public Set<String> getCwdAlleles() {
		return this.cwdAlleles;
	}

	private void setCwdAlleles(Set<String> cwdAlleles) {
		this.cwdAlleles = cwdAlleles;
	}
	
	public Set<String> getCiwdAlleles() {
		return this.ciwdAlleles;
	}

	private void setCiwdAlleles(Set<String> ciwdAlleles) {
		this.ciwdAlleles = ciwdAlleles;
	}
	
	public HashMap<String, List<String>> getAccessionMap() {
		return this.accessionMap;
	}
	
	private void setAccessionMap(HashMap<String, List<String>> accessionMap) {
		this.accessionMap = accessionMap;
	}
	
	public HashMap<String, HashMap<String, String>> getCiwdMap() {
		return ciwdMap;
	}

	private void setCiwdMap(HashMap<String, HashMap<String, String>> ciwdMap) {
		this.ciwdMap = ciwdMap;
	}
}
