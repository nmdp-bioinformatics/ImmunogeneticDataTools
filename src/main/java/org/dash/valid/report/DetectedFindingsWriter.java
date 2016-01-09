package org.dash.valid.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;

public class DetectedFindingsWriter {	
	private static DetectedFindingsWriter instance = null;
	private static Logger LOGGER = Logger.getLogger(DetectedFindingsWriter.class.getName());
	private static XSSFWorkbook workbook;
	private static XSSFSheet spreadsheet;
	private static FileOutputStream out;
	private static int rowId = 0;
	private static FileWriter fileWriter;
	private static PrintWriter printWriter;

	static {
		try {			
			  fileWriter = new FileWriter("./detectedFindings.csv");
			  printWriter = new PrintWriter(fileWriter);
			
		      //Create blank workbook
		      workbook = new XSSFWorkbook(); 
		      //Create a blank sheet
		      spreadsheet = workbook.createSheet("Detected Findings");
		      out = new FileOutputStream(new File("./detectedFindings.xlsx")); 
		      
		      XSSFRow row = spreadsheet.createRow(rowId++);
		      
		      int cellId;
		      XSSFCell cell;
		      
		      String[] labels = new String[] {"Sample Id", "nA", "nB", "nC", "nDRB1", "nDRB345", "nDQB1"};
		      
		      for (cellId = 0; cellId < labels.length; cellId++) {
			      cell = row.createCell(cellId);
		    	  cell.setCellValue(labels[cellId]);
		      }
			  for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
				  cell = row.createCell(cellId++);
				  cell.setCellValue("n" + linkage.getLoci() + " Linkages");
				  cell = row.createCell(cellId++);
				  cell.setCellValue(linkage.getLoci() + " min L(genotype)");
			  }     		
		} catch (IOException e) {
			LOGGER.warning("Couldn't write to file detectedFindings.xlsx");
		}
	}
	
	public void closeWriters() {
		try {
			//Write the workbook in file system
			workbook.write(out);
		    out.close();
		    
			printWriter.flush();
			printWriter.close();
			fileWriter.close();
		}
		catch (IOException ioe) {
			LOGGER.warning("Couldn't close fileWriter after writing to detectedFindings.csv.");
		}
	}
	
	private DetectedFindingsWriter() {
		
	}
	
	public static DetectedFindingsWriter getInstance() {
		if (instance == null) {
			instance = new DetectedFindingsWriter();
		}
		
		return instance;
	}
	/**
	 * @param linkagesFound
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public synchronized void reportDetectedFindings(DetectedLinkageFindings findings) {				
	   XSSFRow row = spreadsheet.createRow(rowId++);
	   int cellId = 0;
	   XSSFCell cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getGenotypeList().getId());
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getGenotypeList().getAlleleCount(Locus.HLA_A));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getGenotypeList().getAlleleCount(Locus.HLA_B));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getGenotypeList().getAlleleCount(Locus.HLA_C));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getGenotypeList().getAlleleCount(Locus.HLA_DRB1));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getGenotypeList().getAlleleCount(Locus.HLA_DRB345));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getGenotypeList().getAlleleCount(Locus.HLA_DQB1));
	   for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
		   cell = row.createCell(cellId++);
		   cell.setCellValue(findings.getLinkageCount(linkage.getLoci()));
		   cell = row.createCell(cellId++);
		   cell.setCellValue(findings.getMinimumDifference(linkage.getLoci()) + "");
	   }
	   
		printWriter.write(findings.getGenotypeList().getId() + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_A) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_B) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_C) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_DRB1) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_DRB345) + GLStringConstants.COMMA);
		printWriter.write(findings.getGenotypeList().getAlleleCount(Locus.HLA_DQB1) + GLStringConstants.COMMA);
		for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
			printWriter.write(findings.getLinkageCount(linkage.getLoci()) + GLStringConstants.COMMA);
			printWriter.write(findings.getMinimumDifference(linkage.getLoci()) + GLStringConstants.COMMA);
		}
		printWriter.write(GLStringConstants.NEWLINE);
	}
}
