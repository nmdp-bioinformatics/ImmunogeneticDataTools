package org.dash.valid.report;

import java.util.List;

import org.dash.valid.Sample;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import org.dash.valid.SkippedSample;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="samples")
public class SamplesList {

	private List<Sample> samples = null;

	@XmlElements({
		@XmlElement(name="sample", type=Sample.class),
		@XmlElement(name="skipped", type=SkippedSample.class)
	})
	public List<Sample> getSamples() {
		return samples;
	}

	public void setSamples(List<Sample> samples) {
		this.samples = samples;
	}
}
