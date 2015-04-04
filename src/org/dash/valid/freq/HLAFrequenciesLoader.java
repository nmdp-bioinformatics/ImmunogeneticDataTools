package org.dash.valid.freq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dash.valid.BCDisequilibriumElement;
import org.dash.valid.DRDQDisequilibriumElement;
import org.dash.valid.base.BaseBCDisequilibriumElement;
import org.dash.valid.base.BaseDRDQDisequilibriumElement;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.race.BCDisequilibriumElementByRace;
import org.dash.valid.race.DRDQDisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;

public class HLAFrequenciesLoader {
	private static final List<BCDisequilibriumElement> bcDisequilibriumElements = new ArrayList<BCDisequilibriumElement>();
	private static final List<DRDQDisequilibriumElement> drdqDisequilibriumElements = new ArrayList<DRDQDisequilibriumElement>();
	
	private static final String UNDERSCORE = "_";
	
	private static final int BASE_DRB1_POS = 0;
	private static final int BASE_DRB345_POS = 1;
	private static final int BASE_DQA1_POS = 2;
	private static final int BASE_DQB1_POS = 3;
	private static final int BASE_DRDQ_FREQ_POS = 4;
	private static final int BASE_DRDQ_NOTE_POS = 5;
	
	private static final int BASE_B_POS = 0;
	private static final int BASE_C_POS = 1;
	private static final int BASE_BC_FREQ_POS = 2;
	private static final int BASE_BC_NOTE_POS = 3;
	
	private static HLAFrequenciesLoader instance = null;
	
    private static final Logger LOGGER = Logger.getLogger(HLAFrequenciesLoader.class.getName());
    
	public static HLAFrequenciesLoader getInstance() {
		if (instance == null) {
			String value = System.getProperty("org.dash.frequencies");
			if (value != null && value.equals("nmdp")) {
				instance = new HLAFrequenciesLoader();
				init(true);
			}
			else {
				instance = new HLAFrequenciesLoader();
				init(false);
			}
		}
		
		return instance;
	}
	
	private static void init(boolean useNMDPFrequencies) {
		try {
			if (useNMDPFrequencies) {
				loadNMDPBCLinkageReferenceData();
				loadNMDPDRDQLinkageReferenceData();
			}
			else {
				loadBCLinkageReferenceData();
				loadDRDQLinkageReferenceData();
			}
			
			LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
		}
		catch (FileNotFoundException fnfe) {
			LOGGER.severe("Couldn't find disequilibrium element reference file.");
			fnfe.printStackTrace();
		}
		catch (IOException ioe) {
			LOGGER.severe("Couldn't load disequilibrium element reference file.");
			ioe.printStackTrace();
		}
	}
	
	public List<BCDisequilibriumElement> getBCDisequilibriumElements() {
		return bcDisequilibriumElements;
	}

	public List<DRDQDisequilibriumElement> getDRDQDisequilibriumElements() {
		return drdqDisequilibriumElements;
	}

	private static void loadNMDPBCLinkageReferenceData() throws FileNotFoundException, IOException {
		File myFile = new File("resources/C~B.xlsx");
        FileInputStream fis = new FileInputStream(myFile);
        
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);
       
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
       
        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        
        int firstRow = mySheet.getFirstRowNum();
        
        List<String> raceHeaders = null;

        // Traversing over each row of XLSX file
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            if (row.getRowNum() == firstRow) {
            	raceHeaders = readBCHeaderElementsByRace(row);
            }
            else {
	            readBCDiseqilibriumElementsByRace(row, raceHeaders);
            }            
        }
        
        myWorkBook.close();
	}
	
	private static void loadNMDPDRDQLinkageReferenceData() throws FileNotFoundException, IOException {
		File myFile = new File("resources/DRB3-4-5~DRB1~DQB1.xlsx");
        FileInputStream fis = new FileInputStream(myFile);
        
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);
       
        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
       
        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        
        int firstRow = mySheet.getFirstRowNum();
        
        List<String> raceHeaders = null;

        // Traversing over each row of XLSX file
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            if (row.getRowNum() == firstRow) {
            	raceHeaders = readDRDQHeaderElementsByRace(row);
            }
            else {
            	readDRDQDiseqilibriumElementsByRace(row, raceHeaders);
            }
        }
        
        myWorkBook.close();
	}
	
	private static List<String> readBCHeaderElementsByRace(Row row) {		
		List<String> raceHeaders = new ArrayList<String>();
		
		Iterator<Cell> cellIterator = row.cellIterator();

		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			
			String[] race = cell.getStringCellValue().split(UNDERSCORE);
			raceHeaders.add(cell.getColumnIndex(), race[0]);
		}
		
		return raceHeaders;
	}
	
	private static List<String> readDRDQHeaderElementsByRace(Row row) {		
		List<String> raceHeaders = new ArrayList<String>();
		
		Iterator<Cell> cellIterator = row.cellIterator();

		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			
			String[] race = cell.getStringCellValue().split(UNDERSCORE);
			raceHeaders.add(cell.getColumnIndex(), race[0]);
		}
		
		return raceHeaders;
	}

	/**
	 * @param row
	 */
	private static void readBCDiseqilibriumElementsByRace(Row row, List<String> raceHeaders) {
		// For each row, iterate through each columns
		Iterator<Cell> cellIterator = row.cellIterator();
		
		List<FrequencyByRace> frequenciesByRace  = new ArrayList<FrequencyByRace>();
		BCDisequilibriumElementByRace disElement = new BCDisequilibriumElementByRace();
		
		while (cellIterator.hasNext()) {
		    Cell cell = cellIterator.next();

		    switch (cell.getColumnIndex()) {
		    case 0:
		        disElement.setHlacElement(GLStringConstants.HLA_DASH + cell.getStringCellValue());
		        break;
		    case 1:
		    	disElement.setHlabElement(GLStringConstants.HLA_DASH + cell.getStringCellValue());
		    	break;
		    default :
		    	if (cell.getColumnIndex() % 2 == 0) {
		    		disElement.setFrequenciesByRace(loadFrequencyAndRank(row, cell, frequenciesByRace, raceHeaders));
		    	}
		    }		    
		}
		
	    bcDisequilibriumElements.add(disElement);
	}
	
	/**
	 * @param row
	 */
	private static void readDRDQDiseqilibriumElementsByRace(Row row, List<String> raceHeaders) {
		// For each row, iterate through each columns
		Iterator<Cell> cellIterator = row.cellIterator();
		
		List<FrequencyByRace> frequenciesByRace  = new ArrayList<FrequencyByRace>();
		DRDQDisequilibriumElementByRace disElement = new DRDQDisequilibriumElementByRace();
		
		while (cellIterator.hasNext()) {
		    Cell cell = cellIterator.next();

		    switch (cell.getColumnIndex()) {
		    case 0:
		        disElement.setHladrb345Element(GLStringConstants.HLA_DASH + cell.getStringCellValue());
		        break;
		    case 1:
		    	disElement.setHladrb1Element(GLStringConstants.HLA_DASH + cell.getStringCellValue());
		    	break;
		    case 2:
		    	disElement.setHladqb1Element(GLStringConstants.HLA_DASH + cell.getStringCellValue());
		    	break;
		    default :
		    	if (cell.getColumnIndex() % 2 != 0) {
		    		disElement.setFrequenciesByRace(loadFrequencyAndRank(row, cell, frequenciesByRace, raceHeaders));
		    	}
		    }		    
		}
		
	    drdqDisequilibriumElements.add(disElement);
	}

	/**
	 * @param row
	 * @param frequenciesByRace
	 * @param cell
	 * @param idx
	 */
	private static List<FrequencyByRace> loadFrequencyAndRank(Row row, Cell cell, 
			List<FrequencyByRace> frequenciesByRace, List<String> raceHeaders) {
		Double freq = cell.getNumericCellValue();
		
		if (freq != 0) {
			FrequencyByRace frequencyByRace = new FrequencyByRace(freq, ((Double) row.getCell(cell.getColumnIndex() + 1).getNumericCellValue()).toString(), raceHeaders.get(cell.getColumnIndex()));
			frequenciesByRace.add(frequencyByRace);
		}
		
		return frequenciesByRace;
	}

	private static void loadBCLinkageReferenceData() throws FileNotFoundException, IOException {
		File bcLinkages = new File("resources/BCLinkageDisequilibrium.txt");
		InputStream in = new FileInputStream(bcLinkages);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String row;
		String[] columns;
		while ((row = reader.readLine()) != null) {
			columns = row.split(GLStringConstants.TAB);

			bcDisequilibriumElements.add(new BaseBCDisequilibriumElement(columns[BASE_B_POS], columns[BASE_C_POS], columns[BASE_BC_FREQ_POS], columns[BASE_BC_NOTE_POS]));
		}
		
		reader.close();
	}
	
	private static void loadDRDQLinkageReferenceData() throws FileNotFoundException, IOException {
		String row;
		String[] columns;
		
		File drdqLinkages = new File("resources/DRDQLinkageDisequilibrium.txt");
		InputStream in = new FileInputStream(drdqLinkages);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		while ((row = reader.readLine()) != null) {
			columns = row.split(GLStringConstants.TAB);
			
			drdqDisequilibriumElements.add(new BaseDRDQDisequilibriumElement(columns[BASE_DRB1_POS], columns[BASE_DRB345_POS], columns[BASE_DQA1_POS], columns[BASE_DQB1_POS], columns[BASE_DRDQ_FREQ_POS], columns[BASE_DRDQ_NOTE_POS]));
		}
		
		reader.close();		
	}
}
