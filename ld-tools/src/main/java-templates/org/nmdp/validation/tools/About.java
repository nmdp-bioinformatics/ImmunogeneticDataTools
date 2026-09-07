/*

    ngs-tools  Next generation sequencing (NGS/HTS) command line tools.
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
package org.nmdp.validation.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintStream;

/**
 * About.
 */
final class About {
    private static final String ARTIFACT_ID = "${project.artifactId}";
    // See ld-tools/pom.xml's build.timestamp property for why this isn't the more obvious
    // "${maven.build.timestamp}" -- that placeholder is left unresolved by
    // templating-maven-plugin's filtering step even though Maven's own POM parser resolves it
    // fine inside pom.xml itself.
    private static final String BUILD_TIMESTAMP = "${build.timestamp}";
    // git-commit-id-maven-plugin's default commitIdGenerationMode ("flat") exports the
    // abbreviated commit id as git.commit.id.abbrev -- there is no bare git.commit.id property
    // in that mode (it's git.commit.id.full instead). Before the plugin was wired up
    // (see ld-tools/pom.xml), this was "${git.commit.id}" and every build left it as a literal,
    // unresolved placeholder: nothing in the build had ever defined that Maven property.
    private static final String COMMIT = "${git.commit.id.abbrev}";
    private static final String COPYRIGHT = "Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)";
    private static final String LICENSE = "Licensed GNU Lesser General Public License (LGPL), version 3 or later";
    private static final String VERSION = "${project.version}";


    /**
     * Return the artifact id.
     *
     * @return the artifact id
     */
    public String artifactId() {
        return ARTIFACT_ID;
    }

    /**
     * Return the build timestamp.
     *
     * @return the build timestamp
     */
    public String buildTimestamp() {
        return BUILD_TIMESTAMP;
    }

    /**
     * Return the last commit.
     *
     * @return the last commit
     */
    public String commit() {
        return COMMIT;
    }

    /**
     * Return the copyright.
     *
     * @return the copyright
     */
    public String copyright() {
        return COPYRIGHT;
    }

    /**
     * Return the license.
     *
     * @return the license
     */
    public String license() {
        return LICENSE;
    }

    /**
     * Return the version.
     *
     * @return the version
     */
    public String version() {
        return VERSION;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(artifactId());
        sb.append(" ");
        sb.append(version());
        sb.append("\n");
        sb.append("Commit: ");
        sb.append(commit());
        sb.append("  Build: ");
        sb.append(buildTimestamp());
        sb.append("\n");
        sb.append(copyright());
        sb.append("\n");
        sb.append(license());
        sb.append("\n");
        return sb.toString();
    }


    /**
     * Write about text to the specified print stream.
     *
     * @param out print stream to write about text to
     */
    public static void about(final PrintStream out) {
        checkNotNull(out);
        out.print(new About().toString());
    }

    /**
     * Return a one-line "&lt;artifactId&gt; &lt;version&gt;" identifier, e.g. {@code ld-tools 1.0.0}.
     * Used to show the software version alongside {@code --help} output, without repeating the
     * full about block's commit/build/copyright/license lines there too (see
     * https://github.com/nmdp-bioinformatics/ImmunogeneticDataTools/issues/89).
     *
     * @return the artifact id and version, space-separated
     */
    public static String header() {
        About about = new About();
        return about.artifactId() + " " + about.version();
    }
}
