package org.dash.valid;

import org.dash.valid.gl.SkippedGenotype;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Entry in a sample list for a genotype that was skipped rather than analysed. Written as a
 * {@code <skipped id= line= reason=/>} element in the XML, in place of a {@code <sample>}.
 * It carries no findings, so code that walks a sample list for findings must skip these.
 */
@XmlRootElement(name="skipped")
@XmlType(propOrder={})
public class SkippedSample extends Sample {
	private SkippedGenotype skipped;

	public SkippedSample() {
	}

	public SkippedSample(SkippedGenotype skipped) {
		this.skipped = skipped;
	}

	@Override
	@XmlAttribute(name="id")
	public String getId() { return skipped.getId(); }

	@XmlAttribute(name="line")
	public Integer getLine() { return skipped.getLine(); }

	@XmlAttribute(name="reason")
	public String getReason() { return skipped.getReason(); }

	@Override
	@XmlTransient
	public String getNote() { return null; }

	@Override
	@XmlTransient
	public String getProcessedGlString() { return null; }

	@Override
	@XmlTransient
	public String getGlString() { return null; }

	@Override
	@XmlTransient
	public org.dash.valid.report.DetectedLinkageFindings getFindings() { return null; }

	public void setId(String id) { }
	public void setLine(Integer line) { }
	public void setReason(String reason) { }
}
