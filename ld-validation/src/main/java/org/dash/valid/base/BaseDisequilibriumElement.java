package org.dash.valid.base;

import java.util.HashMap;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;

public class BaseDisequilibriumElement extends DisequilibriumElement {
	private String frequency;
	private String note;
	
	public BaseDisequilibriumElement(HashMap<Locus, String> hlaElementMap, String frequency, String note) {
		super(hlaElementMap);
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
