package org.dash.valid.freq;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import org.dash.valid.race.FrequencyByRaceComparator;

public class HLAFrequenciesLoader {
	private final List<BCDisequilibriumElement> bcDisequilibriumElements = new ArrayList<BCDisequilibriumElement>();
	private final List<DRDQDisequilibriumElement> drdqDisequilibriumElements = new ArrayList<DRDQDisequilibriumElement>();
	private final Set<String> individualLocusFrequencies = new HashSet<String>();
	
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
    
    private HLAFrequenciesLoader() {
    	
    }
    
	public static HLAFrequenciesLoader getInstance() {
		if (instance == null) {
			instance = new HLAFrequenciesLoader();
			Frequencies freq = Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
			instance.init(Frequencies.NMDP.equals(freq));
		}
		
		return instance;
	}
	
	public void reloadFrequencies() {
		Frequencies freq = Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
		bcDisequilibriumElements.removeAll(bcDisequilibriumElements);
		drdqDisequilibriumElements.removeAll(drdqDisequilibriumElements);
		init(Frequencies.NMDP.equals(freq));
	}
	
	private void init(boolean useNMDPFrequencies) {
		try {
			if (useNMDPFrequencies) {
				loadNMDPBCLinkageReferenceData();
				loadNMDPDRDQLinkageReferenceData();
				loadIndividualLocusFrequencies();
			}
			else {
				loadBCLinkageReferenceData();
				loadDRDQLinkageReferenceData();
			}			
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
	
	public Set<String> getIndividualLocusFrequencies() {
		return individualLocusFrequencies;
	}

	private void loadNMDPBCLinkageReferenceData() throws IOException {
		String filename = "frequencies/nmdp/C~B.xlsx";
        
        // Finds the workbook instance for XLSX file
		
        XSSFWorkbook myWorkBook = new XSSFWorkbook(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename));
       
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
            	raceHeaders = readHeaderElementsByRace(row);
            }
            else {
	            readBCDiseqilibriumElementsByRace(row, raceHeaders);
            }            
        }
        
        myWorkBook.close();
	}
	
	private void loadNMDPDRDQLinkageReferenceData() throws IOException {
		String filename = "frequencies/nmdp/DRB3-4-5~DRB1~DQB1.xlsx";
        
        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename));
       
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
            	raceHeaders = readHeaderElementsByRace(row);
            }
            else {
            	readDRDQDiseqilibriumElementsByRace(row, raceHeaders);
            }
        }
        
        myWorkBook.close();
	}
	
	private void loadIndividualLocusFrequencies() throws IOException {
		String filenames[] = {"frequencies/nmdp/B.xlsx", "frequencies/nmdp/C.xlsx", "frequencies/nmdp/DRB1.xlsx", 
								"frequencies/nmdp/DRB3-4-5.xlsx", "frequencies/nmdp/DQB1.xlsx"};
        
        // Finds the workbook instance for XLSX file
		
		for (String filename : filenames) {
	        XSSFWorkbook myWorkBook = new XSSFWorkbook(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename));
	       
	        // Return first sheet from the XLSX workbook
	        XSSFSheet mySheet = myWorkBook.getSheetAt(0);
	       
	        // Get iterator to all the rows in current sheet
	        Iterator<Row> rowIterator = mySheet.iterator();
	        
	        int firstRow = mySheet.getFirstRowNum();
	        	
	        // Traversing over each row of XLSX file
	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	
	            if (row.getRowNum() == firstRow) {
	            	continue;
	            }
	            else {
	        		individualLocusFrequencies.add(row.getCell(0).getStringCellValue());
	            }            
	        }
	        
	        myWorkBook.close();
		}
	}
	
	private List<String> readHeaderElementsByRace(Row row) {		
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
	private void readBCDiseqilibriumElementsByRace(Row row, List<String> raceHeaders) {
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
	private void readDRDQDiseqilibriumElementsByRace(Row row, List<String> raceHeaders) {
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
	private List<FrequencyByRace> loadFrequencyAndRank(Row row, Cell cell, 
			List<FrequencyByRace> frequenciesByRace, List<String> raceHeaders) {
		Double freq = cell.getNumericCellValue();
		
		if (freq != 0) {
			FrequencyByRace frequencyByRace = new FrequencyByRace(freq, ((Double) row.getCell(cell.getColumnIndex() + 1).getNumericCellValue()).toString(), raceHeaders.get(cell.getColumnIndex()));
			frequenciesByRace.add(frequencyByRace);
		}
		
		Collections.sort(frequenciesByRace, new FrequencyByRaceComparator());
		
		return frequenciesByRace;
	}

	private void loadBCLinkageReferenceData() throws FileNotFoundException, IOException {
		String filename = "frequencies/wikiversity/BCLinkageDisequilibrium.txt";
		BufferedReader reader = new BufferedReader(new InputStreamReader(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename)));
		String row;
		String[] columns;
		while ((row = reader.readLine()) != null) {
			columns = row.split(GLStringConstants.TAB);

			bcDisequilibriumElements.add(new BaseBCDisequilibriumElement(columns[BASE_B_POS], columns[BASE_C_POS], columns[BASE_BC_FREQ_POS], columns[BASE_BC_NOTE_POS]));
		}
		
		reader.close();
	}
	
	private void loadDRDQLinkageReferenceData() throws FileNotFoundException, IOException {
		String row;
		String[] columns;
		
		String filename = "frequencies/wikiversity/DRDQLinkageDisequilibrium.txt";
		BufferedReader reader = new BufferedReader(new InputStreamReader(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename)));
		
		while ((row = reader.readLine()) != null) {
			columns = row.split(GLStringConstants.TAB);
			
			drdqDisequilibriumElements.add(new BaseDRDQDisequilibriumElement(columns[BASE_DRB1_POS], columns[BASE_DRB345_POS], columns[BASE_DQA1_POS], columns[BASE_DQB1_POS], columns[BASE_DRDQ_FREQ_POS], columns[BASE_DRDQ_NOTE_POS]));
		}
		
		reader.close();		
	}
}
