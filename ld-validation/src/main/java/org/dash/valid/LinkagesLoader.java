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
	
	private LinkagesLoader(Set<Linkages> linkages) {
		setLinkages(linkages);
	}
	
	public static LinkagesLoader getInstance() {
		if (instance == null) {
			instance = new LinkagesLoader();
		}
		
		return instance;
	}
	
	public static LinkagesLoader getInstance(Set<Linkages> linkages) {

		if (instance == null) {
			instance = new LinkagesLoader(linkages);
		}

		return instance;
	}

	// Forces the next getInstance() call (either overload) to rebuild from scratch instead of
	// returning whatever's cached. Phase 8: ld-service needs this for the same reason
	// HLAFrequenciesLoader does (see its own reset()) -- a long-running process, unlike the
	// CLI's one-shot-per-invocation model, can outlive whichever request happened to initialize
	// this singleton first. The gap here is more serious than HLAFrequenciesLoader's, though:
	// HLAFrequenciesLoader#init(Set<File>, File) derives which linkages to search for directly
	// from an uploaded custom frequency file's own content and pushes them here via
	// getInstance(Set<Linkages>) -- without resetting first, a later custom-file upload with
	// genuinely different loci than whatever request initialized this singleton first would
	// have its derived linkages silently discarded, still searching for the original ones.
	// Unlike HLAFrequenciesLoader, rebuilding here is cheap (a small lookup, no I/O), so callers
	// can reset unconditionally on every request with no caching tradeoff to weigh.
	public static void reset() {
		instance = null;
	}

	private void setLinkages(Set<Linkages> linkages) {
		this.linkages = linkages;
	}
	
	public Set<Linkages> getLinkages() {
		return linkages;
	}
	
	public Set<Locus> getLoci() {
		Set<Linkages> linkages = getLinkages();
		Set<Locus> loci = new HashSet<Locus>();
		for (Linkages linkage : linkages) {
			loci.addAll(linkage.getLoci());
		}
		
		return loci;
	}
}
