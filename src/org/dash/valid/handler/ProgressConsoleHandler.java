package org.dash.valid.handler;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class ProgressConsoleHandler extends ConsoleHandler implements Filter {
	public ProgressConsoleHandler() throws IOException, SecurityException {
		super();
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
