package org.dash.valid.report;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;


public class DetectedDisequilibriumElement {
	private DisequilibriumElement disequilibriumElement;
	HashMap<Locus, LinkageHitDegree> linkageHitDegreeMap = new HashMap<Locus, LinkageHitDegree>();
	
    protected static final Logger LOGGER = Logger.getLogger(DetectedDisequilibriumElement.class.getName());
    
    public DetectedDisequilibriumElement(DisequilibriumElement disequilibriumElement) {
    	this.disequilibriumElement = disequilibriumElement;
    }

	public DisequilibriumElement getDisequilibriumElement() {
		return disequilibriumElement;
	}

	public void setDisequilibriumElement(DisequilibriumElement disequilibriumElement) {
		this.disequilibriumElement = disequilibriumElement;
	}
		
	public LinkageHitDegree getHitDegree(Locus locus) {
		return linkageHitDegreeMap.get(locus);
	}
	
	public void setHitDegree(Locus locus, LinkageHitDegree hitDegree) {
		linkageHitDegreeMap.put(locus, hitDegree);
	}
	
	public Set<Locus> getLoci() {
		return linkageHitDegreeMap.keySet();
	}
	
	public String toString() {		
		StringBuffer sb = new StringBuffer();
		
		for (Locus locus : getDisequilibriumElement().getLoci()) {
			sb.append(locus.getShortName() + " Locus: " + getDisequilibriumElement().getHlaElement(locus) + " (" + getHitDegree(locus) + ")" + GLStringConstants.NEWLINE);
		}
				
		sb.append(getDisequilibriumElement().getFrequencyInfo());
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object element) {
		if (getDisequilibriumElement().equals(((DetectedDisequilibriumElement) element).getDisequilibriumElement())) {
			return true;
		}
		
		return false;
	}
}
