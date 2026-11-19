package org.dash.valid.ars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dash.valid.ReferenceDataUnavailableException;
import org.dash.valid.gl.GLStringConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

// A requested IMGT/HLA release with no reference file used to be answered silently: the ARS
// loader fell back to the bundled mmc1.xls (and, since it reset its own bookkeeping, re-tried the
// download on every lookup), so the result claimed the requested release. It must now fail, with
// a message naming the release.
//
// IMGT/HLA 3.2.0 is real but predates hla_ambigs.xml.zip, so the download 404s.
// If the network is down the exception is still the same type, and the message still names the
// release, so this test holds either way.
public class AntigenRecognitionSiteLoaderUnavailableVersionTest {

	private String originalHladb;
	private String originalArs;

	@AfterEach
	public void restoreProperties() {
		restore(GLStringConstants.HLADB_PROPERTY, originalHladb);
		restore(GLStringConstants.ARS_PROPERTY, originalArs);
	}

	private static void restore(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		}
		else {
			System.setProperty(key, value);
		}
	}

	@Test
	public void unavailableReleaseFailsLoudlyForArs() {
		originalHladb = System.getProperty(GLStringConstants.HLADB_PROPERTY);
		originalArs = System.getProperty(GLStringConstants.ARS_PROPERTY);
		System.clearProperty(GLStringConstants.ARS_PROPERTY);
		System.setProperty(GLStringConstants.HLADB_PROPERTY, "3.2.0");

		ReferenceDataUnavailableException e = assertThrows(ReferenceDataUnavailableException.class,
				AntigenRecognitionSiteLoader::getInstance);
		assertTrue(e.getMessage().contains("3.2.0"), e.getMessage());
	}

	@Test
	public void failureIsNotCachedAsSuccess() {
		originalHladb = System.getProperty(GLStringConstants.HLADB_PROPERTY);
		originalArs = System.getProperty(GLStringConstants.ARS_PROPERTY);
		System.clearProperty(GLStringConstants.ARS_PROPERTY);
		System.setProperty(GLStringConstants.HLADB_PROPERTY, "3.2.0");

		assertThrows(ReferenceDataUnavailableException.class, AntigenRecognitionSiteLoader::getInstance);
		// A second call must fail the same way, not return a leftover instance.
		assertThrows(ReferenceDataUnavailableException.class, AntigenRecognitionSiteLoader::getInstance);
	}

	@Test
	public void bundledArsStillAvailableOnPurposeViaExplicitSetting() throws Exception {
		originalHladb = System.getProperty(GLStringConstants.HLADB_PROPERTY);
		originalArs = System.getProperty(GLStringConstants.ARS_PROPERTY);
		System.setProperty(GLStringConstants.HLADB_PROPERTY, "3.2.0");
		System.setProperty(GLStringConstants.ARS_PROPERTY, GLStringConstants.ARS_DEFAULT);

		assertEquals(true, AntigenRecognitionSiteLoader.getInstance().getArsMap().size() > 0);
	}
}
