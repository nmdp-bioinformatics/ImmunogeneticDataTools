/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
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
