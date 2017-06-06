package org.dash.valid.report;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="gl-freqs")
public class DetectedLinkageFindingsList {

	private List<DetectedLinkageFindings> findings = null;

	@XmlElement(name="gl-freq")
	public List<DetectedLinkageFindings> getFindings() {
		return findings;
	}

	public void setFindings(List<DetectedLinkageFindings> findings) {
		this.findings = findings;
	}
}
