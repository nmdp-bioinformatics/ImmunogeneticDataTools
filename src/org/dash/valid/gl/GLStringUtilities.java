package org.dash.valid.gl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class GLStringUtilities {
	private static final String ALPHA_REGEX = "[A-Z]";
	private static final String GL_STRING_DELIMITER_REGEX = "[\\^\\|\\+~/]";
	public static final String ESCAPED_ASTERISK = "\\*";
	
	public static List<String> parse(String value, String delimiter) {
		List<String> elements = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(value, delimiter);
		while (st.hasMoreTokens()) {
			elements.add(st.nextToken());
		}
		
		return elements;
	}
	
	public static boolean validateGLStringFormat(String glString) {
		StringTokenizer st = new StringTokenizer(glString, GL_STRING_DELIMITER_REGEX);
		while (st.hasMoreTokens()) {
			if (!st.nextToken().startsWith("HLA-")) {
				System.out.println("GL String format is invalid");
				return false;
			}
		}
		
		return true;
	}
	
	public static String fullyQualifyGLString(String shorthand) {
		StringTokenizer st = new StringTokenizer(shorthand, GL_STRING_DELIMITER_REGEX, true);
		StringBuffer sb = new StringBuffer();
		String part;
		String locus = null;
		
		while (st.hasMoreTokens()) {
			part = st.nextToken();
			if (part.substring(0,1).matches(ALPHA_REGEX)) {
				if (!part.startsWith(GLStringConstants.HLA_DASH)){
					part = GLStringConstants.HLA_DASH + part;
				}

				String[] splitString = part.split(ESCAPED_ASTERISK);
				locus = splitString[0];
			}
			else if (part.substring(0,1).matches(GL_STRING_DELIMITER_REGEX)) {
				sb.append(part);
				continue;
			}
			else {
				part = fillLocus(locus, part);
			}
			
			sb.append(part);
		}
		
		return sb.toString();
	}
	
	public static String fillLocus(String locus, String segment) {
		if (!segment.substring(0,1).matches(ALPHA_REGEX)) {
			segment = locus + GLStringConstants.ASTERISK + segment;
		}
		return segment;
	}
	
	public static List<String> readGLStringFile(String filename) {
		File glStringFile = new File(filename);
		BufferedReader reader = null;
		String glString;
		List<String> glStrings = new ArrayList<String>();
		
		try {
			InputStream in = new FileInputStream(glStringFile);
			reader = new BufferedReader(new InputStreamReader(in));
			
			while ((glString = reader.readLine()) != null) {
				glStrings.add(glString);
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return glStrings;
	}
}
