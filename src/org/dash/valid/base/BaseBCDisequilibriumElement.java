package org.dash.valid.base;

import org.dash.valid.BCDisequilibriumElement;

public class BaseBCDisequilibriumElement extends BCDisequilibriumElement implements BaseDisequilibriumElement {
	private String frequency;
	private String note;
	
	public BaseBCDisequilibriumElement(String hlabElement, String hlacElement, String frequency, String note) {
		setHlabElement(hlabElement);
		setHlacElement(hlacElement);
		setFrequency(frequency);
		setNote(note);
}
	
	@Override
	public String getFrequency() {
		return this.frequency;
	}

	@Override
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}

	@Override
	public String getNote() {
		return this.note;
	}

	@Override
	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public String toString()  {
		return ("B Locus: " + this.getHlabElement() + "\n" + 
				"C Locus: " + this.getHlacElement() + "\n" + 
				"NegLocFreq: " + this.getFrequency() + "\n" + 
				"Notes: " + this.getNote());
	}

}
