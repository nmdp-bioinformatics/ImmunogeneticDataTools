package org.dash.valid.handler;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class HaplotypePairFileHandler extends FileHandler implements Filter {
	public HaplotypePairFileHandler() throws IOException, SecurityException {
		super("./haplotypePairs.log", true);
		setFormatter(new SimpleFormatter());
		setLevel(Level.INFO);
	}

	@Override
	public boolean isLoggable(LogRecord record) {
		if (record.getLevel() == Level.INFO) {
			return true;
		}
		
		return false;
	}
}
