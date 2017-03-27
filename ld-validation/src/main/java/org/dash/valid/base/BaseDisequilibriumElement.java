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
package org.dash.valid.base;

import java.util.HashMap;
import java.util.List;

import org.dash.valid.CoreDisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;

public class BaseDisequilibriumElement extends CoreDisequilibriumElement {
	private String frequency;
	private String note;
	
	public BaseDisequilibriumElement(HashMap<Locus, List<String>> hlaElementMap, String frequency, String note) {
		setHlaElementMap(hlaElementMap);
		setFrequency(frequency);
		setNote(note);
	}
	
	public String getFrequency() {
		return this.frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}

	public String getNote() {
		return this.note;
	}

	public void setNote(String note) {
		this.note = note;
	}
	
	@Override
	public String getFrequencyInfo() {
		return ("NegLocFreq: " + this.getFrequency() + GLStringConstants.NEWLINE + 
				"Notes: " + this.getNote() + GLStringConstants.NEWLINE);
	}
}
