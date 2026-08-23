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
package org.dash.valid.gl;

/**
 * Thrown when a genotype's allele or protein ambiguity at a locus exceeds the
 * configured threshold (org.dash.ambThreshold / org.dash.proteinThreshold),
 * making full haplotype-pair enumeration (a cartesian product across every
 * ambiguous allele at every locus in a linkage set) computationally
 * infeasible. Unchecked: callers that process many genotypes (e.g. a batch
 * file) should catch this per-genotype and continue with the rest of the
 * batch, rather than letting one pathological entry abort the whole run.
 */
public class AmbiguousGenotypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AmbiguousGenotypeException(String message) {
		super(message);
	}
}
