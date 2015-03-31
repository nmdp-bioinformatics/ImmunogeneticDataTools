package org.dash.valid.base;

import org.dash.valid.DRDQDisequilibriumElement;


public class BaseDRDQDisequilibriumElement extends DRDQDisequilibriumElement implements BaseDisequilibriumElement {
	private String hladqa1Element;
	private String frequency;
	private String note;
	
	public BaseDRDQDisequilibriumElement(String hladrb1Element, String hladrb345Element, String hladqa1Element, String hladqb1Element, String frequency, String note) {
		setHladrb1Element(hladrb1Element);
		setHladrb345Element(hladrb345Element);
		setHladqa1Element(hladqa1Element);
		setHladqb1Element(hladqb1Element);
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
	
	public String getHladqa1Element() {
		return hladqa1Element;
	}

	public void setHladqa1Element(String hladqa1Element) {
		this.hladqa1Element = hladqa1Element;
	}

	@Override
	public String toString()  {
		return ("DRB1 Locus: " + this.getHladrb1Element() + "\n" + 
				"DRB345 Locus: " + this.getHladrb345Element() + "\n" + 
				"DQA1 Locus: " + this.getHladqa1Element() + "\n" + 
				"DQB1 Locus: " + this.getHladqb1Element() + "\n" + 
				"NegLocFreq: " + this.getFrequency() + "\n" + 
				"Notes: " + this.getNote());
	}

}
