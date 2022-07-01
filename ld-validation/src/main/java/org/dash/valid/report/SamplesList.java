package org.dash.valid.report;

import java.util.List;

import org.dash.valid.Sample;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="samples")
public class SamplesList {

	private List<Sample> samples = null;

	@XmlElement(name="sample")
	public List<Sample> getSamples() {
		return samples;
	}

	public void setSamples(List<Sample> samples) {
		this.samples = samples;
	}
}
