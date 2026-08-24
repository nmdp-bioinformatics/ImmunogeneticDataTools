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
package org.dash.valid.freq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
import org.dash.valid.Locus;
import org.dash.valid.base.BaseDisequilibriumElement;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dash.valid.race.FrequencyByRaceComparator;

public class HLAFrequenciesLoader {	
	private HashMap<EnumSet<Locus>, List<DisequilibriumElement>> disequilibriumElementsMap = new HashMap<EnumSet<Locus>, List<DisequilibriumElement>>();
	private final HashMap<Locus, List<String>> individualLocusFrequencies = new HashMap<Locus, List<String>>();
	
	private static final String UNDERSCORE = "_";
	
	public static final String WIKIVERSITY_BC_FREQUENCIES = "frequencies/wikiversity/BCLinkageDisequilibrium.txt";
	public static final String WIKIVERSITY_DRDQ_FREQUENCIES = "frequencies/wikiversity/DRDQLinkageDisequilibrium.txt";
	
	public static final String NMDP_ABC_FREQUENCIES = "frequencies/nmdp/A~C~B.xlsx";
	public static final String NMDP_BC_FREQUENCIES = "frequencies/nmdp/C~B.xlsx";
	public static final String NMDP_DRDQ_FREQUENCIES = "frequencies/nmdp/DRB3-4-5~DRB1~DQB1.xlsx";
	public static final String NMDP_FIVE_LOCUS_FREQUENCIES = "frequencies/nmdp/A~C~B~DRB1~DQB1.xlsx";
	public static final String NMDP_SIX_LOCUS_FREQUENCIES = "frequencies/nmdp/A~C~B~DRB3-4-5~DRB1~DQB1.xlsx";
	
	public static final String NMDP_2007_ABC_FREQUENCIES = "frequencies/nmdp-2007/ACB.xls";
	public static final String NMDP_2007_BC_FREQUENCIES = "frequencies/nmdp-2007/CB.xls";
	public static final String NMDP_2007_DRB1DQB1_FREQUENCIES = "frequencies/nmdp-2007/DRB1DQB1.xls";
	public static final String NMDP_2007_FIVE_LOCUS_FREQUENCIES = "frequencies/nmdp-2007/ACBDRB1DQB1.xls";
	
	public static final String NMDP_2007_STD_ABC_FREQUENCIES = "frequencies/std/NMDP_2007_ACB_Freqs.csv";
	public static final String NMDP_2007_STD_BC_FREQUENCIES = "frequencies/std/NMDP_2007_CB_Freqs.csv";
	public static final String NMDP_2007_STD_DRB1DQB1_FREQUENCIES = "frequencies/std/NMDP_2007_DRB1DQB1_Freqs.csv";
	public static final String NMDP_2007_STD_FIVELOCUS_FREQUENCIES = "frequencies/std/NMDP_2007_FiveLocus_Freqs.csv";
	
	public static final String NMDP_STD_ABC_FREQUENCIES = "frequencies/std/NMDP_ACB_Freqs.csv";
	public static final String NMDP_STD_BC_FREQUENCIES = "frequencies/std/NMDP_CB_Freqs.csv";
	public static final String NMDP_STD_DRB1DQB1_FREQUENCIES = "frequencies/std/NMDP_DRDQ_Freqs.csv";
	public static final String NMDP_STD_FIVELOCUS_FREQUENCIES = "frequencies/std/NMDP_FiveLocus_Freqs.csv";
	public static final String NMDP_STD_SIXLOCUS_FREQUENCIES = "frequencies/std/NMDP_SixLocus_Freqs.csv";
	
	public static final Locus[] BASE_BC_LOCI_POS = new Locus[] {Locus.HLA_B, Locus.HLA_C};
	public static final Locus[] BASE_DRDQ_LOCI_POS = new Locus[] {Locus.HLA_DRB1, Locus.HLA_DRB345, Locus.HLA_DQA1, Locus.HLA_DQB1};
	public static final Locus[] NMDP_ABC_LOCI_POS = new Locus[] {Locus.HLA_A, Locus.HLA_C, Locus.HLA_B};
	public static final Locus[] NMDP_BC_LOCI_POS = new Locus[] {Locus.HLA_C, Locus.HLA_B};
	public static final Locus[] NMDP_DRB1DQB1_LOCI_POS = new Locus[] {Locus.HLA_DRB1, Locus.HLA_DQB1};
	public static final Locus[] NMDP_DRDQB1_LOCI_POS = new Locus[] {Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQB1};
	public static final Locus[] NMDP_FIVE_LOCUS_POS = new Locus[] {Locus.HLA_A, Locus.HLA_C, Locus.HLA_B, Locus.HLA_DRB1, Locus.HLA_DQB1};
	public static final Locus[] NMDP_SIX_LOCUS_POS = new Locus[] {Locus.HLA_A, Locus.HLA_C, Locus.HLA_B, Locus.HLA_DRB345, Locus.HLA_DRB1, Locus.HLA_DQB1};
	
	private static HLAFrequenciesLoader instance = null;

	private static final Logger LOGGER = Logger.getLogger(HLAFrequenciesLoader.class.getName());
    
    private HLAFrequenciesLoader() {
    	
    }
    
    public static HLAFrequenciesLoader getInstance(Set<File> frequencyFiles, File allelesFile) {
    	instance = new HLAFrequenciesLoader();
    	instance.init(frequencyFiles, allelesFile);
    	
    	return instance;
    }
    
	public static HLAFrequenciesLoader getInstance() {
		if (instance == null) {
			instance = new HLAFrequenciesLoader();
			Frequencies freq = Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY));
						
			instance.init(freq);
		}
		
		return instance;
	}
	
	public boolean hasIndividualFrequency(Locus locus) {
		HashMap<Locus, List<String>> individualFrequencies = getIndividualLocusFrequencies();
		
		if (individualFrequencies != null && individualFrequencies.size() > 0 && individualFrequencies.get(locus) != null) {
			return true;
		}
		
		return false;
	}
	
	public String hasFrequency(Locus locus, String allele) {
		HashMap<Locus, List<String>> individualFrequencies = getIndividualLocusFrequencies();
		if (individualFrequencies == null || individualFrequencies.get(locus) == null) {
			return null;
		}
		
		for (String alleleWithFrequency : individualFrequencies.get(locus)) {
			if (GLStringUtilities.fieldLevelComparison(allele, alleleWithFrequency) || 
					GLStringUtilities.checkAntigenRecognitionSite(allele, alleleWithFrequency)) {
				return alleleWithFrequency;
			}
		}
		
		return null;
	}
	
	private void init(Set<File> frequencyFiles, File allelesFile) {
		Set<Linkages> linkages = new HashSet<Linkages>();
		try {
			for (File frequencyFile : frequencyFiles) {
				InputStream is = new FileInputStream(frequencyFile);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader reader = new BufferedReader(isr);
				
				List<DisequilibriumElement> elements = loadStandardReferenceData(reader);
				
				//List<DisequilibriumElement> elements = loadPhycusData();
				
				EnumSet<Locus> loci = Locus.lookup(elements.iterator().next().getLoci());
				linkages.addAll(Linkages.lookup(loci));
				
				this.disequilibriumElementsMap.put(loci, elements);
			}
			
			LinkagesLoader.getInstance(linkages);
			
			if (allelesFile != null) {
				loadIndividualLocusFrequencies(allelesFile);
			}
		}
		catch (IOException e) { //| ApiException e) {
			LOGGER.severe("Couldn't load disequilibrium element reference file.");
			e.printStackTrace();
			
			System.exit(-1);
		}
	}
	
	private void init(Frequencies freq) {
		try {
			switch(freq) {
			case NMDP_2007:
				for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
					switch (linkage) {
					case A_B_C:
						this.disequilibriumElementsMap.put(Locus.A_C_B_LOCI, loadNMDPLinkageReferenceData(NMDP_2007_ABC_FREQUENCIES, NMDP_ABC_LOCI_POS));
						break;
					case B_C:
						this.disequilibriumElementsMap.put(Locus.C_B_LOCI, loadNMDPLinkageReferenceData(NMDP_2007_BC_FREQUENCIES, NMDP_BC_LOCI_POS));
						break;
					case DRB1_DQB1:
						this.disequilibriumElementsMap.put(Locus.DRB1_DQB1_LOCI, loadNMDPLinkageReferenceData(NMDP_2007_DRB1DQB1_FREQUENCIES, NMDP_DRB1DQB1_LOCI_POS));
						break;
					case FIVE_LOCUS:
						this.disequilibriumElementsMap.put(Locus.FIVE_LOCUS, loadNMDPLinkageReferenceData(NMDP_2007_FIVE_LOCUS_FREQUENCIES, NMDP_FIVE_LOCUS_POS));
						break;
					default:
						break;
					}
				}
				loadIndividualLocusFrequencies(freq);
				break;
			case NMDP:
				for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
					switch (linkage) {
					case A_B_C:
						this.disequilibriumElementsMap.put(Locus.A_C_B_LOCI, loadNMDPLinkageReferenceData(NMDP_ABC_FREQUENCIES, NMDP_ABC_LOCI_POS));
						break;
					case B_C:
						this.disequilibriumElementsMap.put(Locus.C_B_LOCI, loadNMDPLinkageReferenceData(NMDP_BC_FREQUENCIES, NMDP_BC_LOCI_POS));
						break;
					case DRB_DQB:
						this.disequilibriumElementsMap.put(Locus.DRB_DQB_LOCI, loadNMDPLinkageReferenceData(NMDP_DRDQ_FREQUENCIES, NMDP_DRDQB1_LOCI_POS));
						break;
					case FIVE_LOCUS:
						this.disequilibriumElementsMap.put(Locus.FIVE_LOCUS,  loadNMDPLinkageReferenceData(NMDP_FIVE_LOCUS_FREQUENCIES, NMDP_FIVE_LOCUS_POS));
						break;
					case SIX_LOCUS:
						this.disequilibriumElementsMap.put(Locus.SIX_LOCUS, loadNMDPLinkageReferenceData(NMDP_SIX_LOCUS_FREQUENCIES, NMDP_SIX_LOCUS_POS));
						break;
					default:
						break;
					}
				}
				loadIndividualLocusFrequencies(freq);
				break;
			case NMDP_2007_STD:
				for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
					switch (linkage) {
					case A_B_C:
						this.disequilibriumElementsMap.put(Locus.A_C_B_LOCI, loadStandardReferenceData(NMDP_2007_STD_ABC_FREQUENCIES));
						break;
					case B_C:
						this.disequilibriumElementsMap.put(Locus.C_B_LOCI, loadStandardReferenceData(NMDP_2007_STD_BC_FREQUENCIES));
						break;
					case DRB_DQB:
						this.disequilibriumElementsMap.put(Locus.DRB_DQB_LOCI, loadStandardReferenceData(NMDP_2007_STD_DRB1DQB1_FREQUENCIES));
						break;
					case FIVE_LOCUS:
						this.disequilibriumElementsMap.put(Locus.FIVE_LOCUS, loadStandardReferenceData(NMDP_2007_STD_FIVELOCUS_FREQUENCIES));
						//this.disequilibriumElementsMap.put(Locus.FIVE_LOCUS, loadPhycusData());
						break;
					default:
						break;
					}
				}
				loadIndividualLocusFrequencies(freq);
				break;
			case NMDP_STD:
				for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
					switch (linkage) {
					case A_B_C:
						this.disequilibriumElementsMap.put(Locus.A_C_B_LOCI, loadStandardReferenceData(NMDP_STD_ABC_FREQUENCIES));
						break;
					case B_C:
						this.disequilibriumElementsMap.put(Locus.C_B_LOCI, loadStandardReferenceData(NMDP_STD_BC_FREQUENCIES));
						break;
					case DRB_DQB:
						this.disequilibriumElementsMap.put(Locus.DRB_DQB_LOCI, loadStandardReferenceData(NMDP_STD_DRB1DQB1_FREQUENCIES));
						break;
					case FIVE_LOCUS:
						this.disequilibriumElementsMap.put(Locus.FIVE_LOCUS, loadStandardReferenceData(NMDP_STD_FIVELOCUS_FREQUENCIES));
						break;
					case SIX_LOCUS:
						this.disequilibriumElementsMap.put(Locus.SIX_LOCUS, loadStandardReferenceData(NMDP_STD_SIXLOCUS_FREQUENCIES));
						break;
					default:
						break;
					}
				}
				loadIndividualLocusFrequencies(freq);
				break;
			case WIKIVERSITY:
				for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
					switch (linkage) {
					case B_C:
						this.disequilibriumElementsMap.put(Locus.C_B_LOCI, loadLinkageReferenceData(WIKIVERSITY_BC_FREQUENCIES, BASE_BC_LOCI_POS));
						break;
					case DRB_DQ:
						this.disequilibriumElementsMap.put(Locus.DRB_DQ_LOCI, loadLinkageReferenceData(WIKIVERSITY_DRDQ_FREQUENCIES, BASE_DRDQ_LOCI_POS));
						break;
					default:
						break;
					}
				}
				break;
			default:
				break;
			}			
		}
		catch (IOException | InvalidFormatException ioe) { // | ApiException ioe) {
			if (Frequencies.NMDP.equals(freq)) {
				LOGGER.warning("2011 NMDP Frequencies are not included by default.  Please be sure you've loaded them according to the instructions in the README.");
			}
			LOGGER.severe("Couldn't load disequilibrium element reference file.");
			ioe.printStackTrace();
			
			System.exit(-1);
		}
	}
	
	public List<DisequilibriumElement> getDisequilibriumElements(EnumSet<Locus> loci) {
		if (this.disequilibriumElementsMap.containsKey(loci)) {
			return this.disequilibriumElementsMap.get(loci);
		}
		
		return new ArrayList<DisequilibriumElement>();
	}
	
	public Set<EnumSet<Locus>> getLoci() {
		return this.disequilibriumElementsMap.keySet();
	}
	
	public HashMap<Locus, List<String>> getIndividualLocusFrequencies() {
		return individualLocusFrequencies;
	}
	
//	private List<DisequilibriumElement> loadPhycusData() throws ApiException {
//		HashMap<String, List<FrequencyByRace>> frequencyMap = new HashMap<String, List<FrequencyByRace>>();
//
//		ApiClient apiClient = new ApiClient();
//		apiClient.setBasePath("http://localhost:8080");
//		DefaultApi api = new DefaultApi(apiClient);
//		
//		HaplotypeFrequencyData freqData = null;
//				
//		for (long submissionId=1;submissionId<5;submissionId++) {
//			PopulationData popData = null;
//			popData = api.hfcSubmissionIdPopulationGet(submissionId);
//			
//			String race = popData.getName();
//			freqData = api.hfcSubmissionIdHaplotypesGet(submissionId);
//			
//			for (HaplotypeFrequency haplotypeFrequency : freqData.getHaplotypeFrequencyList()) {
//				Double frequency = haplotypeFrequency.getFrequency();
//				String haplotype = haplotypeFrequency.getHaplotypeString();
//				
//				List<FrequencyByRace> freqList = frequencyMap.get(haplotype);
//				
//				if (freqList == null) {
//					freqList = new ArrayList<FrequencyByRace>();
//				}
//				
//				FrequencyByRace frequencyByRace = new FrequencyByRace(frequency, null, race);
//				freqList.add(frequencyByRace);
//				
//				frequencyMap.put(haplotypeFrequency.getHaplotypeString(), freqList);
//			}
//		}
//		
//		List<DisequilibriumElement> disequilibriumElements = new ArrayList<DisequilibriumElement>();
//		DisequilibriumElementByRace disElement;
//		HashMap<String, Locus> locusMap = new HashMap<String, Locus>();
//		Locus locus = null;
//		
//		for (String haplotype : frequencyMap.keySet()) {
//			String[] locusHaplotypes = haplotype.split(GLStringConstants.GENE_PHASE_DELIMITER);
//			
//			HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
//			for (String locusHaplotype : locusHaplotypes) {
//				String[] parts = locusHaplotype.split(GLStringUtilities.ESCAPED_ASTERISK);
//				List<String> val = new ArrayList<String>();
//				val.add(locusHaplotype);
//				
//				if (locusMap.containsKey(parts[0])) {
//					locus = locusMap.get(parts[0]);
//				}
//				else {
//					locus = Locus.normalizeLocus(Locus.lookup(parts[0]));
//					locusMap.put(parts[0], locus);
//				}
//				
//				hlaElementMap.put(locus, val);
//			}
//			
//			disElement = new DisequilibriumElementByRace(hlaElementMap, frequencyMap.get(haplotype));
//			
//			disequilibriumElements.add(disElement);			
//		}
//		
//		return disequilibriumElements;
//	}
	
	private List<DisequilibriumElement> loadStandardReferenceData(String filename) throws IOException, InvalidFormatException {
		InputStream inStream = HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename);
		
		if (inStream == null) {
			throw new FileNotFoundException();
		}
		
		InputStreamReader isr = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(isr);

		return loadStandardReferenceData(reader);
	}

	public static List<DisequilibriumElement> loadStandardReferenceData(
			BufferedReader reader) throws IOException {
		String row;
		String[] columns;
		HashMap<String, List<FrequencyByRace>> frequencyMap = new HashMap<String, List<FrequencyByRace>>();
		
		while ((row = reader.readLine()) != null) {			
			columns = row.split(GLStringConstants.COMMA);
						
			String race = columns[0];
			String haplotype = columns[1];
			double frequency = Double.parseDouble(columns[2]);
			String rank = null;
			
			if (columns.length == 4) rank = columns[3];
			
			List<FrequencyByRace> freqList = frequencyMap.get(haplotype);
			
			if (freqList == null) {
				freqList = new ArrayList<FrequencyByRace>();
			}
			
			FrequencyByRace freqByRace = new FrequencyByRace(frequency, rank, race);
			freqList.add(freqByRace);
			
			frequencyMap.put(haplotype, freqList);
		}
		
		List<DisequilibriumElement> disequilibriumElements = new ArrayList<DisequilibriumElement>();
		DisequilibriumElementByRace disElement;
		HashMap<String, Locus> locusMap = new HashMap<String, Locus>();
		Locus locus = null;
		
		for (String haplotype : frequencyMap.keySet()) {
			String[] locusHaplotypes = haplotype.split(GLStringConstants.GENE_PHASE_DELIMITER);
			
			HashMap<Locus, List<String>> hlaElementMap = new HashMap<Locus, List<String>>();
			for (String locusHaplotype : locusHaplotypes) {
				String[] parts = locusHaplotype.split(GLStringUtilities.ESCAPED_ASTERISK);
				List<String> val = new ArrayList<String>();
				val.add(locusHaplotype);
				
				if (locusMap.containsKey(parts[0])) {
					locus = locusMap.get(parts[0]);
				}
				else {
					locus = Locus.normalizeLocus(Locus.lookup(parts[0]));
					locusMap.put(parts[0], locus);
				}
				
				hlaElementMap.put(locus, val);
			}
			
			disElement = new DisequilibriumElementByRace(hlaElementMap, frequencyMap.get(haplotype));
			
			disequilibriumElements.add(disElement);			
		}
		
		reader.close();
		
		return disequilibriumElements;
	}
	
	public List<DisequilibriumElement> loadNMDPLinkageReferenceData(String filename, Locus[] locusPositions) throws IOException, InvalidFormatException {  
        // Finds the workbook instance for XLSX file
		InputStream inStream = HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename);
		
		if (inStream == null) {
			throw new FileNotFoundException();
		}
		
        return loadNMDPLinkageReferenceData(inStream, locusPositions);
	}

	public static List<DisequilibriumElement> loadNMDPLinkageReferenceData(
			InputStream inStream,
			Locus[] locusPositions) throws IOException, InvalidFormatException {
		List<DisequilibriumElement> disequilibriumElements = new ArrayList<DisequilibriumElement>();

		// The entire bundled 2007 NMDP reference dataset (frequencies/nmdp-2007/*.xls) is the
		// legacy OLE2/BIFF binary format, not the ZIP-based OOXML format ".xlsx" implies --
		// WorkbookFactory.create() always auto-detected and handled both transparently, but
		// StreamingXlsxRows (see its class comment) only understands OOXML. Peeking the actual
		// container format (not trusting the extension) means only genuinely OOXML input takes
		// the streaming path; the OLE2 files, which were never the memory/performance problem
		// StreamingXlsxRows exists to fix, keep going through the DOM reader that already
		// handles them correctly.
		InputStream markableStream = FileMagic.prepareToCheckMagic(inStream);
		FileMagic magic = FileMagic.valueOf(markableStream);

		if (FileMagic.OOXML.equals(magic)) {
			ReferenceDataRowHandler rowHandler = new ReferenceDataRowHandler(disequilibriumElements, locusPositions);
			StreamingXlsxRows.read(markableStream, rowHandler);

			if (rowHandler.isCombinedHaplotypeFormat()) {
				rankCombinedHaplotypeFrequencies(disequilibriumElements);
			}
		}
		else {
			loadNMDPLinkageReferenceDataDom(markableStream, locusPositions, disequilibriumElements);
		}

        return disequilibriumElements;
	}

	// DOM (WorkbookFactory/Row/Cell) fallback for legacy OLE2 (.xls) reference files -- see
	// loadNMDPLinkageReferenceData(InputStream, Locus[]) above for why this still exists
	// alongside the SAX streaming path. The combined-haplotype layout (see
	// COMBINED_HAPLOTYPE_HEADER) is only ever used by the 2026 nine-locus release, which is
	// always OOXML, so this fallback only ever needs the original per-locus-column layout.
	private static void loadNMDPLinkageReferenceDataDom(InputStream inStream, Locus[] locusPositions,
			List<DisequilibriumElement> disequilibriumElements) throws IOException, InvalidFormatException {
		Workbook workbook = WorkbookFactory.create(inStream);

		Sheet mySheet = workbook.getSheetAt(0);
		Iterator<Row> rowIterator = mySheet.iterator();
		int firstRow = mySheet.getFirstRowNum();

		List<String> raceHeaders = null;

		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();

			if (row.getRowNum() == firstRow) {
				raceHeaders = readHeaderElementsByRaceDom(row);
			}
			else {
				disequilibriumElements.add(readDiseqilibriumElementsByRaceDom(row, raceHeaders, locusPositions));
			}
		}

		workbook.close();
	}

	// Row-by-row callback for loadNMDPLinkageReferenceData(InputStream, Locus[]) above: the
	// first row streamed is always the header row (self-describing -- see
	// COMBINED_HAPLOTYPE_HEADER), every row after that is a data row.
	private static final class ReferenceDataRowHandler implements StreamingXlsxRows.RowHandler {
		private final List<DisequilibriumElement> disequilibriumElements;
		private final Locus[] locusPositions;
		private boolean firstRowSeen;
		private boolean combinedHaplotypeFormat;
		private List<String> raceHeaders;

		ReferenceDataRowHandler(List<DisequilibriumElement> disequilibriumElements, Locus[] locusPositions) {
			this.disequilibriumElements = disequilibriumElements;
			this.locusPositions = locusPositions;
		}

		@Override
		public void row(int rowNum, List<String> row) {
			if (!firstRowSeen) {
				firstRowSeen = true;
				combinedHaplotypeFormat = !row.isEmpty() && COMBINED_HAPLOTYPE_HEADER.equalsIgnoreCase(row.get(0));
				raceHeaders = combinedHaplotypeFormat
						? readCombinedHaplotypeHeaderElementsByRace(row)
						: readHeaderElementsByRace(row);
			}
			else {
				disequilibriumElements.add(combinedHaplotypeFormat
						? readCombinedHaplotypeElementsByRace(row, raceHeaders, locusPositions)
						: readDiseqilibriumElementsByRace(row, raceHeaders, locusPositions));
			}
		}

		boolean isCombinedHaplotypeFormat() {
			return combinedHaplotypeFormat;
		}
	}
	
	private void loadIndividualLocusFrequencies(File allelesFile) throws IOException {
		String row;
		String parts[];
		HashMap<String, Locus> locusMap = new HashMap<String, Locus>();
		Locus locus;
		List<String> singleLocusFrequencies;
		
		InputStream inStream = new FileInputStream(allelesFile);
		
		InputStreamReader isr = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(isr);
		
		while ((row = reader.readLine()) != null) {
			parts = row.split(GLStringUtilities.ESCAPED_ASTERISK);
			if (locusMap.containsKey(parts[0])) {
				locus = locusMap.get(parts[0]);
			}
			else {
				locus = Locus.lookup(parts[0]);
				locusMap.put(parts[0], locus);
			}
			
			if (individualLocusFrequencies.containsKey(locus)) {
				singleLocusFrequencies = individualLocusFrequencies.get(locus);
			}
			else {
				singleLocusFrequencies = new ArrayList<String>();
			}
			
			singleLocusFrequencies.add(row);
			individualLocusFrequencies.put(locus, singleLocusFrequencies);			
		}
		
		reader.close();
	}
	
	private void loadIndividualLocusFrequencies(Frequencies freq) throws IOException, InvalidFormatException {
		for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
			for (Locus locus : linkage.getLoci()) {
				if (locus.hasIndividualFrequencies()) {
					loadIndividualLocusFrequency(freq, locus);
				}
			}
		}
	}

	private void loadIndividualLocusFrequency(Frequencies freq, Locus locus)
			throws IOException, InvalidFormatException {
		String extension = freq.equals(Frequencies.NMDP) || freq.equals(Frequencies.NMDP_STD) ? ".xlsx" : ".xls";
		String shortName = freq.getShortName();
		if (freq.equals(Frequencies.NMDP_STD)) shortName = Frequencies.NMDP.getShortName();
		if (freq.equals(Frequencies.NMDP_2007_STD)) shortName = Frequencies.NMDP_2007_STD.getShortName();
		InputStream inputStream = HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream("frequencies/" + shortName + "/" + locus.getFrequencyName() + extension);
      
		if (inputStream == null) return;
		
		List<String> singleLocusFrequencies = loadIndividualLocusFrequency(inputStream);
		
		individualLocusFrequencies.put(locus,  singleLocusFrequencies);
	}

	public static List<String> loadIndividualLocusFrequency(InputStream inputStream)
			throws IOException, InvalidFormatException {
		// See loadNMDPLinkageReferenceData(InputStream, Locus[]) for why format is detected
		// from the actual container bytes rather than assumed: the bundled 2007 NMDP
		// single-locus files (frequencies/nmdp-2007/*.xls) are legacy OLE2, not OOXML, and
		// StreamingXlsxRows only understands OOXML.
		InputStream markableStream = FileMagic.prepareToCheckMagic(inputStream);
		FileMagic magic = FileMagic.valueOf(markableStream);

		if (FileMagic.OOXML.equals(magic)) {
			return loadIndividualLocusFrequencyStreaming(markableStream);
		}

		return loadIndividualLocusFrequencyDom(markableStream);
	}

	private static List<String> loadIndividualLocusFrequencyStreaming(InputStream inputStream)
			throws IOException, InvalidFormatException {
		final List<String> singleLocusFrequencies = new ArrayList<String>();

		// Streams rows via StreamingXlsxRows (SAX) rather than building the full in-memory
		// DOM WorkbookFactory.create()/XSSFWorkbook would -- see loadNMDPLinkageReferenceData
		// and StreamingXlsxRows' class comment for why.
		StreamingXlsxRows.read(inputStream, new StreamingXlsxRows.RowHandler() {
			private boolean firstRowSeen;
			private String detectedLocus;
			private String locusShortName;

			@Override
			public void row(int rowNum, List<String> row) {
				if (!firstRowSeen) {
					firstRowSeen = true;
					// Legacy single-locus files header their first column with the locus
					// name itself (e.g. "A"); the newer combined-haplotype layout (see
					// loadNMDPLinkageReferenceData) headers it "Haplotype" instead, since
					// every row's first cell already carries a fully-qualified allele string
					// (e.g. "A*02:01") needing no locus name applied to it. Resolving the
					// locus's short name only lazily, when a legacy-format data row actually
					// needs it below, means a "Haplotype" header (which Locus.lookup()
					// wouldn't recognize) no longer needs to resolve to anything up front.
					detectedLocus = row.get(0);
					return;
				}

				String cellValue = row.get(0);
				if (!cellValue.contains(GLStringConstants.ASTERISK)) {
					if (locusShortName == null) {
						locusShortName = Locus.lookup(detectedLocus).getShortName();
					}
					cellValue = locusShortName + GLStringConstants.ASTERISK + cellValue.substring(0, 2) + GLStringUtilities.COLON + cellValue.substring(2);
				}
				singleLocusFrequencies.add(GLStringConstants.HLA_DASH + cellValue);
			}
		});

		return singleLocusFrequencies;
	}

	// DOM (WorkbookFactory/Row/Cell) fallback for legacy OLE2 (.xls) single-locus reference
	// files -- see loadIndividualLocusFrequency(InputStream) above.
	private static List<String> loadIndividualLocusFrequencyDom(InputStream inputStream)
			throws IOException, InvalidFormatException {
		String detectedLocus = null;
		String locusShortName = null;
		List<String> singleLocusFrequencies = new ArrayList<String>();

		Workbook workbook = WorkbookFactory.create(inputStream);

		Sheet mySheet = workbook.getSheetAt(0);
		Iterator<Row> rowIterator = mySheet.iterator();
		int firstRow = mySheet.getFirstRowNum();

		while (rowIterator.hasNext()) {
		    Row row = rowIterator.next();

		    if (row.getRowNum() == firstRow) {
		    	detectedLocus = row.getCell(0).getStringCellValue();
		    	locusShortName = Locus.lookup(detectedLocus).getShortName();
		    	continue;
		    }

		    String cellValue = row.getCell(0).getStringCellValue();
		    if (!cellValue.contains(GLStringConstants.ASTERISK)) {
		    	cellValue = locusShortName + GLStringConstants.ASTERISK + cellValue.substring(0, 2) + GLStringUtilities.COLON + cellValue.substring(2);
		    }
			singleLocusFrequencies.add(GLStringConstants.HLA_DASH + cellValue);
		}

		workbook.close();
		return singleLocusFrequencies;
	}
	
	// DOM (Row/Cell) counterpart of readHeaderElementsByRace(List<String>) below, used only by
	// the legacy OLE2 (.xls) fallback path (see loadNMDPLinkageReferenceDataDom()).
	private static List<String> readHeaderElementsByRaceDom(Row row) {
		List<String> raceHeaders = new ArrayList<String>();

		Iterator<Cell> cellIterator = row.cellIterator();

		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();

			String[] race = cell.getStringCellValue().split(UNDERSCORE);
			raceHeaders.add(cell.getColumnIndex(), race[0]);
		}

		return raceHeaders;
	}

	// DOM (Row/Cell) counterpart of readDiseqilibriumElementsByRace(List<String>, ...) below,
	// used only by the legacy OLE2 (.xls) fallback path.
	private static DisequilibriumElement readDiseqilibriumElementsByRaceDom(Row row, List<String> raceHeaders, Locus[] locusPositions) {
		Iterator<Cell> cellIterator = row.cellIterator();

		List<FrequencyByRace> frequenciesByRace  = new ArrayList<FrequencyByRace>();
		DisequilibriumElementByRace disElement = new DisequilibriumElementByRace();

		int columnIndex;
		String cellValue = null;

		while (cellIterator.hasNext()) {
		    Cell cell = cellIterator.next();

		    columnIndex = cell.getColumnIndex();

		    if (columnIndex < locusPositions.length) {
			    cellValue = cell.getStringCellValue();
			    if (!cellValue.contains(GLStringConstants.ASTERISK)) {
			    	cellValue = locusPositions[columnIndex].getShortName() + GLStringConstants.ASTERISK + cellValue.substring(0, 2) + GLStringUtilities.COLON + cellValue.substring(2);
			    }
			    List<String> val = new ArrayList<String>();
			    val.add(GLStringConstants.HLA_DASH + cellValue);
		    	disElement.setHlaElement(locusPositions[columnIndex], val);
		    }
		    else {
		    	if ((locusPositions.length % 2 == 0 && columnIndex % 2 == 0) || (locusPositions.length % 2 != 0 && columnIndex % 2 != 0)) {
		    		disElement.setFrequenciesByRace(loadFrequencyAndRankDom(row, cell, frequenciesByRace, raceHeaders));
		    	}
		    }
		}

	    return disElement;
	}

	// DOM (Row/Cell) counterpart of loadFrequencyAndRank(List<String>, ...) below, used only by
	// the legacy OLE2 (.xls) fallback path.
	private static List<FrequencyByRace> loadFrequencyAndRankDom(Row row, Cell cell,
			List<FrequencyByRace> frequenciesByRace, List<String> raceHeaders) {
		Double freq = cell.getNumericCellValue();

		if (freq != 0) {
			FrequencyByRace frequencyByRace = new FrequencyByRace(freq, ((Double) row.getCell(cell.getColumnIndex() + 1).getNumericCellValue()).toString(), raceHeaders.get(cell.getColumnIndex()));
			frequenciesByRace.add(frequencyByRace);
		}

		Collections.sort(frequenciesByRace, new FrequencyByRaceComparator());

		return frequenciesByRace;
	}

	private static List<String> readHeaderElementsByRace(List<String> row) {
		List<String> raceHeaders = new ArrayList<String>();

		for (String cellValue : row) {
			String[] race = cellValue.split(UNDERSCORE);
			raceHeaders.add(race[0]);
		}

		return raceHeaders;
	}

	/**
	 * @param row
	 */
	private static DisequilibriumElement readDiseqilibriumElementsByRace(List<String> row, List<String> raceHeaders, Locus[] locusPositions) {
		List<FrequencyByRace> frequenciesByRace  = new ArrayList<FrequencyByRace>();
		DisequilibriumElementByRace disElement = new DisequilibriumElementByRace();

		for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
		    if (columnIndex < locusPositions.length) {
			    String cellValue = row.get(columnIndex);
			    if (!cellValue.contains(GLStringConstants.ASTERISK)) {
			    	cellValue = locusPositions[columnIndex].getShortName() + GLStringConstants.ASTERISK + cellValue.substring(0, 2) + GLStringUtilities.COLON + cellValue.substring(2);
			    }
			    List<String> val = new ArrayList<String>();
			    val.add(GLStringConstants.HLA_DASH + cellValue);
		    	disElement.setHlaElement(locusPositions[columnIndex], val);
		    }
		    else {
		    	if ((locusPositions.length % 2 == 0 && columnIndex % 2 == 0) || (locusPositions.length % 2 != 0 && columnIndex % 2 != 0)) {
		    		disElement.setFrequenciesByRace(loadFrequencyAndRank(row, columnIndex, frequenciesByRace, raceHeaders));
		    	}
		    }
		}

	    return disElement;
	}

	/**
	 * @param row
	 * @param columnIndex
	 * @param frequenciesByRace
	 * @param raceHeaders
	 */
	private static List<FrequencyByRace> loadFrequencyAndRank(List<String> row, int columnIndex,
			List<FrequencyByRace> frequenciesByRace, List<String> raceHeaders) {
		// A frequency cell (and its companion rank cell) can legitimately be absent from the
		// source XML entirely rather than present with a "0" value -- treat a blank the same
		// as a zero frequency (skip it) instead of failing to parse it as a number.
		String freqText = row.get(columnIndex);
		if (!freqText.isEmpty()) {
			double freq = Double.parseDouble(freqText);

			if (freq != 0) {
				String rankText = columnIndex + 1 < row.size() ? row.get(columnIndex + 1) : "";
				// Matches the legacy DOM code's own transformation exactly: it read the rank
				// cell's raw numeric value and re-stringified it via Double.toString() (e.g.
				// "4" in the file becomes "4.0"), rather than passing the file's own text
				// through unchanged.
				String rank = rankText.isEmpty() ? null : Double.toString(Double.parseDouble(rankText));
				frequenciesByRace.add(new FrequencyByRace(freq, rank, raceHeaders.get(columnIndex)));
			}
		}

		Collections.sort(frequenciesByRace, new FrequencyByRaceComparator());

		return frequenciesByRace;
	}

	// Header row label that identifies the combined-haplotype layout (see
	// loadNMDPLinkageReferenceData(InputStream, Locus[])).
	private static final String COMBINED_HAPLOTYPE_HEADER = "Haplotype";

	// Combined-haplotype layout's header row is: "Haplotype", one column per population
	// (its plain name, e.g. "AAFA" -- no "_..." suffix to strip, unlike the legacy layout's
	// readHeaderElementsByRace()), then "TotalFreq". Race headers are kept aligned to their
	// column index (indices 0 and lastColumn left null and never read) so the shared
	// raceHeaders.get(columnIndex) lookup pattern used elsewhere still works unchanged.
	private static List<String> readCombinedHaplotypeHeaderElementsByRace(List<String> row) {
		int lastColumn = row.size() - 1;
		List<String> raceHeaders = new ArrayList<String>(Collections.nCopies(lastColumn + 1, (String) null));

		for (int col = 1; col < lastColumn; col++) {
			raceHeaders.set(col, row.get(col));
		}

		return raceHeaders;
	}

	// Parses one data row of the combined-haplotype layout. Column 0 is a single "~"-joined,
	// already-fully-formatted haplotype string (e.g. "A*01:01~C*07:01~B*08:01"), so each
	// locus's allele is read directly out of that one cell instead of from its own column,
	// unlike readDiseqilibriumElementsByRace()'s per-locus columns. The remaining columns
	// (excluding the trailing TotalFreq column) are plain per-population frequency values
	// with no companion rank column -- rank isn't in this layout's source data at all, so it's
	// left null here and filled in afterward by rankCombinedHaplotypeFrequencies(), once every
	// row's frequency for a given population is known.
	private static DisequilibriumElementByRace readCombinedHaplotypeElementsByRace(List<String> row, List<String> raceHeaders, Locus[] locusPositions) {
		DisequilibriumElementByRace disElement = new DisequilibriumElementByRace();

		String haplotype = row.get(0);
		String[] tokens = haplotype.split(GLStringConstants.GENE_PHASE_DELIMITER);

		for (int i = 0; i < tokens.length && i < locusPositions.length; i++) {
			List<String> val = new ArrayList<String>();
			val.add(GLStringConstants.HLA_DASH + tokens[i]);
			disElement.setHlaElement(locusPositions[i], val);
		}

		List<FrequencyByRace> frequenciesByRace = new ArrayList<FrequencyByRace>();
		int lastColumn = row.size() - 1;

		for (int col = 1; col < lastColumn; col++) {
			String freqText = row.get(col);
			if (freqText.isEmpty()) {
				continue;
			}

			double freq = Double.parseDouble(freqText);
			if (freq != 0) {
				frequenciesByRace.add(new FrequencyByRace(freq, null, raceHeaders.get(col)));
			}
		}

		disElement.setFrequenciesByRace(frequenciesByRace);

		return disElement;
	}

	// Computes rank for the combined-haplotype layout, which supplies no rank column (see
	// readCombinedHaplotypeElementsByRace() above): for each population, every haplotype's
	// FrequencyByRace entry for that population is ranked 1 (highest frequency) through N,
	// matching what "rank" meant when it was read directly from the legacy layout's rank
	// columns instead of computed.
	private static void rankCombinedHaplotypeFrequencies(List<DisequilibriumElement> disequilibriumElements) {
		HashMap<String, List<FrequencyByRace>> frequenciesByPopulation = new HashMap<String, List<FrequencyByRace>>();

		for (DisequilibriumElement element : disequilibriumElements) {
			for (FrequencyByRace frequencyByRace : ((DisequilibriumElementByRace) element).getFrequenciesByRace()) {
				List<FrequencyByRace> forPopulation = frequenciesByPopulation.get(frequencyByRace.getRace());
				if (forPopulation == null) {
					forPopulation = new ArrayList<FrequencyByRace>();
					frequenciesByPopulation.put(frequencyByRace.getRace(), forPopulation);
				}
				forPopulation.add(frequencyByRace);
			}
		}

		for (List<FrequencyByRace> forPopulation : frequenciesByPopulation.values()) {
			Collections.sort(forPopulation, new java.util.Comparator<FrequencyByRace>() {
				@Override
				public int compare(FrequencyByRace a, FrequencyByRace b) {
					return Double.compare(b.getFrequency(), a.getFrequency());
				}
			});

			for (int i = 0; i < forPopulation.size(); i++) {
				forPopulation.get(i).setRank(String.valueOf(i + 1));
			}
		}
	}

	public List<DisequilibriumElement> loadLinkageReferenceData(String filename, Locus[] locusPositions) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(HLAFrequenciesLoader.class.getClassLoader().getResourceAsStream(filename)));
		String row;
		String[] columns;
		HashMap<Locus, List<String>> hlaElementMap;
		List<DisequilibriumElement> disequilibriumElements = new ArrayList<DisequilibriumElement>();
		
		while ((row = reader.readLine()) != null) {
			hlaElementMap = new HashMap<Locus, List<String>>();
			
			columns = row.split(GLStringConstants.TAB);
			
			for (int i=0;i<locusPositions.length;i++) {
				List<String> val = new ArrayList<String>();
				
				val.add(GLStringConstants.DASH.equals(columns[i]) ? GLStringConstants.NNNN : columns[i]);
				hlaElementMap.put(locusPositions[i],  val);
			}
			
			disequilibriumElements.add(new BaseDisequilibriumElement(hlaElementMap, columns[locusPositions.length], columns[locusPositions.length + 1]));
		}
		
		reader.close();
		
		return disequilibriumElements;
	}
}
