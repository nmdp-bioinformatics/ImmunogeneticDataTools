package org.dash.valid.freq;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Linkages;
import org.dash.valid.Locus;
import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.race.FrequencyByRaceComparator;

public class HLAFrequenciesLoader {	
	private HashMap<EnumSet<Locus>, List<DisequilibriumElement>> disequilibriumElementsMap = new HashMap<EnumSet<Locus>, List<DisequilibriumElement>>();
	private final HashMap<Locus, List<String>> individualLocusFrequencies = new HashMap<Locus, List<String>>();
	
	private static final String UNDERSCORE = "_";
	
	private static final String WIKIVERSITY_BC_FREQUENCIES = "frequencies/wikiversity/BCLinkageDisequilibrium.txt";
	private static final String WIKIVERSITY_DRDQ_FREQUENCIES = "frequencies/wikiversity/DRDQLinkageDisequilibrium.txt";
	
	private static final String NMDP_ABC_FREQUENCIES = "frequencies/nmdp/A~C~B.xlsx";
	private static final String NMDP_BC_FREQUENCIES = "frequencies/nmdp/C~B.xlsx";
	private static final String NMDP_DRDQ_FREQUENCIES = "frequencies/nmdp/DRB3-4-5~DRB1~DQB1.xlsx";
	
	private static final Locus[] BASE_BC_LOCI_POS = new Locus[] {Locus.HLA_B, Locus.HLA_C};
	private static final Locus[] BASE_DRDQ_LOCI_POS = new Locus[] {Locus.HLA_DRB1, Locus.HLA_DRB345, Locus.HLA_DQA1, Locus.HLA_DQB1};
	private static final Locus[] NMDP_ABC_LOCI_POS = new Locus[] {Locus.HLA_A, Locus.HLA_C, Locus.HLA_B};
	private static final Locus[] NMDP_BC_LOCI_POS = new Locus[] {Locus.HLA_C, Locus.HLA_B};
	private static final Locus[] NMDP_DRDQ_LOCI_POS = new Locus[] {Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQB1};
	
	private static HLAFrequenciesLoader instance = null;
	
	private Set<Linkages> linkages = null;
	
    private static final Logger LOGGER = Logger.getLogger(HLAFrequenciesLoader.class.getName());
    
    private HLAFrequenciesLoader() {
    	
    }
    
	public static HLAFrequenciesLoader getInstance() {
		if (instance == null) {
			instance = new HLAFrequenciesLoader();
			Frequencies freq = Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
			
			Set<String> linkageNames = new HashSet<String>();
			String linkageProperties = System.getProperty(Linkages.LINKAGES_PROPERTY);
			
			if (linkageProperties != null) {
				StringTokenizer st = new StringTokenizer(linkageProperties, GLStringConstants.SPACE);
				while (st.hasMoreTokens()) {
					linkageNames.add(st.nextToken());
				}
			}
			
			instance.linkages = Linkages.lookup(linkageNames);
			
			instance.init(Frequencies.NMDP.equals(freq));
		}
		
		return instance;
	}
	
	public Set<Linkages> getLinkages() {
		return linkages;
	}
	
	public void reloadFrequencies() {
		Frequencies freq = Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
		
		this.disequilibriumElementsMap = new HashMap<EnumSet<Locus>, List<DisequilibriumElement>>();

		init(Frequencies.NMDP.equals(freq));
	}
	
	private void init(boolean useNMDPFrequencies) {
		try {
			if (useNMDPFrequencies) {
				for (Linkages linkage : getLinkages()) {
					switch (linkage) {
					case A_B_C:
						this.disequilibriumElementsMap.put(Locus.A_B_C_LOCI, loadNMDPLinkageReferenceData(NMDP_ABC_FREQUENCIES, NMDP_ABC_LOCI_POS));
					case B_C:
						this.disequilibriumElementsMap.put(Locus.B_C_LOCI, loadNMDPLinkageReferenceData(NMDP_BC_FREQUENCIES, NMDP_BC_LOCI_POS));
					case DRB_DQB:
						this.disequilibriumElementsMap.put(Locus.DRB_DQB_LOCI, loadNMDPLinkageReferenceData(NMDP_DRDQ_FREQUENCIES, NMDP_DRDQ_LOCI_POS));
					default:
						break;
					}
				}
				loadIndividualLocusFrequencies();
			}
			else {
				for (Linkages linkage : getLinkages()) {
					switch (linkage) {
					case B_C:
						this.disequilibriumElementsMap.put(Locus.B_C_LOCI, loadLinkageReferenceData(WIKIVERSITY_BC_FREQUENCIES, BASE_BC_LOCI_POS));
					case DRB_DQB:
						this.disequilibriumElementsMap.put(Locus.DRB_DQ_LOCI, loadLinkageReferenceData(WIKIVERSITY_DRDQ_FREQUENCIES, BASE_DRDQ_LOCI_POS));
					default:
						break;
					}
				}
			}			
		}
		catch (IOException | InvalidFormatException ioe) {
			LOGGER.severe("Couldn't load disequilibrium element reference file.");
			ioe.printStackTrace();
		}
	}
	
	public List<DisequilibriumElement> getDisequilibriumElements(EnumSet<Locus> loci) {
		if (this.disequilibriumElementsMap.containsKey(loci)) {
			return this.disequilibriumElementsMap.get(loci);
		}
		
		return new ArrayList<DisequilibriumElement>();
	}
	
	public HashMap<Locus, List<String>> getIndividualLocusFrequencies() {
		return individualLocusFrequencies;
	}
	
	private List<DisequilibriumElement> loadNMDPLinkageReferenceData(String filename, Locus[] locusPositions) throws IOException, InvalidFormatException {  
		List<DisequilibriumElement> disequilibriumElements = new ArrayList<DisequilibriumElement>();
		
        // Finds the workbook instance for XLSX file
		
        Workbook workbook = WorkbookFactory.create(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename));
       
        // Return first sheet from the XLSX workbook
        Sheet mySheet = workbook.getSheetAt(0);
       
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
	            disequilibriumElements.add(readDiseqilibriumElementsByRace(row, raceHeaders, locusPositions));
            }            
        }
        
        workbook.close();
        
        return disequilibriumElements;
	}
	
	private void loadIndividualLocusFrequencies() throws IOException, InvalidFormatException {
		for (Linkages linkage : getLinkages()) {
			for (Locus locus : linkage.getLoci()) {
				if (locus.hasIndividualFrequencies()) {
					loadIndividualLocusFrequency(locus);
				}
			}
		}
	}

	private void loadIndividualLocusFrequency(Locus locus)
			throws IOException, InvalidFormatException {
		List<String> singleLocusFrequencies = new ArrayList<String>();
		Workbook workbook = WorkbookFactory.create(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream("frequencies/nmdp/" + locus.getFrequencyName() + ".xlsx"));
      
		// Return first sheet from the XLSX workbook
		Sheet mySheet = workbook.getSheetAt(0);
      
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
				singleLocusFrequencies.add(GLStringConstants.HLA_DASH + row.getCell(0).getStringCellValue());
		    }            
		}
		
		individualLocusFrequencies.put(locus,  singleLocusFrequencies);
		
		workbook.close();
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
	private DisequilibriumElement readDiseqilibriumElementsByRace(Row row, List<String> raceHeaders, Locus[] locusPositions) {		
		// For each row, iterate through each columns
		Iterator<Cell> cellIterator = row.cellIterator();
		
		List<FrequencyByRace> frequenciesByRace  = new ArrayList<FrequencyByRace>();
		DisequilibriumElementByRace disElement = new DisequilibriumElementByRace();
		
		int columnIndex;
		
		while (cellIterator.hasNext()) {
		    Cell cell = cellIterator.next();

		    columnIndex = cell.getColumnIndex();
		    
		    if (columnIndex < locusPositions.length) {
		    	disElement.setHlaElement(locusPositions[cell.getColumnIndex()], GLStringConstants.HLA_DASH + cell.getStringCellValue());
		    }
		    else {
		    	if ((locusPositions.length % 2 == 0 && columnIndex % 2 == 0) || (locusPositions.length % 2 != 0 && columnIndex % 2 != 0)) {
		    		disElement.setFrequenciesByRace(loadFrequencyAndRank(row, cell, frequenciesByRace, raceHeaders));
		    	}
		    }
		}
		
	    return disElement;
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
	
	private List<DisequilibriumElement> loadLinkageReferenceData(String filename, Locus[] locusPositions) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename)));
		String row;
		String[] columns;
		HashMap<Locus, String> hlaElementMap;
		List<DisequilibriumElement> disequilibriumElements = new ArrayList<DisequilibriumElement>();
		
		while ((row = reader.readLine()) != null) {
			hlaElementMap = new HashMap<Locus, String>();
			
			columns = row.split(GLStringConstants.TAB);
			
			for (int i=0;i<locusPositions.length;i++) {
				hlaElementMap.put(locusPositions[i],  columns[i]);
			}
			
			disequilibriumElements.add(new BaseDisequilibriumElement(hlaElementMap, columns[locusPositions.length], columns[locusPositions.length + 1]));
		}
		
		reader.close();
		
		return disequilibriumElements;
	}
}
