package org.dash.valid.report;

import org.dash.valid.DRDQDisequilibriumElement;
import org.dash.valid.base.BaseDRDQDisequilibriumElement;
import org.dash.valid.gl.GLStringConstants;

public class DetectedDRDQDisequilibriumElement extends DetectedDisequilibriumElement{
	private LinkageHitDegree drb1HitDegree;
	private LinkageHitDegree drb345HitDegree;
	private LinkageHitDegree dqb1HitDegree;
	private LinkageHitDegree dqa1HitDegree;
	
	public DetectedDRDQDisequilibriumElement(DRDQDisequilibriumElement disequilibriumElement) {
		setDisequilibriumElement(disequilibriumElement);
	}
	
	@Override
	public DRDQDisequilibriumElement getDisequilibriumElement() {
		return (DRDQDisequilibriumElement) super.getDisequilibriumElement();
	}

	public LinkageHitDegree getDqa1HitDegree() {
		return dqa1HitDegree;
	}

	public void setDqa1HitDegree(LinkageHitDegree dqa1HitDegree) {
		this.dqa1HitDegree = dqa1HitDegree;
	}

	public LinkageHitDegree getDrb1HitDegree() {
		return drb1HitDegree;
	}

	public void setDrb1HitDegree(LinkageHitDegree drb1HitDegree) {
		this.drb1HitDegree = drb1HitDegree;
	}

	public LinkageHitDegree getDrb345HitDegree() {
		return drb345HitDegree;
	}

	public void setDrb345HitDegree(LinkageHitDegree drb345HitDegree) {
		this.drb345HitDegree = drb345HitDegree;
	}

	public LinkageHitDegree getDqb1HitDegree() {
		return dqb1HitDegree;
	}

	public void setDqb1HitDegree(LinkageHitDegree dqb1HitDegree) {
		this.dqb1HitDegree = dqb1HitDegree;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(
				"DRB1 Locus: " + getDisequilibriumElement().getHladrb1Element() + " (" + getDrb1HitDegree() + ")" + GLStringConstants.NEWLINE + 
				"DRB345 Locus: " + getDisequilibriumElement().getHladrb345Element() + " (" + getDrb345HitDegree() + ")" + GLStringConstants.NEWLINE +  
				"DQB1 Locus: " + getDisequilibriumElement().getHladqb1Element() + " (" + getDqb1HitDegree() + ")" + GLStringConstants.NEWLINE);
		
		if (getDisequilibriumElement() instanceof BaseDRDQDisequilibriumElement) {
				sb.append("DQA1 Locus: " + ((BaseDRDQDisequilibriumElement)getDisequilibriumElement()).getHladqa1Element() + " (" + getDqa1HitDegree() + ")" + GLStringConstants.NEWLINE);
		}
				
		sb.append(getDisequilibriumElement().getFrequencyInfo());
		
		return sb.toString();
	}
}
