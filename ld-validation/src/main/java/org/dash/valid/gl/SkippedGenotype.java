package org.dash.valid.gl;

/**
 * A genotype that was read from the input but not analysed, with the reason (e.g. the
 * allele or protein ambiguity threshold it exceeded), so it can be reported in the output.
 */
public class SkippedGenotype {
	private final String id;
	private final Integer line;
	private final String reason;

	/**
	 * @param id the genotype's id
	 * @param line 1-based input line, or null when the input has no lines (XML)
	 * @param reason why it was skipped
	 */
	public SkippedGenotype(String id, Integer line, String reason) {
		this.id = id;
		this.line = line;
		this.reason = reason;
	}

	public String getId() { return id; }
	public Integer getLine() { return line; }
	public String getReason() { return reason; }
}
