package org.dash.valid;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;

public class LinkageDisequilibriumWriter {
	    
	/**
	 * @param linkagesFound
	 */
	public static void reportDetectedLinkages(LinkageDisequilibriumGenotypeList linkedGLString, 
			HashMap<DisequilibriumElement, Boolean> linkagesFound) {
		StringBuffer sb = new StringBuffer("Your GL String: " + linkedGLString.getGLString());

		if (linkagesFound == null || linkagesFound.size() == 0) {
			sb.append("\n\n");
			sb.append("NO LINKAGES FOUND\n");
		}
		for (DisequilibriumElement linkages : linkagesFound.keySet()) {
			sb.append("\n\n");
			if (linkagesFound.get(linkages).equals(Boolean.TRUE)) {
				sb.append("We found perfect linkages:\n");
			}
			else {
				sb.append("We found partial linkages:\n");
			}
			sb.append(linkages);
		}
		sb.append("\n***************************************\n");
		
		class LinkageDisequilibriumFileHandler extends FileHandler implements Filter {
			public LinkageDisequilibriumFileHandler() throws IOException,
				SecurityException {
			super("%h/linkages.log", true);
			setFormatter(new SimpleFormatter());
			setLevel(Level.INFO);
		}

			public boolean isLoggable(LogRecord record) {
				if (record.getLevel() == Level.INFO) {
					return true;
				}
				
				return false;
			}
		};
		
		Handler handler = null;
		Logger FILE_LOGGER = Logger.getLogger(LinkageDisequilibriumWriter.class.getName());
		try {
			handler = new LinkageDisequilibriumFileHandler();
			FILE_LOGGER.addHandler(handler);
	
			FILE_LOGGER.info(sb.toString());
		}
		catch (IOException ioe) {
			
		}
		finally {
			handler.close();
		}
	}
}
