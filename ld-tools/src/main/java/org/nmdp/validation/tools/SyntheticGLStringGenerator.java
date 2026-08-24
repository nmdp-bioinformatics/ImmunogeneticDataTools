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
package org.nmdp.validation.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.ars.AntigenRecognitionSiteLoader;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;
import org.dishevelled.commandline.argument.DoubleArgument;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.IntegerArgument;
import org.dishevelled.commandline.argument.LongArgument;

/**
 * Generates a synthetic GL-string test file, modeled on the shape of real-world NGS HLA
 * typing output (stanfordExamplesFixed.txt, used throughout the 2026-08 modernization/
 * non-determinism investigation), without reading or reproducing any real individual's
 * genotype. Every allele combination here is drawn directly from an NMDP haplotype
 * frequency reference file supplied by the caller -- a population-level statistical table,
 * not any person's actual typing result -- so nothing generated here can be traced back
 * to an individual. See resolveHaplotype()/expressAllele() below for exactly how the
 * three real-data-driven synthetic scenarios (plain ambiguity, an oversized ambiguity
 * that must be rejected, and a genotype-ambiguity G-group tie) are constructed.
 *
 * The frequency reference file itself is NOT read from anywhere bundled in this repo, by
 * design -- like the "2011 NMDP frequency files" noted elsewhere in this project as
 * license-gated and requiring manual install, the fuller reference tables this generator
 * is meant to be pointed at are not freely redistributable, so this tool takes a path to
 * a locally-provided file rather than shipping one. The generated *output* -- structurally
 * synthetic combinations, with real frequency values discarded entirely, never reproducing
 * the source table's rows or its statistical values -- carries none of that restriction.
 */
public class SyntheticGLStringGenerator implements Callable<Integer> {

    private static final String USAGE = "synthetic-gl-string-generator [args]";

    // Deliberately just these 5 loci: they're exactly Locus.FIVE_LOCUS, the widest locus
    // set any of this codebase's Linkages actually check (DP loci are never referenced by
    // any Linkages set at all; DRB345 is real-file-realistic but adds handling complexity
    // for marginal extra coverage of code paths the other loci already exercise).
    private static final Locus[] LOCI = { Locus.HLA_A, Locus.HLA_C, Locus.HLA_B, Locus.HLA_DRB1, Locus.HLA_DQB1 };

    private final File frequencyFile;
    private final File outputFile;
    private final int sampleCount;
    private final long seed;
    private final double ambiguityRate;
    private final double thresholdExceedingRate;
    private final double genotypeTieRate;

    public SyntheticGLStringGenerator(File frequencyFile, File outputFile, int sampleCount, long seed,
            double ambiguityRate, double thresholdExceedingRate, double genotypeTieRate) {
        this.frequencyFile = frequencyFile;
        this.outputFile = outputFile;
        this.sampleCount = sampleCount;
        this.seed = seed;
        this.ambiguityRate = ambiguityRate;
        this.thresholdExceedingRate = thresholdExceedingRate;
        this.genotypeTieRate = genotypeTieRate;
    }

    @Override
    public Integer call() throws Exception {
        List<DisequilibriumElement> referenceHaplotypes = loadReferenceHaplotypes();
        if (referenceHaplotypes.isEmpty()) {
            throw new IllegalStateException("No haplotype rows loaded from " + frequencyFile);
        }

        // G-group membership data, for expressing a G-group allele as a plausible-looking
        // ambiguity across its own real member alleles. Same public IMGT/HLA source
        // AntigenRecognitionSiteLoader already downloads for the main analysis path.
        AntigenRecognitionSiteLoader arsLoader = AntigenRecognitionSiteLoader.getInstance();

        Random random = new Random(seed);

        try (PrintWriter writer = new PrintWriter(outputFile)) {
            for (int sampleIndex = 1; sampleIndex <= sampleCount; sampleIndex++) {
                String id = "synthetic-" + String.format("%03d", sampleIndex);
                String glString = generateSample(referenceHaplotypes, arsLoader, random);
                writer.println(id + GLStringConstants.TAB + glString);
            }
        }

        return 0;
    }

    private List<DisequilibriumElement> loadReferenceHaplotypes() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(frequencyFile)))) {
            return HLAFrequenciesLoader.loadStandardReferenceData(reader);
        }
    }

    private String generateSample(List<DisequilibriumElement> referenceHaplotypes,
            AntigenRecognitionSiteLoader arsLoader, Random random) {
        DisequilibriumElement copy1 = referenceHaplotypes.get(random.nextInt(referenceHaplotypes.size()));
        DisequilibriumElement copy2 = referenceHaplotypes.get(random.nextInt(referenceHaplotypes.size()));

        // At most one locus per sample gets a deliberate scenario injected (an oversized
        // ambiguity to exercise AmbiguousGenotypeException, or a "|" genotype-ambiguity
        // G-group tie to exercise the tie-break path) -- real samples aren't uniformly
        // pathological at every locus, and layering both at the same locus would just
        // make the threshold rejection swallow the tie scenario before it's ever reached.
        // Not every locus's alleles are G-group-suffixed (or G-group-suffixed with enough
        // real members loaded) for either scenario to actually be constructible there, so
        // rather than picking one random locus and silently falling back to plain
        // expression if it doesn't pan out (which would make the realized rate far lower
        // than the requested one), search all 5 loci in random order and use the first
        // that actually works. Only truly gives up if none of the 5 can support it.
        boolean wantThresholdBreach = random.nextDouble() < thresholdExceedingRate;
        boolean wantGenotypeTie = !wantThresholdBreach && random.nextDouble() < genotypeTieRate;

        Locus thresholdBreachLocus = null;
        Locus genotypeTieLocus = null;
        String genotypeTieExpression = null;

        if (wantThresholdBreach || wantGenotypeTie) {
            List<Locus> shuffledLoci = new ArrayList<Locus>(java.util.Arrays.asList(LOCI));
            java.util.Collections.shuffle(shuffledLoci, random);

            for (Locus candidate : shuffledLoci) {
                if (wantThresholdBreach) {
                    if (arsLoader.getArsMap().getOrDefault(copy1.getHlaElement(candidate).get(0), new HashSet<String>())
                            .size() > 21) {
                        thresholdBreachLocus = candidate;
                        break;
                    }
                }
                else {
                    String tie = expressGenotypeTie(copy1.getHlaElement(candidate).get(0),
                            copy2.getHlaElement(candidate).get(0), arsLoader, random);
                    if (tie != null) {
                        genotypeTieLocus = candidate;
                        genotypeTieExpression = tie;
                        break;
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LOCI.length; i++) {
            Locus locus = LOCI[i];
            if (i > 0) {
                sb.append(GLStringConstants.GENE_DELIMITER);
            }

            String allele1 = copy1.getHlaElement(locus).get(0);
            String allele2 = copy2.getHlaElement(locus).get(0);

            if (locus == thresholdBreachLocus) {
                sb.append(expressOversizedAmbiguity(allele1, arsLoader));
                sb.append(GLStringConstants.GENE_COPY_DELIMITER);
                sb.append(expressAllele(allele2, arsLoader, random));
            }
            else if (locus == genotypeTieLocus) {
                sb.append(genotypeTieExpression);
            }
            else {
                sb.append(expressAllele(allele1, arsLoader, random));
                sb.append(GLStringConstants.GENE_COPY_DELIMITER);
                sb.append(expressAllele(allele2, arsLoader, random));
            }
        }

        return sb.toString();
    }

    // Ordinary expression of one allele: most of the time, exactly as the reference file
    // has it. If it's a G-group allele, sometimes (ambiguityRate) express it instead as a
    // "/"-joined ambiguity across a random subset of its own real IMGT/HLA member alleles --
    // this is the same shape of ambiguity real shallow/low-resolution NGS typing produces,
    // and it's what checkAntigenRecognitionSite()'s multi-member matching loop exists for.
    private String expressAllele(String allele, AntigenRecognitionSiteLoader arsLoader, Random random) {
        if (!allele.endsWith("g") || random.nextDouble() >= ambiguityRate) {
            return allele;
        }

        List<String> members = sampleMembers(allele, arsLoader, random, 2, 4);
        return members == null ? allele : String.join(GLStringConstants.ALLELE_AMBIGUITY_DELIMITER, members);
    }

    // Exceeds ALLELE_AMBIGUITY_THRESHOLD (20) deliberately, so a real batch run of the
    // generated file exercises AmbiguousGenotypeException end to end, not just the
    // hand-crafted unit test fixture. Falls back to the plain allele if this particular
    // G-group doesn't have 21+ members to draw from (most don't).
    private String expressOversizedAmbiguity(String allele, AntigenRecognitionSiteLoader arsLoader) {
        if (!allele.endsWith("g")) {
            return allele;
        }
        HashSet<String> members = arsLoader.getArsMap().get(allele);
        if (members == null || members.size() <= 21) {
            return allele;
        }
        List<String> subset = new ArrayList<String>(members).subList(0, 21);
        return String.join(GLStringConstants.ALLELE_AMBIGUITY_DELIMITER, subset);
    }

    // Reproduces the exact mechanism behind the tie-breaking non-determinism investigated
    // and fixed in #122/#125 (sample -17/-141 from stanfordExamplesFixed.txt): a "|"
    // genotype ambiguity gives constructPossibleHaplotypes() two independent cartesian-
    // product candidates for the *same* pair of G-groups, expressed via different specific
    // member alleles -- "allele1a+allele2a|allele1b+allele2b" -- so they tie at the
    // G-group level while differing in fullValue. A plain "/" ambiguity within one copy
    // does NOT reproduce this: parseGLString() keeps a "/"-joined list together as one
    // candidate's allele set, never splitting it into separate cartesian-product entries --
    // only "|" (each alternative becomes its own List<String> position in the locus's
    // "copies" list) does that. Returns null if either allele isn't a G-group with at
    // least 2 members to pick two different ones from.
    private String expressGenotypeTie(String allele1, String allele2, AntigenRecognitionSiteLoader arsLoader,
            Random random) {
        List<String> members1 = sampleMembers(allele1, arsLoader, random, 2, 2);
        List<String> members2 = sampleMembers(allele2, arsLoader, random, 2, 2);
        if (members1 == null || members2 == null) {
            return null;
        }

        String interpretationA = members1.get(0) + GLStringConstants.GENE_COPY_DELIMITER + members2.get(0);
        String interpretationB = members1.get(1) + GLStringConstants.GENE_COPY_DELIMITER + members2.get(1);
        return interpretationA + GLStringConstants.GENOTYPE_AMBIGUITY_DELIMITER + interpretationB;
    }

    // Picks a random subset (minSize to maxSize, clamped to what's actually available) of
    // a G-group's real member alleles. Returns null if allele isn't G-group-suffixed or
    // doesn't have enough members loaded.
    private List<String> sampleMembers(String allele, AntigenRecognitionSiteLoader arsLoader, Random random,
            int minSize, int maxSize) {
        if (!allele.endsWith("g")) {
            return null;
        }
        HashSet<String> memberSet = arsLoader.getArsMap().get(allele);
        if (memberSet == null || memberSet.size() < minSize) {
            return null;
        }

        List<String> allMembers = new ArrayList<String>(memberSet);
        java.util.Collections.shuffle(allMembers, random);
        int size = Math.min(maxSize, allMembers.size());
        size = Math.max(size, minSize);
        return allMembers.subList(0, size);
    }

    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help = new Switch("h", "help", "display help message");
        FileArgument frequencyFile = new FileArgument("f", "frequency-file",
                "NMDP five-locus haplotype frequency reference file (race,haplotype,frequency,rank CSV, "
                        + "e.g. NMDP_FiveLocus_Freqs.csv) -- not bundled in this repo, provide your own path",
                true);
        FileArgument outputFile = new FileArgument("o", "output-file", "output GL string file", true);
        IntegerArgument sampleCount = new IntegerArgument("n", "sample-count", "number of samples to generate, default 40",
                false);
        LongArgument seed = new LongArgument("s", "seed", "random seed, for reproducible output, default 42", false);
        DoubleArgument ambiguityRate = new DoubleArgument("r", "ambiguity-rate",
                "fraction of G-group alleles to express as a member-allele ambiguity, default 0.35", false);
        DoubleArgument thresholdExceedingRate = new DoubleArgument("t", "threshold-exceeding-rate",
                "fraction of samples to deliberately push over the allele ambiguity threshold, default 0.06", false);
        DoubleArgument genotypeTieRate = new DoubleArgument("g", "genotype-tie-rate",
                "fraction of (non-threshold-exceeding) samples to inject a genotype-ambiguity G-group tie into, "
                        + "default 0.15", false);

        ArgumentList arguments = new ArgumentList(about, help, frequencyFile, outputFile, sampleCount, seed,
                ambiguityRate, thresholdExceedingRate, genotypeTieRate);
        CommandLine commandLine = new CommandLine(args);

        SyntheticGLStringGenerator generator = null;
        try {
            CommandLineParser.parse(commandLine, arguments);
            if (about.wasFound()) {
                About.about(System.out);
                System.exit(0);
            }
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(0);
            }
            generator = new SyntheticGLStringGenerator(
                    frequencyFile.getValue(),
                    outputFile.getValue(),
                    sampleCount.getValue() == null ? 40 : sampleCount.getValue(),
                    seed.getValue() == null ? 42L : seed.getValue(),
                    ambiguityRate.getValue() == null ? 0.35 : ambiguityRate.getValue(),
                    thresholdExceedingRate.getValue() == null ? 0.06 : thresholdExceedingRate.getValue(),
                    genotypeTieRate.getValue() == null ? 0.15 : genotypeTieRate.getValue());
        }
        catch (CommandLineParseException | IllegalArgumentException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        try {
            System.exit(generator.call());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
