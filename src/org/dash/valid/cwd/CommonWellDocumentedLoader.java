package org.dash.valid.cwd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.gl.GLStringUtilities;

public class CommonWellDocumentedLoader {
	private static CommonWellDocumentedLoader instance = null;
	
	private HashMap<String, String> cwdAlleles;

	private CommonWellDocumentedLoader() {
		init();
	}
	
	public static CommonWellDocumentedLoader getInstance() {
		if (instance == null) {
			instance = new CommonWellDocumentedLoader();
		}
		
		return instance;
	}
	
	private void init() {
		try {
			loadCommonWellDocumentedAlleles();
		}
		catch (FileNotFoundException e) {
			
		}
		catch (IOException e) {
			
		}
	}
	
	private void loadCommonWellDocumentedAlleles() throws IOException, FileNotFoundException {		
		File cwdFile = new File("resources/CWD.txt");
		
		HashMap<String, String> map = new HashMap<String, String>();
		
		InputStream in = new FileInputStream(cwdFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String row;
		String[] columns;
		int idx = 0;
		String[] parts;
		
		while ((row = reader.readLine()) != null) {
			if (idx < 1) {
				continue;
			}
			
			columns = row.split(GLStringConstants.TAB);
			parts = columns[2].split(GLStringUtilities.ESCAPED_ASTERISK);
			map.put(GLStringConstants.HLA_DASH + parts[0], columns[2]);
			
			idx++;
		}
		
		setCwdAlleles(map);
		
		reader.close();
	}
	
	public HashMap<String, String> getCwdAlleles() {
		return this.cwdAlleles;
	}

	private void setCwdAlleles(HashMap<String, String> cwdAlleles) {
		this.cwdAlleles = cwdAlleles;
	}
}
