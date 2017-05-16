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

import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.Logger;

import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.handler.HaplotypePairFileHandler;
import org.dash.valid.handler.HaplotypePairWarningFileHandler;

public class HaplotypePairWriter {	
	private static HaplotypePairWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(HaplotypePairWriter.class.getName());
	
	private HaplotypePairWriter() {
		try {
			FILE_LOGGER.addHandler(new HaplotypePairFileHandler());
			FILE_LOGGER.addHandler(new HaplotypePairWarningFileHandler());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static HaplotypePairWriter getInstance() {
		if (instance == null) {
			instance = new HaplotypePairWriter();
		}
		
		return instance;
	}
	/**
	 * @param linkagesFound 
	 */
	public synchronized void reportDetectedLinkages(DetectedLinkageFindings findings) {				
		String linkages = formatDetectedLinkages(findings);
	
		if (findings.hasAnomalies()) {
			FILE_LOGGER.warning(linkages);
		}
		else {
			FILE_LOGGER.info(linkages);
		}
	}

	public static String formatDetectedLinkages(DetectedLinkageFindings findings) {
		StringBuffer sb = new StringBuffer("Id: " + findings.getGLId() + GLStringConstants.NEWLINE + "GL String: " + findings.getGLString());
		sb.append(GLStringConstants.NEWLINE + GLStringConstants.NEWLINE + "HLA DB Version: " + findings.getHladb() + GLStringConstants.NEWLINE);
		
		sb.append(GLStringConstants.NEWLINE + "Frequencies:  " + findings.getFrequencies() + GLStringConstants.NEWLINE);
		
		for (EnumSet<Locus> findingSought : findings.getFindingsSought()) {
			if (findings.hasLinkedPairs(findingSought) && findings.getFirstPair(findingSought) != null) {
				sb.append(GLStringConstants.NEWLINE + "First " + findingSought + " Haplotype pair:" + GLStringConstants.NEWLINE + findings.getFirstPair(findingSought));
			}
			else {
				sb.append(GLStringConstants.NEWLINE + "WARNING - No " + findingSought + " haplotype pairs detected." + GLStringConstants.NEWLINE);
			}
		}
		
		for (HaplotypePair pair : findings.getLinkedPairs()) {
			if (findings.getFirstPairs().contains(pair)) {
				continue;
			}
			else {
				sb.append(GLStringConstants.NEWLINE + "Possible " + pair.getLoci() + " Haplotype Pair:" + GLStringConstants.NEWLINE);
			}
			sb.append(pair);
		}
		
		sb.append(GLStringConstants.NEWLINE + "***************************************" + GLStringConstants.NEWLINE);
		return sb.toString();
	}
}
