package org.dash.valid.handler;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class HaplotypePairWarningFileHandler extends FileHandler implements Filter {
	public HaplotypePairWarningFileHandler() throws IOException, SecurityException {
		super("./haplotypePairWarnings.log", true);
		setFormatter(new SimpleFormatter());
		setLevel(Level.WARNING);
	}

	@Override
	public boolean isLoggable(LogRecord record) {
		if (record.getLevel() == Level.WARNING) {
			return true;
		}
		
		return false;
	}
}
