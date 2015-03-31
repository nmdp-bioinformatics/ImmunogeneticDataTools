package org.dash.valid;

public abstract class DRDQDisequilibriumElement {
	private String hladrb1Element;
	private String hladrb345Element;
	private String hladqb1Element;
	
	public String getHladrb1Element() {
		return hladrb1Element;
	}
	public void setHladrb1Element(String drb1Element) {
		this.hladrb1Element = drb1Element;
	}
	public String getHladrb345Element() {
		return hladrb345Element;
	}
	public void setHladrb345Element(String drb345Element) {
		this.hladrb345Element = drb345Element;
	}

	public String getHladqb1Element() {
		return hladqb1Element;
	}

	public void setHladqb1Element(String hladqb1Element) {
		this.hladqb1Element = hladqb1Element;
	}
	
	@Override
	public abstract String toString();
}
