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
package org.dash.valid.wmda;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;

public class WMDASerLoader {    
    private static final String HASH = "#";
    private static final String SEMICOLON = ";";

	private static WMDASerLoader instance = null;
	
	private HashMap<String, List<String>> serMap = new HashMap<String, List<String>>();

	private WMDASerLoader() {
		init();
	}
	
	public static WMDASerLoader getInstance() {
		if (instance == null) {
			instance = new WMDASerLoader();
		}
		
		return instance;
	}
	
	private void init() {
		try {
			loadWmdaSerAlleles();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void loadWmdaSerAlleles() throws IOException, FileNotFoundException {
		HashMap<String, List<String>> serMap = new HashMap<String, List<String>>();	
		URL url = new URL("https://raw.githubusercontent.com/ANHIG/IMGTHLA/Latest/wmda/rel_ser_ser.txt");
				
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		String row;
		String[] columns;
		
		List<String> antigens;
		String[] parts;
		
		Locus locus;
		
		while ((row = reader.readLine()) != null) {
			if (row.startsWith(HASH)) continue;
			
			columns = row.split(SEMICOLON);
			antigens = new ArrayList<String>();

			for (int i=1;i<columns.length;i++) {
				if (columns[i].equals(GLStringConstants.EMPTY_STRING)) continue;
				
				parts = columns[i].split(GLStringConstants.ALLELE_AMBIGUITY_DELIMITER);
				antigens.addAll(Arrays.asList(parts));
			}
			
			locus = Locus.lookup(columns[0]);
			
			if (locus == null) continue;
	
			serMap.put(locus + GLStringConstants.ASTERISK + columns[1], antigens);
		}
		
		setSerMap(serMap);
				
		reader.close();
	}
	
	public HashMap<String, List<String>> getSerMap() {
		return this.serMap;
	}
	
	private void setSerMap(HashMap<String, List<String>> serMap) {
		this.serMap = serMap;
	}
}
