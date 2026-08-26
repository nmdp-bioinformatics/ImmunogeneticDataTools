/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.dash.valid.gl.haplo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dash.valid.Locus;
import org.dash.valid.report.DetectedDisequilibriumElement;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

/**
 * One haplotype — an ordered set of alleles across one or more loci believed to descend
 * together from a single ancestral chromosome. {@link MultiLocusHaplotype} is the concrete
 * implementation used throughout this project; see {@link HaplotypePair} for how two
 * haplotypes combine into a detected linkage.
 * <p>
 * {@code equals()}/{@code hashCode()} are based solely on {@link #getHaplotypeString()}
 * (the canonical, G-group-level representation) — deliberately, since a plain per-field
 * comparison here was a real source of run-to-run non-determinism before that was fixed
 * (see the inline comment on {@link #equals(Object)}).
 */
@XmlRootElement(name="haplotype")
@XmlType(propOrder={"sequence", "haplotypeString"})
public abstract class Haplotype {
	DetectedDisequilibriumElement linkage;
	private boolean drb345Homozygous;

	/**
	 * The reference-data match found for this haplotype, if any.
	 *
	 * @return the reference-data linkage this haplotype matched, or {@code null} if no
	 *         match was found (see {@link org.dash.valid.HLALinkageDisequilibrium})
	 */
	@XmlTransient
	public DetectedDisequilibriumElement getLinkage() {
		return linkage;
	}

	public void setLinkage(DetectedDisequilibriumElement linkage) {
		this.linkage = linkage;
	}
	
	public void setDRB345Homozygous(boolean drb345Homozygous) {
		this.drb345Homozygous = drb345Homozygous;
	}
	
	public boolean getDrb345Homozygous() {
		return this.drb345Homozygous;
	}
	
	@XmlAttribute(name="seq")
	public abstract Integer getSequence();
	public abstract void setSequence(Integer sequence);
	
	/**
	 * The canonical representation used for matching, equality, and hashing.
	 *
	 * @return the canonical, G-group-level representation of this haplotype (e.g.
	 *         {@code "HLA-C*07:01g~HLA-B*08:01g"}) — reported as {@code value} in output XML
	 */
	@XmlAttribute(name="value")
	public abstract String getHaplotypeString();

	/**
	 * The fully expanded representation, distinct from the canonical {@link #getHaplotypeString()}.
	 *
	 * @return the fully expanded, allele-level representation of this haplotype (e.g. every
	 *         specific allele the matched G-group(s) resolved to) — reported as
	 *         {@code fullValue} in output XML. Distinct from {@link #getHaplotypeString()}
	 *         since reference-data matching is G-group/ARS-based, not exact-allele-string.
	 */
	@XmlAttribute(name="fullValue")
	public abstract String getFullHaplotypeString();
	
	public abstract List<String> getAlleles();
	public abstract Map<Locus, List<String>> getAlleleMap();
	public abstract HashMap<Locus, Integer> getHaplotypeInstanceMap();
	public abstract List<String> getAlleles(Locus locus);
	public abstract void removeAlleles(Locus locus);
	public abstract Integer getHaplotypeInstance(Locus locus);
	
	public abstract Set<Locus> getLoci();
	
	public abstract List<Integer> getHaplotypeInstances();
	
	public String toString() {
		return getHaplotypeString();
	}
	
	// Was: required getHaplotypeInstances() to match (a List<Integer> built from
	// HashMap<Locus, Integer>.values() -- unordered, so List.equals() on it varies with
	// hash-bucket iteration order, itself not guaranteed stable across JVM runs since
	// Locus enum constants use identity hashCode); getAlleles().containsAll() (asymmetric
	// -- true if this's list is a superset of the other's, not mutual containment); and a
	// getLinkage() check that returned true whenever *this* object's linkage was null,
	// regardless of the other side's linkage (also asymmetric). All three meant
	// haplotype1.equals(haplotype2) could disagree with haplotype2.equals(haplotype1), and
	// results depended on HashSet iteration order -- which varies run to run. Confirmed via
	// running the same input twice and diffing output: identical code, different results.
	//
	// getHaplotypeString() is already the canonical, order-stable representation (built via
	// LocusSet/LocusComparator, not raw HashMap iteration) -- it alone is sufficient and
	// gives a properly symmetric, deterministic equals()/hashCode() pair.
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Haplotype)) {
			return false;
		}
		return getHaplotypeString().equals(((Haplotype) obj).getHaplotypeString());
	}

	@Override
	public int hashCode() {
		return getHaplotypeString().hashCode();
	}
}
