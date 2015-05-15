package org.dash.valid.report;

import org.dash.valid.BCDisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringConstants;

public class DetectedBCDisequilibriumElement extends DetectedDisequilibriumElement {
	private LinkageHitDegree bHitDegree;
	private LinkageHitDegree cHitDegree;
		
	public DetectedBCDisequilibriumElement(BCDisequilibriumElement disequilibriumElement) {
		setDisequilibriumElement(disequilibriumElement);
	}
	
	@Override
	public BCDisequilibriumElement getDisequilibriumElement() {
		return (BCDisequilibriumElement) super.getDisequilibriumElement();
	}
	
	@Override
	public LinkageHitDegree getHitDegree(Locus locus) {
		switch (locus) {
		case HLA_B:
			return getbHitDegree();
		case HLA_C:
			return getcHitDegree();
		default:
			LOGGER.warning("Attempted to retrieve hitDegree for unexpected locus: " + locus);
			return null;
		}
	}
	
	private LinkageHitDegree getbHitDegree() {
		return bHitDegree;
	}

	private void setbHitDegree(LinkageHitDegree bHitDegree) {
		this.bHitDegree = bHitDegree;
	}

	private LinkageHitDegree getcHitDegree() {
		return cHitDegree;
	}

	private void setcHitDegree(LinkageHitDegree cHitDegree) {
		this.cHitDegree = cHitDegree;
	}
	
	@Override
	public void setHitDegree(Locus locus, LinkageHitDegree hitDegree) {
		switch (locus) {
		case HLA_B:
			setbHitDegree(hitDegree);
			break;
		case HLA_C:
			setcHitDegree(hitDegree);
			break;
		default:
			LOGGER.warning("Attempted to set hitDegree for unexpected locus: " + locus);
			break;
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("B Locus: " + getDisequilibriumElement().getHlabElement() + " (" + getbHitDegree() + ")" + GLStringConstants.NEWLINE +
				"C Locus: " + getDisequilibriumElement().getHlacElement() + " (" + getcHitDegree() + ")" + GLStringConstants.NEWLINE +  
				getDisequilibriumElement().getFrequencyInfo());
		
		return sb.toString();
	}
}
