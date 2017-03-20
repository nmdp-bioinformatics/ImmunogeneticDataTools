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
	   cell.setCellValue(findings.getGLId());
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getAlleleCount(Locus.HLA_A));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getAlleleCount(Locus.HLA_B));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getAlleleCount(Locus.HLA_C));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getAlleleCount(Locus.HLA_DRB1));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getAlleleCount(Locus.HLA_DRB345));
	   cell = row.createCell(cellId++);
	   cell.setCellValue(findings.getAlleleCount(Locus.HLA_DQB1));
	   for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
		   cell = row.createCell(cellId++);
		   cell.setCellValue(findings.getLinkageCount(linkage.getLoci()));
		   cell = row.createCell(cellId++);
		   cell.setCellValue(findings.getMinimumDifference(linkage.getLoci()) + "");
	   }
	   
	   String detectedFindings = formatDetectedFindings(findings);
		
		printWriter.write(detectedFindings);
	}

	public String formatDetectedFindings(DetectedLinkageFindings findings) {
		StringBuffer sb = new StringBuffer();
			sb.append(findings.getGLId() + GLStringConstants.COMMA);
			sb.append(findings.getAlleleCount(Locus.HLA_A) + GLStringConstants.COMMA);
			sb.append(findings.getAlleleCount(Locus.HLA_B) + GLStringConstants.COMMA);
			sb.append(findings.getAlleleCount(Locus.HLA_C) + GLStringConstants.COMMA);
			sb.append(findings.getAlleleCount(Locus.HLA_DRB1) + GLStringConstants.COMMA);
			sb.append(findings.getAlleleCount(Locus.HLA_DRB345) + GLStringConstants.COMMA);
			sb.append(findings.getAlleleCount(Locus.HLA_DQB1) + GLStringConstants.COMMA);
			for (Linkages linkage : LinkagesLoader.getInstance().getLinkages()) {
				sb.append(findings.getLinkageCount(linkage.getLoci()) + GLStringConstants.COMMA);
				sb.append(findings.getMinimumDifference(linkage.getLoci()) + GLStringConstants.COMMA);
			}
			sb.append(GLStringConstants.NEWLINE);
		return sb.toString();
	}
}
