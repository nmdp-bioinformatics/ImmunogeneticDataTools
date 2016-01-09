package org.dash.valid.ars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;

public class AntigenRecognitionSiteLoader {
	private static AntigenRecognitionSiteLoader instance = null;
	HashMap<String, List<String>> arsMap = new HashMap<String, List<String>>();
	
	private static final Locus[] loci = {Locus.HLA_A,
											Locus.HLA_B, 
											Locus.HLA_C, 
											Locus.HLA_DRB1, 
											Locus.HLA_DRB3,
											Locus.HLA_DRB4,
											Locus.HLA_DRB5,
											Locus.HLA_DQB1};

    private static final Logger LOGGER = Logger.getLogger(AntigenRecognitionSiteLoader.class.getName());
    
    private static final String DEFAULT_ARS_FILE = "reference/mmc1.xls";
	
	private AntigenRecognitionSiteLoader(HLADatabaseVersion hladb) {
		init(hladb);
	}
	
	private AntigenRecognitionSiteLoader() {
		init();
	}
	
	public HashMap<String, List<String>> getArsMap() {
		return this.arsMap;
	}
	
	public static AntigenRecognitionSiteLoader getInstance() {
		HLADatabaseVersion hladb = null;
		if (instance == null) {
			String ars = System.getProperty(HLADatabaseVersion.ARS_PROPERTY);
			if (ars != null && ars.equals(HLADatabaseVersion.ARS_BY_HLADB)) {
				hladb = HLADatabaseVersion.lookup(System.getProperty(HLADatabaseVersion.HLADB_PROPERTY));
				instance = new AntigenRecognitionSiteLoader(hladb);
			}
			else {
				instance = new AntigenRecognitionSiteLoader();
			}
		}

		return instance;
	}
	
	private void init(HLADatabaseVersion hladb) {
		for (Locus locus : loci) {
			HashMap<String, List<String>> locusArsMap = loadARSData(hladb, locus);
			this.arsMap.putAll(locusArsMap);
		}
	}
	
	private void init() {
		this.arsMap = loadARSData();
	}
	
	private static HashMap<String, List<String>> loadARSData(HLADatabaseVersion hladb, Locus locus) {
		BufferedReader reader = null;
		HashMap<String, List<String>> arsMap = new HashMap<String, List<String>>();
		
		String filename = "reference/" + hladb.getCwdName() + "/" + locus.getShortName() + ".txt";
		
		try {			
			reader = new BufferedReader(new InputStreamReader(AntigenRecognitionSiteLoader.class.getClassLoader().getResourceAsStream(filename)));
			
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
	
	private static HashMap<String, List<String>> loadARSData() {
		Workbook workbook = null;
		try {
			workbook = WorkbookFactory.create(AntigenRecognitionSiteLoader.class.getClassLoader().getResourceAsStream(DEFAULT_ARS_FILE));
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	       
        // Return first sheet from the XLSX workbook
        Sheet mySheet = workbook.getSheetAt(0);
       
        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        
        String gCode;
        String alleleString;
        List<String> alleles;
		HashMap<String, List<String>> arsMap = new HashMap<String, List<String>>();
                
        // Traversing over each row of XLSX file
        while (rowIterator.hasNext()) {
        	alleles = new ArrayList<String>();
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
        
        try {
			workbook.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return arsMap;
	}

	/**
	 * @param arsMap
	 * @param row
	 */
	private static HashMap<String, List<String>> assembleARSMap(String row) {
		String[] parts;
		String[] columns;
		String arsCode;
		List<String> alleles = new ArrayList<String>();
		String allele;
		
		HashMap<String, List<String>> arsMap = new HashMap<String, List<String>>();

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
}
