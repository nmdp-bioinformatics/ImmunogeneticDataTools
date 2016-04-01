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
import java.util.List;
import java.util.concurrent.Callable;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Locus;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.report.DetectedLinkageFindings;
import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;
import org.dishevelled.commandline.argument.FileArgument;
import org.nmdp.gl.MultilocusUnphasedGenotype;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;

/**
 * ValidateLdGlstrings
 *
 */
public class ValidateLdGlstrings implements Callable<Integer> {
	
    private final File inputHmlFile;
    private final File outputFile;
    private static final String USAGE = "validate-gl-ld [args]";


    /**
     * Extract expected allele assignments in haploid elements from a file in HML format.
     *
     * @param inputHmlFile input HML file, if any
     * @param outputFile output interpretation file, if any
     */
    public ValidateLdGlstrings(final File inputHmlFile, final File outputFile) {
        this.inputHmlFile = inputHmlFile;
        this.outputFile   = outputFile;
    }
    
    @Override
    public Integer call() throws Exception {
        BufferedReader reader = null;
        PrintWriter writer    = null;
        try {
            ListMultimap<String, SubjectMug> validationResults = read(inputHmlFile);
            writer = writer(outputFile);
            for (String sample : validationResults.keySet()) {
            	for (SubjectMug subjectResults : validationResults.get(sample)) {
            		writer.println(sample + "\t" + subjectResults.minimumDifference());
            	}
            }
            return 0;
        }
        finally {
            try {
                reader.close();
            }
            catch (Exception e) {
                // ignore
            }
            try {
                writer.close();
            }
            catch (Exception e) {
                // ignore
            }
        }
    }

   static ListMultimap<String, SubjectMug> read(final File file) throws IOException {
       BufferedReader reader = null;
       final ListMultimap<String, SubjectMug> subjectmugs = ArrayListMultimap.create();
       final SubjectMug.Builder builder = SubjectMug.builder();
       try {
           reader = reader(file);
           CharStreams.readLines(reader, new LineProcessor<Void>() {
                   private int count = 0;

                   @Override
                   public boolean processLine(final String line) throws IOException {
                       String[] tokens = line.split("\t");
                       if (tokens.length < 2) {
                           throw new IOException("illegal format, expected at least 2 columns, found " + tokens.length + "\nline=" + line);
                       }
                      
                      String fullyQualified            = GLStringUtilities.fullyQualifyGLString(tokens[1]);
         			  MultilocusUnphasedGenotype mug   = GLStringUtilities.convertToMug(fullyQualified);
        			  DetectedLinkageFindings findings = LinkageDisequilibriumAnalyzer.detectLinkages(mug);
        			  Float minimumDifference          = findings.getMinimumDifference(Locus.FIVE_LOCUS);
        			  minimumDifference                = minimumDifference == null ? 0 : minimumDifference;
        			  SubjectMug subjectMug = builder.reset()
                           .withSample(tokens[0])
                           .withGlstring(tokens[1])
                           .withMug(mug)
                           .withFindings(findings)
                           .withMinimumDifference(minimumDifference)
                           .build();

                       subjectmugs.put(subjectMug.sample(), subjectMug);
                       count++;
                       return true;
                   }

                   @Override
                   public Void getResult() {
                       return null;
                   }
               });

           return subjectmugs;
       }
       finally {
           try {
               reader.close();
           }
           catch (Exception e) {
               // ignore
           }
       }
   }
    
    static final class SubjectMug {
        private final String sample;
        private final MultilocusUnphasedGenotype mug;
        private final DetectedLinkageFindings findings;
        private final Float minimumDifference;
        private final String glstring;

        private SubjectMug(final String sample,
                               final MultilocusUnphasedGenotype mug,
                               final DetectedLinkageFindings findings,
                               final String glstring,
                               final Float minimumDifference) {
            this.sample    = sample;
            this.mug       = mug;
            this.findings  = findings;
            this.glstring  = glstring;
            this.minimumDifference = minimumDifference;
        }

        String sample() {
            return sample;
        }

        MultilocusUnphasedGenotype mug() {
            return mug;
        }


        DetectedLinkageFindings findings() {
            return findings;
        }

        String glstring() {
            return glstring;
        }

        Float minimumDifference() {
            return minimumDifference;
        }

        static Builder builder() {
            return new Builder();
        }

        static final class Builder {

            private String sample;
            private MultilocusUnphasedGenotype mug;
            private DetectedLinkageFindings findings;
            private Float minimumDifference;
            private String glstring;
            
            private Builder() {
                // empty
            }

            Builder withSample(final String sample) {
                this.sample = sample;
                return this;
            }

            Builder withFindings(final DetectedLinkageFindings findings) {
                this.findings = findings;
                return this;
            }

            Builder withMug(final MultilocusUnphasedGenotype mug) {
                this.mug = mug;
                return this;
            }

            Builder withMinimumDifference(final Float minimumDifference) {
                this.minimumDifference = minimumDifference;
                return this;
            }

            Builder withGlstring(final String glstring) {
                this.glstring = glstring;
                return this;
            }

            Builder reset() {
                sample   = null;
                mug      = null;
                findings = null;
                minimumDifference = null;
                glstring  = null;
                return this;
            }

            SubjectMug build() {
                return new SubjectMug(sample, mug, findings, glstring, minimumDifference);
            }
        }
    }
    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help  = new Switch("h", "help", "display help message");
        FileArgument inputHmlFile = new FileArgument("i", "input-file", "input file, default stdin", false);
        FileArgument outputFile   = new FileArgument("o", "output-file", "output allele assignment file, default stdout", false);

        ArgumentList arguments  = new ArgumentList(about, help, inputHmlFile, outputFile);
        CommandLine commandLine = new CommandLine(args);

        ValidateLdGlstrings validateLdGlstrings = null;
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
            validateLdGlstrings = new ValidateLdGlstrings(inputHmlFile.getValue(), outputFile.getValue());
        }
        catch (CommandLineParseException | IllegalArgumentException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        try {
            System.exit(validateLdGlstrings.call());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}
