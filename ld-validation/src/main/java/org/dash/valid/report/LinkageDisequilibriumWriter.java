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
import java.util.logging.Logger;

import org.dash.valid.freq.Frequencies;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.handler.LinkageDisequilibriumFileHandler;
import org.dash.valid.handler.LinkageWarningFileHandler;

public class LinkageDisequilibriumWriter {	
	private static LinkageDisequilibriumWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(LinkageDisequilibriumWriter.class.getName());

	static {
		try {
			FILE_LOGGER.addHandler(new LinkageDisequilibriumFileHandler());
			FILE_LOGGER.addHandler(new LinkageWarningFileHandler());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private LinkageDisequilibriumWriter() {
		
	}
	
	public static LinkageDisequilibriumWriter getInstance() {
		if (instance == null) {
			instance = new LinkageDisequilibriumWriter();
		}
		
		return instance;
	}
	/**
	 * @param linkagesFound
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public synchronized void reportDetectedLinkages(DetectedLinkageFindings findings) throws SecurityException, IOException {				
		StringBuffer sb = new StringBuffer("Id: " + findings.getGenotypeList().getId() + GLStringConstants.NEWLINE + "GL String: " + findings.getGenotypeList().getGLString());
		sb.append(GLStringConstants.NEWLINE + GLStringConstants.NEWLINE + "HLA DB Version: " + findings.getHladb() + GLStringConstants.NEWLINE);
		
		sb.append(GLStringConstants.NEWLINE + "Frequencies:  " + Frequencies.lookup(System.getProperty(Frequencies.FREQUENCIES_PROPERTY)) + GLStringConstants.NEWLINE);
				
		if (!findings.hasLinkages()) {
			sb.append(GLStringConstants.NEWLINE + "WARNING - NO LINKAGES FOUND" + GLStringConstants.NEWLINE);
		}
				
		for (DetectedDisequilibriumElement linkage : findings.getLinkages()) {
			sb.append(GLStringConstants.NEWLINE);
			sb.append("We found linkages:" + GLStringConstants.NEWLINE);
			sb.append(linkage.toString());
		}
		
		sb.append(GLStringConstants.NEWLINE + "***************************************" + GLStringConstants.NEWLINE);
	
		if (findings.hasAnomalies()) {
			FILE_LOGGER.warning(sb.toString());
		}
		else {
			FILE_LOGGER.info(sb.toString());
		}
	}
}
