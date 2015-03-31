package org.dash.valid;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

class LinkageDisequilibriumFileHandler extends FileHandler implements Filter {
	private static LinkageDisequilibriumFileHandler instance = null;
	private LinkageDisequilibriumFileHandler() throws IOException, SecurityException {
		super("./linkages.log", true);
		setFormatter(new SimpleFormatter());
		setLevel(Level.INFO);
	}
	
	public static LinkageDisequilibriumFileHandler getInstance() throws IOException,
		SecurityException {
		if (instance == null) {
			instance = new LinkageDisequilibriumFileHandler();
		}
		
		return instance;
	}

	public boolean isLoggable(LogRecord record) {
		if (record.getLevel() == Level.INFO) {
			return true;
		}
		
		return false;
	}
}
