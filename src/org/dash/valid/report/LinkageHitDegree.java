package org.dash.valid.report;

import org.dash.valid.gl.GLStringUtilities;

public class LinkageHitDegree {	
	private int hitBlock;
	private String allele;
	
	public int getHitBlock() {
		return hitBlock;
	}
	
	public String getAllele() {
		return allele;
	}

	public int getAlleleBlocks() {
		return allele.split(GLStringUtilities.COLON).length;
	}
	
	public LinkageHitDegree(int hitBlock, String allele) {
		this.hitBlock = hitBlock;
		this.allele = allele;
	}
	
	@Override
	public String toString() {
		return hitBlock + " of " + getAlleleBlocks() + " field match [" + getAllele() + "]";
	}
}
