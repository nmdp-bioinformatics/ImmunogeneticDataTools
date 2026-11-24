package org.dash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Sample;
import org.dash.valid.SkippedSample;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.report.SamplesList;
import org.dash.valid.report.SummaryWriter;
import org.junit.jupiter.api.Test;

public class SkippedSampleXmlTest {

	@Test
	public void testSkippedGenotypeIsReportedAndBlankLineIsNot() throws Exception {
		StringBuilder ambiguousB = new StringBuilder("HLA-B*99:01");
		for (int i = 2; i <= 21; i++) {
			ambiguousB.append(GLStringConstants.ALLELE_AMBIGUITY_DELIMITER).append(String.format("HLA-B*99:%02d", i));
		}
		String input = "ok\tHLA-A*01:01+HLA-A*02:01\n\ntooAmbiguous\tHLA-A*01:01"
				+ GLStringConstants.GENE_DELIMITER + ambiguousB + "\n";

		List<Sample> samples = LinkageDisequilibriumAnalyzer.analyzeGLStringFile("x.txt",
				new BufferedReader(new StringReader(input)));

		assertEquals(2, samples.size(), "one analysed sample (no blank-line sample) plus one skipped");
		assertFalse(samples.get(0) instanceof SkippedSample);
		assertTrue(samples.get(1) instanceof SkippedSample);

		SamplesList list = new SamplesList();
		list.setSamples(samples);
		String xml = SummaryWriter.formatDetectedLinkages(list);
		assertTrue(xml.contains("<skipped id=\"tooAmbiguous\" line=\"3\" reason=\""), xml);
		assertEquals(1, xml.split("<sample ").length - 1, xml);
	}
}
