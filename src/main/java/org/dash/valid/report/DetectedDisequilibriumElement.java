package org.dash.valid.report;

import java.util.logging.Logger;

import org.dash.valid.Locus;


public abstract class DetectedDisequilibriumElement {
	private Object disequilibriumElement;
	
    protected static final Logger LOGGER = Logger.getLogger(DetectedDisequilibriumElement.class.getName());

	public Object getDisequilibriumElement() {
		return disequilibriumElement;
	}

	public void setDisequilibriumElement(Object disequilibriumElement) {
		this.disequilibriumElement = disequilibriumElement;
	}
	
	public abstract String toString();
	
	public abstract LinkageHitDegree getHitDegree(Locus locus);
	
	public abstract void setHitDegree(Locus locus, LinkageHitDegree hitDegree);
	
	@Override
	public boolean equals(Object element) {
		if (getDisequilibriumElement().equals(((DetectedDisequilibriumElement) element).getDisequilibriumElement())) {
			return true;
		}
		
		return false;
	}
}
