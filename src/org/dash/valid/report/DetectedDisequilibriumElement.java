package org.dash.valid.report;

public abstract class DetectedDisequilibriumElement {
	private Object disequilibriumElement;

	public Object getDisequilibriumElement() {
		return disequilibriumElement;
	}

	public void setDisequilibriumElement(Object disequilibriumElement) {
		this.disequilibriumElement = disequilibriumElement;
	}
	
	public abstract String toString();
}
