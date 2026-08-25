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

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Locus;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;
import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.StringArgument;

/**
 * CLI entry point for {@code normalize-frequency-file} — converts an NMDP haplotype
 * frequency reference file into this project's own standard comma-delimited format, so it
 * can be passed to {@code analyze-gl-strings} via {@code -q}. Handles both the legacy
 * per-locus-column layout and the newer combined-haplotype layout (auto-detected from the
 * file's own header row); multi-locus column order is derived from the input file's own
 * {@code "~"}-joined name (e.g. {@code A~C~B.xlsx}).
 */
public class NormalizeFrequencyFile implements Callable<Integer> {

    private final File inputFile;
    private final String frequencies;
    private final File outputFile;

    public static final String SINGLE = "single";

    private static final String USAGE = "normalize-frequency-file [args]";


    /**
     * Builds the conversion job for one input file.
     *
     * @param inputFile input frequency reference file
     * @param frequencies {@link #SINGLE} for an individual-locus frequency file; any other
     *                    value (or {@code null}) for a multi-locus haplotype file
     * @param outputFile output file, in this project's standard format
     */
    public NormalizeFrequencyFile(File inputFile, String frequencies, File outputFile) {
        this.inputFile = inputFile;
        this.frequencies = frequencies;
        this.outputFile   = outputFile;
    }
    
    @Override
    public Integer call() throws Exception {
    	// POI's default 100MB cap on a single in-memory array allocation (a zip-bomb guard) is
    	// too small for some of the larger reference files this tool is meant to read (the
    	// nine-locus NMDP release's biggest files run well past that once decompressed); this
    	// tool only ever reads a file the caller names directly on the local filesystem, never
    	// an untrusted upload, so raising the cap here is safe.
    	org.apache.poi.util.IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

    	PrintWriter writer = new PrintWriter(outputFile);
    	
    	if (SINGLE.equals(frequencies)) {
    		List<String> singleLocusFrequencies = HLAFrequenciesLoader.loadIndividualLocusFrequency(new FileInputStream(inputFile));
    		
    		for (String allele : singleLocusFrequencies) {
    			writer.write(allele + GLStringConstants.NEWLINE);
    		}
    	}
    	else {
			// Was: a hardcoded Map<EnumSet<Locus>, Locus[]> keyed by a pre-registered Linkages
			// enum constant, so adding support for a new locus combination meant adding new
			// Linkages/Locus.*_LOCI constants first. This project's own bundled reference files
			// (frequencies/nmdp/A~C~B.xlsx, .../DRB3-4-5~DRB1~DQB1.xlsx, etc.) already name
			// themselves with their exact column order, "~"-joined -- Locus.lookup() already
			// resolves every token that convention uses (including "DRBX" and "DRB3-4-5" as
			// DRB345 aliases), so deriving column order directly from the input file's own name
			// handles any locus combination a reference file happens to be named for, without
			// needing new enum constants registered ahead of time for each one.
			Locus[] locusPositions = deriveLocusPositions(inputFile);

			List<DisequilibriumElement> disequilibriumElements = HLAFrequenciesLoader.loadNMDPLinkageReferenceData(new FileInputStream(inputFile), locusPositions);

			for (DisequilibriumElement element : disequilibriumElements) {
				StringBuffer sb = new StringBuffer();
				int locusCounter = 0;
				// Was: Locus.lookup(element.getLoci()) -- looks up the exact locus SET against
				// the same small set of pre-registered combos, returning null (NPE on iteration)
				// for anything else. locusPositions is already the correct, file-derived order
				// for these exact elements (it's what they were just loaded with), so use it
				// directly instead of re-deriving order through that same limited lookup.
				for (Locus locus : locusPositions) {
					if (locusCounter > 0) {
						sb.append(GLStringConstants.GENE_PHASE_DELIMITER);
					}
					sb.append(element.getHlaElement(locus).get(0));
					locusCounter++;
				}
				
				List<FrequencyByRace> frequencies = ((DisequilibriumElementByRace) element).getFrequenciesByRace();
				for (FrequencyByRace frequency : frequencies) {
					writer.write(frequency.getRace() + GLStringConstants.COMMA + sb + GLStringConstants.COMMA + frequency.getFrequency() + GLStringConstants.COMMA + frequency.getRank() + GLStringConstants.NEWLINE);
				}
			}
		
    	}
    	
    	writer.close();

    	return 0;
	}

	// Derives the per-column Locus order from the input file's own name: "A~C~B.xlsx" ->
	// [HLA_A, HLA_C, HLA_B]. Matches the naming convention this project's own bundled
	// frequencies/nmdp/*.xlsx files already use (including "DRB3-4-5", which Locus.lookup()
	// already resolves via its freqName field), so newer NMDP reference file releases that
	// follow the same convention (e.g. the 2026 nine-locus release's "DRBX~DRB1~DQA1~DQB1.xlsx")
	// work without any code change here, as long as every "~"-joined token is a locus
	// Locus.lookup() recognizes.
	private static Locus[] deriveLocusPositions(File file) {
		String baseName = file.getName();
		int dot = baseName.lastIndexOf('.');
		String stem = dot > 0 ? baseName.substring(0, dot) : baseName;
		String[] tokens = stem.split(GLStringConstants.GENE_PHASE_DELIMITER);

		Locus[] positions = new Locus[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			Locus locus = Locus.lookup(tokens[i]);
			if (locus == null) {
				throw new IllegalArgumentException("Could not recognize locus token \"" + tokens[i]
						+ "\" in file name \"" + file.getName() + "\" -- expected \"~\"-joined locus names "
						+ "matching the order of columns in the file (e.g. \"A~C~B.xlsx\"), the same "
						+ "convention this project's own bundled frequencies/nmdp/*.xlsx files use.");
			}
			positions[i] = locus;
		}
		return positions;
	}

    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help  = new Switch("h", "help", "display help message");
        FileArgument inputFile = new FileArgument("i", "input-file", "input file, default stdin", false);
        StringArgument frequencies = new StringArgument("f", "frequencies",
                "\"single\" for an individual-locus frequency file; any other value (or omitted) for a "
                        + "multi-locus haplotype file, whose column order is derived from the input file's own "
                        + "\"~\"-joined name (e.g. A~C~B.xlsx), not from this argument",
                false);
        FileArgument outputFile   = new FileArgument("o", "output-file", "output allele assignment file, default stdout", false);

        ArgumentList arguments  = new ArgumentList(about, help, inputFile, frequencies, outputFile);
        CommandLine commandLine = new CommandLine(args);

        NormalizeFrequencyFile normalizeFrequencyFile = null;
        try
        {
            CommandLineParser.parse(commandLine, arguments);
            if (about.wasFound()) {
                About.about(System.out);
                System.exit(0);
            }
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(0);
            }
            normalizeFrequencyFile = new NormalizeFrequencyFile(inputFile.getValue(), frequencies.getValue(), outputFile.getValue());
        }
        catch (CommandLineParseException | IllegalArgumentException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        try {
            System.exit(normalizeFrequencyFile.call());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}
