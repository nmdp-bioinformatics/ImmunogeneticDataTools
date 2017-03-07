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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.dash.valid.DisequilibriumElement;
import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
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
 * AnalyzeGLStrings
 *
 */
public class NormalizeFrequencyFile implements Callable<Integer> {
	
    private final File inputFile;
    private final String linkages;
    private final File outputFile;
    
    private static Map<EnumSet<Locus>, Locus[]> LOCUS_POSITION_MAP = new HashMap<EnumSet<Locus>, Locus[]>();
    
    static {
    	LOCUS_POSITION_MAP.put(Locus.A_C_B_LOCI, HLAFrequenciesLoader.NMDP_ABC_LOCI_POS);
    	LOCUS_POSITION_MAP.put(Locus.C_B_LOCI, HLAFrequenciesLoader.NMDP_BC_LOCI_POS);
    	LOCUS_POSITION_MAP.put(Locus.DRB_DQB_LOCI, HLAFrequenciesLoader.NMDP_DRDQB1_LOCI_POS);
    	LOCUS_POSITION_MAP.put(Locus.FIVE_LOCUS, HLAFrequenciesLoader.NMDP_FIVE_LOCUS_POS);
    	LOCUS_POSITION_MAP.put(Locus.SIX_LOCUS, HLAFrequenciesLoader.NMDP_SIX_LOCUS_POS);
    }

    private static final String USAGE = "normalize-frequency-file [args]";


    /**
     * Normalize frequency files into a standardized format
     *
     * @param inputFile input file, if any
     * @param outputFile output interpretation file, if any
     */
    public NormalizeFrequencyFile(File inputFile, String linkages, File outputFile) {
        this.inputFile = inputFile;
        this.linkages = linkages;
        this.outputFile   = outputFile;
    }
    
    @Override
    public Integer call() throws Exception {
		HashSet<String> linkageNames = new HashSet<String>();
		linkageNames.add(linkages);
		Set<Linkages> linkagesSet = Linkages.lookup(linkageNames);
		LinkagesLoader.getInstance(linkagesSet);
								
		List<DisequilibriumElement> disequilibriumElements = HLAFrequenciesLoader.loadNMDPLinkageReferenceData(new FileInputStream(inputFile), LOCUS_POSITION_MAP.get(Linkages.lookup(linkages).getLoci()));
		
		PrintWriter writer = new PrintWriter(outputFile);
		
		for (DisequilibriumElement element : disequilibriumElements) {
			StringBuffer sb = new StringBuffer();
			int locusCounter = 0;
			for (Locus locus : Locus.lookup(element.getLoci())) {
				if (locusCounter > 0) {
					sb.append(GLStringConstants.GENE_PHASE_DELIMITER);
				}
				sb.append(element.getHlaElement(locus));
				locusCounter++;
			}
			
			List<FrequencyByRace> frequencies = ((DisequilibriumElementByRace) element).getFrequenciesByRace();
			for (FrequencyByRace frequency : frequencies) {
				writer.write(frequency.getRace() + GLStringConstants.COMMA + sb + GLStringConstants.COMMA + frequency.getFrequency() + GLStringConstants.COMMA + frequency.getRank() + GLStringConstants.NEWLINE);
			}
		}
		
		writer.close();
    	
    	return 0;
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
        StringArgument linkages = new StringArgument("l", "linakges", "linkages (acb, cb, drb_dqb, five_loc, six_loc), default five_loc", false);
        FileArgument outputFile   = new FileArgument("o", "output-file", "output allele assignment file, default stdout", false);

        ArgumentList arguments  = new ArgumentList(about, help, inputFile, linkages, outputFile);
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
            normalizeFrequencyFile = new NormalizeFrequencyFile(inputFile.getValue(), linkages.getValue(), outputFile.getValue());
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
