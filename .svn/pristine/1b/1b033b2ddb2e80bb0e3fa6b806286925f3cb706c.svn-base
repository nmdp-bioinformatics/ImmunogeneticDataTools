package org.dash.valid;

public class BCDisequilibriumElement extends DisequilibriumElement {
	private String hlabElement;
	private String hlacElement;
	
	public BCDisequilibriumElement(String hlabElement, String hlacElement, String frequency, String note) {
		this.hlabElement = hlabElement;
		this.hlacElement = hlacElement;
		setFrequency(frequency);
		setNote(note);
	}
	
	public String getHlabElement() {
		return hlabElement;
	}
	public void setHlabElement(String bElement) {
		this.hlabElement = bElement;
	}
	public String getHlacElement() {
		return hlacElement;
	}
	public void setHlacElement(String cElement) {
		this.hlacElement = cElement;
	}
	
	public String toString() {
		return ("B Locus: " + this.getHlabElement() + "\n" + 
				"C Locus: " + this.getHlacElement() + "\n" + 
				"NegLocFreq: " + this.getFrequency() + "\n" + 
				"Notes: " + this.getNote());
	}
}
