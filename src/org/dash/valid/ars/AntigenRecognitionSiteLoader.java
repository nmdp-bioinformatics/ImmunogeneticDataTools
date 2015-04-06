package org.dash.valid.ars;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;

public class AntigenRecognitionSiteLoader {
	private static AntigenRecognitionSiteLoader instance = null;
	private HashMap<String, Set<String>> bArsMap;
	private HashMap<String, Set<String>> cArsMap;
	private HashMap<String, Set<String>> drb1ArsMap;
	private HashMap<String, Set<String>> drb3ArsMap;
	private HashMap<String, Set<String>> drb4ArsMap;
	private HashMap<String, Set<String>> drb5ArsMap;
	private HashMap<String, Set<String>> dqb1ArsMap;
	
	private static final String[] loci = {GLStringConstants.HLA_B, 
											GLStringConstants.HLA_C, 
											GLStringConstants.HLA_DRB1, 
											GLStringConstants.HLA_DRB3,
											GLStringConstants.HLA_DRB4,
											GLStringConstants.HLA_DRB5,
											GLStringConstants.HLA_DQB1};

    private static final Logger LOGGER = Logger.getLogger(AntigenRecognitionSiteLoader.class.getName());
	
	private AntigenRecognitionSiteLoader(HLADatabaseVersion hladb) {
		init(hladb);
	}
	
	public static AntigenRecognitionSiteLoader getInstance() {
		HLADatabaseVersion hladb = null;
		if (instance == null) {
			hladb = HLADatabaseVersion.lookup(System.getProperty("org.dash.hladb"));
			instance = new AntigenRecognitionSiteLoader(hladb);
		}

		return instance;
	}
	
	private void init(HLADatabaseVersion hladb) {
		for (String locus : loci) {
			HashMap<String, Set<String>> arsMap = loadARSData(hladb, locus);
			switch (locus) {
			case GLStringConstants.HLA_B:
				setbArsMap(arsMap);
				break;
			case GLStringConstants.HLA_C:
				setcArsMap(arsMap);
				break;
			case GLStringConstants.HLA_DRB1:
				setDrb1ArsMap(arsMap);
				break;
			case GLStringConstants.HLA_DRB3:
				setDrb3ArsMap(arsMap);
				break;
			case GLStringConstants.HLA_DRB4:
				setDrb4ArsMap(arsMap);
				break;
			case GLStringConstants.HLA_DRB5:
				setDrb5ArsMap(arsMap);
				break;
			case GLStringConstants.HLA_DQB1:
				setDqb1ArsMap(arsMap);
				break;
			}
		}
	}
	
	private static HashMap<String, Set<String>> loadARSData(HLADatabaseVersion hladb, String locus) {
		BufferedReader reader = null;
		HashMap<String, Set<String>> arsMap = new HashMap<String, Set<String>>();
		
		String[] parts = locus.split(GLStringConstants.DASH);
		String filename = "resources/reference/" + hladb.getArsName() + "/" + parts[1] + ".txt";
		
		try {
			File arsFile = new File(filename);
			
			InputStream in = new FileInputStream(arsFile);
			reader = new BufferedReader(new InputStreamReader(in));
			
			String row;
			
			while ((row = reader.readLine()) != null) {
				arsMap.putAll(assembleARSMap(row));
			}
		}
		catch (FileNotFoundException e) {
			LOGGER.severe("Could not open ARS file: " + filename);
		}
		catch (IOException e) {
			LOGGER.severe("Could not read ARS file: " + filename);
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				LOGGER.severe("Could not close reader");
			}
		}
		
		return arsMap;
	}

	/**
	 * @param arsMap
	 * @param row
	 */
	private static HashMap<String, Set<String>> assembleARSMap(String row) {
		String[] parts;
		String[] columns;
		String arsCode;
		Set<String> alleles = new HashSet<String>();
		String allele;
		
		HashMap<String, Set<String>> arsMap = new HashMap<String, Set<String>>();

		columns = row.split(GLStringConstants.TAB);
		parts = columns[0].split(GLStringUtilities.COLON);
		
		arsCode = GLStringConstants.HLA_DASH + parts[0] + GLStringUtilities.COLON + parts[1];
		if (columns.length > 2) {
			arsCode += "g";
		}
		
		for (int i=1;i<columns.length;i++) {
			parts = columns[i].split(GLStringUtilities.COLON);
			allele = GLStringConstants.HLA_DASH + parts[0] + GLStringUtilities.COLON + parts[1];
			if (parts.length > 2 && Pattern.matches("[SNLQ]", "" + columns[i].charAt(columns[i].length() - 1))) {
				LOGGER.finest("Found an SNLQ during the ARS load: " + columns[i] + " became: " + allele + columns[i].charAt(columns[i].length()-1));
				alleles.add(allele + columns[i].charAt(columns[i].length()-1));
			}
			else {
				alleles.add(allele);
			}
		}
		
		arsMap.put(arsCode, alleles);
		
		return arsMap;
	}
	
    public HashMap<String, Set<String>> getbArsMap() {
		return bArsMap;
	}

	private void setbArsMap(HashMap<String, Set<String>> bArsMap) {
		this.bArsMap = bArsMap;
	}

	public HashMap<String, Set<String>> getcArsMap() {
		return cArsMap;
	}

	private void setcArsMap(HashMap<String, Set<String>> cArsMap) {
		this.cArsMap = cArsMap;
	}

	public HashMap<String, Set<String>> getDrb1ArsMap() {
		return drb1ArsMap;
	}

	private void setDrb1ArsMap(HashMap<String, Set<String>> drb1ArsMap) {
		this.drb1ArsMap = drb1ArsMap;
	}

	public HashMap<String, Set<String>> getDrb3ArsMap() {
		return drb3ArsMap;
	}

	private void setDrb3ArsMap(HashMap<String, Set<String>> drb3ArsMap) {
		this.drb3ArsMap = drb3ArsMap;
	}

	public HashMap<String, Set<String>> getDrb4ArsMap() {
		return drb4ArsMap;
	}

	private void setDrb4ArsMap(HashMap<String, Set<String>> drb4ArsMap) {
		this.drb4ArsMap = drb4ArsMap;
	}

	public HashMap<String, Set<String>> getDrb5ArsMap() {
		return drb5ArsMap;
	}

	private void setDrb5ArsMap(HashMap<String, Set<String>> drb5ArsMap) {
		this.drb5ArsMap = drb5ArsMap;
	}

	public HashMap<String, Set<String>> getDqb1ArsMap() {
		return dqb1ArsMap;
	}

	private void setDqb1ArsMap(HashMap<String, Set<String>> dqb1ArsMap) {
		this.dqb1ArsMap = dqb1ArsMap;
	}
}
