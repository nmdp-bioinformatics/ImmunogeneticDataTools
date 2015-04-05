package org.dash.valid.cwd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dash.valid.HLADatabaseVersion;
import org.dash.valid.gl.GLStringConstants;

public class CommonWellDocumentedLoader {
	private static CommonWellDocumentedLoader instance = null;
	
	private Set<String> cwdAlleles;
	
	private CommonWellDocumentedLoader(HLADatabaseVersion hladb) {
		init(hladb);
	}
	
	public static CommonWellDocumentedLoader getInstance() {
		HLADatabaseVersion hladb = null;
		if (instance == null) {
			hladb = HLADatabaseVersion.lookup(System.getProperty(HLADatabaseVersion.HLADB_PROPERTY));
			instance = new CommonWellDocumentedLoader(hladb);
		}
		
		return instance;
	}
	
	private void init(HLADatabaseVersion hladb) {
		try {
			loadCommonWellDocumentedAlleles(hladb);
		}
		catch (FileNotFoundException e) {
			
		}
		catch (IOException e) {
			
		}
	}
	
	private void loadCommonWellDocumentedAlleles(HLADatabaseVersion hladb) throws IOException, FileNotFoundException {		
		File cwdFile = new File("resources/reference/CWD.txt");
		
		Set<String> cwdSet = new HashSet<String>();
		
		InputStream in = new FileInputStream(cwdFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String row;
		String[] columns;
		int idx = 0;
		
		int hladbIdx = -1;
		
		while ((row = reader.readLine()) != null) {
			columns = row.split(GLStringConstants.TAB);
			if (idx < 1) {
				List<String> headers = Arrays.asList(columns);

				hladbIdx = headers.indexOf(hladb.getCwdName());			}
			else {
				cwdSet.add(GLStringConstants.HLA_DASH + columns[hladbIdx]);
			}
			
			idx++;
		}
		
		setCwdAlleles(cwdSet);
		
		reader.close();
	}
	
	public Set<String> getCwdAlleles() {
		return this.cwdAlleles;
	}

	private void setCwdAlleles(Set<String> cwdAlleles) {
		this.cwdAlleles = cwdAlleles;
	}
}
