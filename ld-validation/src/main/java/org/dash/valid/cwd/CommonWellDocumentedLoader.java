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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.gl.GLStringConstants;

public class CommonWellDocumentedLoader {
	private static final String NOT_APPLICABLE = "NA";
	private static CommonWellDocumentedLoader instance = null;
	
	private Set<String> cwdAlleles;
	
	private HashMap<String, String> cwdByAccession;
	private HashMap<String, List<String>> hlaDbByAccession;
	
	public HashMap<String, String> getCwdByAccession() {
		return cwdByAccession;
	}

	private void setCwdByAccession(HashMap<String, String> cwdByAccession) {
		this.cwdByAccession = cwdByAccession;
	}

	public HashMap<String, List<String>> getHlaDbByAccession() {
		return hlaDbByAccession;
	}

	private void setHlaDbByAccession(HashMap<String, List<String>> hlaDbByAccession) {
		this.hlaDbByAccession = hlaDbByAccession;
	}

	private CommonWellDocumentedLoader(HLADatabaseVersion hladb) {
		init(hladb);
	}
	
	public static CommonWellDocumentedLoader getInstance() {
		HLADatabaseVersion hladb = null;
		if (instance == null) {
			hladb = HLADatabaseVersion.lookup(System.getProperty(HLADatabaseVersion.HLADB_PROPERTY));
			instance = new CommonWellDocumentedLoader(hladb);
		}
		
		return instance;
	}
	
	private void init(HLADatabaseVersion hladb) {
		try {
			loadCommonWellDocumentedAlleles(hladb);
		}
		catch (FileNotFoundException e) {
			
		}
		catch (IOException e) {
			
		}
	}
	
	public void loadCommonWellDocumentedAlleles(HLADatabaseVersion hladb) throws IOException, FileNotFoundException {		
		String filename = "reference/CWD.txt";
		
		HashMap<String, String> cwdByAccession = new HashMap<String, String>();
		HashMap<String, List<String>> hlaDbByAccession = new HashMap<String, List<String>>();
		List<String> hladbs;
		Set<String> cwdSet = new HashSet<String>();
				
		BufferedReader reader = new BufferedReader(new InputStreamReader(CommonWellDocumentedLoader.class.getClassLoader().getResourceAsStream(filename)));
		String row;
		String[] columns;
		List<String> headers = null;
		int idx = 0;
		
		int hladbIdx = -1;
				
		while ((row = reader.readLine()) != null) {
			hladbs = new ArrayList<String>();
			columns = row.split(GLStringConstants.TAB);
			cwdByAccession.put(columns[0], GLStringConstants.HLA_DASH + columns[1]);
			
			if (idx < 1) {
				headers = Arrays.asList(columns);

				hladbIdx = headers.indexOf(hladb.getCwdName());			}
			else {
				cwdSet.add(GLStringConstants.HLA_DASH + columns[hladbIdx]);
			}

			for (int i=0;i<columns.length-1;i++) {
				if (!columns[i+1].equals(NOT_APPLICABLE)) {
					hladbs.add(headers.get(i+1));
				}
			}
			
			hlaDbByAccession.put(columns[0], hladbs);
			idx++;
		}
		
		setHlaDbByAccession(hlaDbByAccession);
		setCwdByAccession(cwdByAccession);
		setCwdAlleles(cwdSet);
				
		reader.close();
	}
	
	public Set<String> getCwdAlleles() {
		return this.cwdAlleles;
	}

	private void setCwdAlleles(Set<String> cwdAlleles) {
		this.cwdAlleles = cwdAlleles;
	}
	
	public String getAccessionByAllele(String allele) {
		if (!getCwdByAccession().containsValue(allele)) {
			return null;
		}
		
		for (String key : getCwdByAccession().keySet()) {
			if (getCwdByAccession().get(key).equals(allele)) {
				return key;
			}
		}
		
		return null;
	}
	
	public List<String> getHlaDbsByAccession(String accession) {
		return getHlaDbByAccession().get(accession);
	}
}
