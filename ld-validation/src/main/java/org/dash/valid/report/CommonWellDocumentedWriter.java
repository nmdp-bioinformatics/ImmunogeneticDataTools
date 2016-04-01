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

import org.dash.valid.cwd.CommonWellDocumentedLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.handler.CommonWellDocumentedFileHandler;

public class CommonWellDocumentedWriter {
	private static CommonWellDocumentedWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(CommonWellDocumentedWriter.class.getName());

	static {
		try {
			FILE_LOGGER.addHandler(new CommonWellDocumentedFileHandler());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private CommonWellDocumentedWriter() {
		
	}
	
	public static CommonWellDocumentedWriter getInstance() {
		if (instance == null) {
			instance = new CommonWellDocumentedWriter();
		}
		
		return instance;
	}
	public synchronized void reportCommonWellDocumented(DetectedLinkageFindings findings) {
		if (findings.getNonCWDAlleles() == null || findings.getNonCWDAlleles().size() == 0) {
			return;
		}
		
		StringBuffer sb = new StringBuffer("Id: " + findings.getGenotypeList().getId() + GLStringConstants.NEWLINE + "GL String: " + findings.getGenotypeList().getGLString());
		sb.append(GLStringConstants.NEWLINE + GLStringConstants.NEWLINE + "HLA DB Version: " + findings.getHladb() + GLStringConstants.NEWLINE);
		
		CommonWellDocumentedLoader loader = CommonWellDocumentedLoader.getInstance();
		String accession;
		
		for (String allele : findings.getNonCWDAlleles()) {
			sb.append("WARNING - Allele: " + allele + " not in the CWD list for HLA DB: " + findings.getHladb());
			accession = loader.getAccessionByAllele(allele);
			if (accession != null) {
				sb.append(" (Found under accession: " + accession + " in these HLA DBs: " + 
					loader.getHlaDbsByAccession(accession) + ")");
			}
			sb.append(GLStringConstants.NEWLINE);
		}
		
		FILE_LOGGER.warning(sb.toString());
	}
}
