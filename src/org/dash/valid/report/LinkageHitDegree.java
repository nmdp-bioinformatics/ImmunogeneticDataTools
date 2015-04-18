package org.dash.valid.report;


public class LinkageHitDegree {	
	private int hitBlock;
	private int alleleBlocks;
	private String allele;
	
	public int getHitBlock() {
		return hitBlock;
	}
	
	public String getAllele() {
		return allele;
	}

	public int getAlleleBlocks() {
		return alleleBlocks;
	}
	
	public LinkageHitDegree(int hitBlock, int alleleBlocks, String allele) {
		this.hitBlock = hitBlock;
		this.alleleBlocks = alleleBlocks;
		this.allele = allele;
	}
	
	@Override
	public String toString() {
		return hitBlock + " of " + getAlleleBlocks() + " field match [" + getAllele() + "]";
	}
}
