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

import static org.dishevelled.compress.Readers.reader;
import static org.dishevelled.compress.Writers.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Linkages;
import org.dash.valid.LinkagesLoader;
import org.dash.valid.Locus;
import org.dash.valid.ars.HLADatabaseVersion;
import org.dash.valid.freq.Frequencies;
import org.dash.valid.freq.HLAFrequenciesLoader;
import org.dash.valid.gl.GLStringConstants;
import org.dash.valid.report.DetectedLinkageFindings;
import org.dash.valid.report.HaplotypePairWriter;
import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;
import org.dishevelled.commandline.argument.BooleanArgument;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.StringArgument;

/**
 * AnalyzeGLStrings
 *
 */
public class AnalyzeGLStrings implements Callable<Integer> {
	
    private final File inputFile;
    private final File outputFile;
    private final String hladb;
    private final String freq;
    private final Boolean warnings;
    private final File frequencyFile;
    private static final String USAGE = "analyze-gl-strings [args]";


    /**
     * Analyze gl string using linkage disequilibrium frequencies
     *
     * @param inputFile input file, if any
     * @param outputFile output interpretation file, if any
     */
    public AnalyzeGLStrings(File inputFile, File outputFile, String hladb, String freq, Boolean warnings, File frequencyFile) {
        this.inputFile = inputFile;
        this.outputFile   = outputFile;
        this.hladb = hladb;
        this.freq = freq;
        this.warnings = warnings;
        this.frequencyFile = frequencyFile;
    }
    
    @Override
    public Integer call() throws Exception {
    	BufferedReader reader = reader(inputFile);
    	
    	runAnalysis(reader);
    	return 0;
    }

	public void runAnalysis(BufferedReader reader) throws IOException {
		List<DetectedLinkageFindings> findingsList;
    	System.setProperty(HLADatabaseVersion.HLADB_PROPERTY, hladb != null ? hladb : GLStringConstants.EMPTY_STRING);
    	System.setProperty(Frequencies.FREQUENCIES_PROPERTY, freq != null ? freq : GLStringConstants.EMPTY_STRING);
    	
    	Set<Linkages> linkages = null;
    	
    	if (frequencyFile !=  null) {
    		HLAFrequenciesLoader hlaFreqLoader = HLAFrequenciesLoader.getInstance(frequencyFile);
    		Set<EnumSet<Locus>> lociSet = hlaFreqLoader.getLoci();
    		
    		System.out.println(lociSet);
    		// TODO:  Handle multiple linkages here
    		linkages = Linkages.lookup(lociSet.iterator().next());
    		    		
    		// TODO:  initialize this differently
    		LinkagesLoader.getInstance(linkages);
    	}
    	    	
    	findingsList = LinkageDisequilibriumAnalyzer.analyzeGLStringFile(inputFile == null ? "STDIN" : inputFile.getName(), reader);
    	
    	PrintWriter writer = null;
    	
//    	if (outputFile != null && outputFile.isDirectory()) {
//    		writer = writer(new File(outputFile.getPath() + "/summary.log"));
//    	}
//    	else {
//    		writer = writer(outputFile);
//    	}
    	
    	writer = writer(outputFile);
    	
    	for (DetectedLinkageFindings findings : findingsList) {
    		if (warnings != null && warnings == Boolean.TRUE && !findings.hasAnomalies()) {
    			continue;
    		}
    		//writer.write(SummaryWriter.getInstance().formatDetectedLinkages(findings));
    		writer.write(HaplotypePairWriter.getInstance().formatDetectedLinkages(findings));
    	}
    	
    	writer.close();
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
        FileArgument outputFile   = new FileArgument("o", "output-file", "output allele assignment file, default stdout", false);
        StringArgument hladb = new StringArgument("v", "hladb-version", "HLA DB version (e.g. 3.19.0), default latest", false);
        StringArgument freq = new StringArgument("f", "frequencies", "Frequency Set (e.g. nmdp, nmdp-2007, wiki), default nmdp-2007", false);
        BooleanArgument warnings = new BooleanArgument("w", "warnings-only", "Only log warnings, default all GL String output", false);
        FileArgument frequencyFile = new FileArgument("q", "frequency-file", "frequency input file, default nmdp-2007 five locus", false);

        ArgumentList arguments  = new ArgumentList(about, help, inputFile, outputFile, hladb, freq, warnings, frequencyFile);
        CommandLine commandLine = new CommandLine(args);

        AnalyzeGLStrings analyzeGLStrings = null;
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
            analyzeGLStrings = new AnalyzeGLStrings(inputFile.getValue(), outputFile.getValue(), hladb.getValue(), freq.getValue(), warnings.getValue(), frequencyFile.getValue());
        }
        catch (CommandLineParseException | IllegalArgumentException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        try {
            System.exit(analyzeGLStrings.call());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}
