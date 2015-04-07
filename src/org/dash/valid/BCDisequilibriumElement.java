package org.dash.valid;

public abstract class BCDisequilibriumElement {
	private String hlabElement;
	private String hlacElement;
	
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
	
	public abstract String getFrequencyInfo();
}
