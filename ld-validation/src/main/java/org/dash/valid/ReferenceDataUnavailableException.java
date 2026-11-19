package org.dash.valid;

/**
 * Thrown when the reference data an analysis depends on (for example the IMGT/HLA G-group or
 * allele files for a requested {@code hladb} version) cannot be loaded. Deliberately unchecked:
 * several callers up the stack catch {@code IOException} and carry on with empty data, which
 * would turn "this version doesn't exist" into a quietly wrong result. This must reach the user.
 */
public class ReferenceDataUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReferenceDataUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Builds the user-facing message for a failed IMGT/HLA download.
	 *
	 * @param hladb the version as requested (e.g. "3.2.0" or "Latest")
	 * @param what a short description of the data, e.g. "antigen recognition site (G-group)"
	 * @param cause what went wrong while loading it
	 */
	public static ReferenceDataUnavailableException forHladb(String hladb, String what, Throwable cause) {
		String message;

		if (cause instanceof java.io.FileNotFoundException) {
			message = "No " + what + " data is available for IMGT/HLA release '" + hladb + "' (the file was not found, "
					+ "so the release number may be mistyped, or it predates that file). "
					+ "Release numbers are listed at https://hml.nmdp.org/mac/api/imgtHlaReleases. "
					+ "Leave the version blank to use the latest release.";
		}
		else if (cause instanceof java.io.IOException) {
			message = "Could not download " + what + " data for IMGT/HLA release '" + hladb + "': " + cause
					+ ". Check the internet connection and try again.";
		}
		else {
			message = "Could not read " + what + " data for IMGT/HLA release '" + hladb + "': " + cause;
		}

		return new ReferenceDataUnavailableException(message, cause);
	}
}
