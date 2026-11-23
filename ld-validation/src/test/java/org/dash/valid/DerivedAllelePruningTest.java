package org.dash.valid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// An uploaded haplotype file needs no separate alleles list: every allele in it has a frequency, so
// the loader derives the allowed-allele set from it and prunes ambiguous genotype alleles to match
// (issue 89). The pruned list is what the analysis uses (fullValue); the list as submitted is kept
// alongside as originalValue.
public class DerivedAllelePruningTest {

	@TempDir
	Path tempDir;

	@BeforeEach
	@AfterEach
	public void resetLoaders() {
		LinkagesLoader.reset();
		HLAFrequenciesLoader.reset();
	}

	private static final String ROWS = "HIS,HLA-C*01:02~HLA-B*08:01g,1.0E-4\n"
			+ "HIS,HLA-C*07:01~HLA-B*07:02,6.0E-5\n";
	private static final String HET_C = "HLA-C*01:02:01+HLA-C*07:01:01:01";

	private Haplotype firstHaplotype(String glString) throws IOException {
		Path file = tempDir.resolve("freqs.csv");
		Files.write(file, ROWS.getBytes(StandardCharsets.UTF_8));
		HLAFrequenciesLoader.getInstance(Collections.singleton(file.toFile()), (File) null);

		Sample sample = HLALinkageDisequilibrium.hasLinkageDisequilibrium(new LinkageDisequilibriumGenotypeList("test", glString));
		HaplotypePair pair = sample.getFindings().getLinkedPairs().iterator().next();
		for (Haplotype haplotype : pair.getHaplotypes()) {
			if (haplotype.getHaplotypeString().contains("B*08:01g")) {
				return haplotype;
			}
		}
		throw new AssertionError("no B*08:01g haplotype in " + pair);
	}

	@Test
	public void testAlleleWithNoFrequencyIsPrunedAndOriginalListKept() throws IOException {
		Haplotype haplotype = firstHaplotype(HET_C + "^HLA-B*08:01:01:01/HLA-B*15:01:01:01+HLA-B*07:02:01");

		assertFalse(haplotype.getFullHaplotypeString().contains("B*15:01"), haplotype.getFullHaplotypeString());
		// Kept alleles stay as submitted: swapping in the derived list's spelling (08:01g) could
		// pick a spelling other haplotype rows don't match and silently drop linkages.
		assertTrue(haplotype.getFullHaplotypeString().contains("HLA-B*08:01:01:01"), haplotype.getFullHaplotypeString());
		assertTrue(haplotype.getOriginalFullHaplotypeString().contains("HLA-B*08:01:01:01/HLA-B*15:01:01:01"),
				haplotype.getOriginalFullHaplotypeString());
	}

	@Test
	public void testExplicitAllelesFileOverridesDerivation() throws IOException {
		Path file = tempDir.resolve("freqs.csv");
		Files.write(file, ROWS.getBytes(StandardCharsets.UTF_8));
		Path alleles = tempDir.resolve("alleles.txt");
		Files.write(alleles, "HLA-B*15:01\n".getBytes(StandardCharsets.UTF_8));

		HLAFrequenciesLoader loader = HLAFrequenciesLoader.getInstance(Collections.singleton(file.toFile()), alleles.toFile());

		assertEquals("HLA-B*15:01", loader.hasFrequency(Locus.HLA_B, "HLA-B*15:01:01:01"));
		assertNull(loader.hasFrequency(Locus.HLA_B, "HLA-B*07:02:01"));
	}

	@Test
	public void testDerivedAllelesAreUnionedAcrossFilesSharingALocus() throws IOException {
		Path cb = tempDir.resolve("cb.csv");
		Files.write(cb, ROWS.getBytes(StandardCharsets.UTF_8));
		Path acb = tempDir.resolve("acb.csv");
		Files.write(acb, "HIS,HLA-A*01:01~HLA-C*03:04~HLA-B*15:01,1.0E-4\n".getBytes(StandardCharsets.UTF_8));

		HLAFrequenciesLoader loader = HLAFrequenciesLoader.getInstance(new java.util.LinkedHashSet<File>(Arrays.asList(cb.toFile(), acb.toFile())), (File) null);

		assertEquals("HLA-B*15:01", loader.hasFrequency(Locus.HLA_B, "HLA-B*15:01:01:01"));
		assertEquals("HLA-B*07:02", loader.hasFrequency(Locus.HLA_B, "HLA-B*07:02:01"));
	}

	@Test
	public void testOriginalValueIsMarshalledAsXmlAttribute() throws Exception {
		Haplotype haplotype = firstHaplotype(HET_C + "^HLA-B*08:01:01:01/HLA-B*15:01:01:01+HLA-B*07:02:01");

		java.io.StringWriter out = new java.io.StringWriter();
		jakarta.xml.bind.JAXBContext.newInstance(org.dash.valid.gl.haplo.MultiLocusHaplotype.class)
				.createMarshaller().marshal(new jakarta.xml.bind.JAXBElement<org.dash.valid.gl.haplo.MultiLocusHaplotype>(
						new javax.xml.namespace.QName("haplotype"), org.dash.valid.gl.haplo.MultiLocusHaplotype.class,
						(org.dash.valid.gl.haplo.MultiLocusHaplotype) haplotype), out);

		assertTrue(out.toString().contains("originalValue="), out.toString());
	}

	// hasFrequency is indexed; it must still return exactly what the front-to-back scan with
	// fieldLevelComparison / checkAntigenRecognitionSite did (first matching entry in list order).
	@Test
	public void testIndexedHasFrequencyMatchesLinearScan() throws Exception {
		String std = "src/main/resources/frequencies/std/";
		HLAFrequenciesLoader loader = HLAFrequenciesLoader.getInstance(new java.util.LinkedHashSet<File>(Arrays.asList(
				new File(std + "NMDP_2007_ACB_Freqs.csv"), new File(std + "NMDP_2007_DRB1DQB1_Freqs.csv"),
				new File(std + "NMDP_2007_FiveLocus_Freqs.csv"))), (File) null);

		java.util.List<String> probes = new java.util.ArrayList<String>();
		for (String line : Files.readAllLines(Path.of("src/test/resources/syntheticExamples.txt"))) {
			for (String alleleList : line.split("[\\^+~|]")) {
				for (String allele : alleleList.split("/")) {
					probes.add(allele.startsWith("HLA-") ? allele : alleleList.substring(0, alleleList.indexOf('*') + 1) + allele);
				}
			}
		}
		probes.add("HLA-B*52");
		probes.add("HLA-C*07:01:01:01");
		probes.add("HLA-A*99:99");

		int matched = 0;
		for (Locus locus : Arrays.asList(Locus.HLA_A, Locus.HLA_B, Locus.HLA_C, Locus.HLA_DRB1, Locus.HLA_DQB1)) {
			java.util.List<String> list = loader.getIndividualLocusFrequencies().get(locus);
			for (String allele : probes) {
				String expected = null;
				for (String candidate : list) {
					if (org.dash.valid.gl.GLStringUtilities.fieldLevelComparison(allele, candidate)
							|| org.dash.valid.gl.GLStringUtilities.checkAntigenRecognitionSite(allele, candidate)) {
						expected = candidate;
						break;
					}
				}
				assertEquals(expected, loader.hasFrequency(locus, allele), locus + " " + allele);
				if (expected != null) matched++;
			}
		}
		assertTrue(matched > 100, "probe set should exercise real matches, got " + matched);
	}
}
