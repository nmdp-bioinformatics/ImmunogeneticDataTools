package org.dash.valid.handler;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class LoggingFilter implements Filter {

	@Override
	public boolean isLoggable(LogRecord record) {
		// TODO Auto-generated method stub
		if (record.getClass().getPackage().getName().contains("org.dash.valid")) {
			return true;
		}
		return false;
	}

}
