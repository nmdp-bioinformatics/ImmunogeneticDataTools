package org.dash.valid;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.dash.valid.gl.GLStringConstants;

// TODO:  Write tests
public class LinkagesLoader {
	private static LinkagesLoader instance = null;
	private Set<Linkages> linkages = null;
	
	private LinkagesLoader() {
		Set<String> linkageNames = new HashSet<String>();
		String linkageProperties = System.getProperty(Linkages.LINKAGES_PROPERTY);
		
		if (linkageProperties != null) {
			StringTokenizer st = new StringTokenizer(linkageProperties, GLStringConstants.SPACE);
			while (st.hasMoreTokens()) {
				linkageNames.add(st.nextToken());
			}
		}
		
		setLinkages(Linkages.lookup(linkageNames));
	}
	
	public static LinkagesLoader getInstance() {
		if (instance == null) {
			instance = new LinkagesLoader();
		}
		
		return instance;
	}
	
	private void setLinkages(Set<Linkages> linkages) {
		this.linkages = linkages;
	}
	
	public Set<Linkages> getLinkages() {
		return linkages;
	}
}
