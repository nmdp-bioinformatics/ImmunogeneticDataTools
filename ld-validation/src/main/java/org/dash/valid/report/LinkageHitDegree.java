package org.dash.valid.report;


public class LinkageHitDegree {	
	private int hitBlock;
	private int alleleBlocks;
	private String allele;
	private String matchedValue;
	
	public int getHitBlock() {
		return hitBlock;
	}
	
	public String getAllele() {
		return allele;
	}

	public int getAlleleBlocks() {
		return alleleBlocks;
	}
	
	public String getMatchedValue() {
		return matchedValue;
	}
	
	public LinkageHitDegree(int hitBlock, int alleleBlocks, String allele, String matchedValue) {
		this.hitBlock = hitBlock;
		this.alleleBlocks = alleleBlocks;
		this.allele = allele;
		this.matchedValue = matchedValue;
	}
	
	@Override
	public String toString() {
		return hitBlock + " of " + getAlleleBlocks() + " field match [" + getMatchedValue() + "]";
	}
}
