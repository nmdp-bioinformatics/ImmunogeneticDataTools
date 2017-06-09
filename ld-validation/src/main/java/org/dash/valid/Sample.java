package org.dash.valid;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.report.DetectedLinkageFindings;

@XmlRootElement(name="sample")
@XmlType(propOrder={"glString", "findings", "processedGlString"})
public class Sample {
	private LinkageDisequilibriumGenotypeList genotypeList;
	private DetectedLinkageFindings findings;
	
	@XmlAttribute(name="id")
	public String getId() {
		return getGenotypeList().getId();
	}
	
	@XmlElement(name="processed-gl-string")
	public String getProcessedGlString() {
		return getGenotypeList().getGLString().equals(getGenotypeList().getSubmittedGlString()) ? null : getGenotypeList().getGLString();
	}
	
	@XmlElement(name="gl-string")
	public String getGlString() {
		return getGenotypeList().getSubmittedGlString();
	}
	
	@XmlElement(name="gl-freq")
	public DetectedLinkageFindings getFindings() {
		return findings;
	}
	public void setFindings(DetectedLinkageFindings findings) {
		this.findings = findings;
	}

	@XmlTransient
	public LinkageDisequilibriumGenotypeList getGenotypeList() {
		return genotypeList;
	}

	public void setGenotypeList(LinkageDisequilibriumGenotypeList genotypeList) {
		this.genotypeList = genotypeList;
	}
	
	public Sample(LinkageDisequilibriumGenotypeList genotypeList) {
		setGenotypeList(genotypeList);
	}
	
	public Sample() {
		
	}
}
