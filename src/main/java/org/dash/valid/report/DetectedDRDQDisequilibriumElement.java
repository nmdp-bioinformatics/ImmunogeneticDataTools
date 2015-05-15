package org.dash.valid.report;

import org.dash.valid.DRDQDisequilibriumElement;
import org.dash.valid.Locus;
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
	
	@Override
	public LinkageHitDegree getHitDegree(Locus locus) {
		switch (locus) {
		case HLA_DRB1:
			return getDrb1HitDegree();
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRB345:
		case HLA_DRBX:
			return getDrb345HitDegree();
		case HLA_DQB1:
			return getDqb1HitDegree();
		case HLA_DQA1:
			return getDqa1HitDegree();
		default:
			LOGGER.warning("Attempted to retrieve hitDegree for unexpected locus: " + locus);
			return null;
		}
	}
	
	@Override
	public void setHitDegree(Locus locus, LinkageHitDegree hitDegree) {
		switch (locus) {
		case HLA_DRB1:
			setDrb1HitDegree(hitDegree);
			break;
		case HLA_DRB3:
		case HLA_DRB4:
		case HLA_DRB5:
		case HLA_DRB345:
		case HLA_DRBX:
			setDrb345HitDegree(hitDegree);
			break;
		case HLA_DQB1:
			setDqb1HitDegree(hitDegree);
			break;
		case HLA_DQA1:
			setDqa1HitDegree(hitDegree);
			break;
		default:
			LOGGER.warning("Attempted to set hitDegree for unexpected locus: " + locus);
			break;
		}
	}

	private LinkageHitDegree getDqa1HitDegree() {
		return dqa1HitDegree;
	}

	private void setDqa1HitDegree(LinkageHitDegree dqa1HitDegree) {
		this.dqa1HitDegree = dqa1HitDegree;
	}

	private LinkageHitDegree getDrb1HitDegree() {
		return drb1HitDegree;
	}

	private void setDrb1HitDegree(LinkageHitDegree drb1HitDegree) {
		this.drb1HitDegree = drb1HitDegree;
	}

	private LinkageHitDegree getDrb345HitDegree() {
		return drb345HitDegree;
	}

	private void setDrb345HitDegree(LinkageHitDegree drb345HitDegree) {
		this.drb345HitDegree = drb345HitDegree;
	}

	private LinkageHitDegree getDqb1HitDegree() {
		return dqb1HitDegree;
	}

	private void setDqb1HitDegree(LinkageHitDegree dqb1HitDegree) {
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
