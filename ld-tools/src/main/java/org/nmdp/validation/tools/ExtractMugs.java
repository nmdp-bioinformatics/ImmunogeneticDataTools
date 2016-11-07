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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.dishevelled.compress.Readers.reader;
import static org.dishevelled.compress.Writers.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;
import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.StringArgument;
import org.nmdp.gl.client.GlClient;
import org.nmdp.gl.client.local.LocalGlClient;
import org.nmdp.ngs.hml.HmlReader;
import org.nmdp.ngs.hml.jaxb.AlleleAssignment;
import org.nmdp.ngs.hml.jaxb.Haploid;
import org.nmdp.ngs.hml.jaxb.Hml;
import org.nmdp.ngs.hml.jaxb.Sample;
import org.nmdp.ngs.hml.jaxb.Typing;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

/**
 * Validate interpretation.
 */
public final class ExtractMugs implements Callable<Integer> {
    private final File inputHmlFile;
    private final File outputFile;
    private final String subjectId;
    private final GlClient glclient;
    static final int DEFAULT_RESOLUTION = 2;
    static final List<String> DEFAULT_LOCI = ImmutableList.of("HLA-A", "HLA-B", "HLA-C", "HLA-DRB1", "HLA-DQB1");
    private static final String USAGE = "ngs-validate-interpretation -e expected.txt -b observed.txt -r 2 -l \"HLA-A,HLA-B\"";


    /**
     * Validate interpretation.
     *
     * @param expectedFile expected file, at least one of expected or observed file must not be null
     * @param observedFile observed file, at least one of expected or observed file must not be null
     * @param outputFile output file, if any
     * @param resolution resolution, must be in the range [1..4]
     * @param loci list of loci to validate, must not be null
     * @param printSummary print summary report
     * @param glclient genotype list client, must not be null
     */
    public ExtractMugs(final File inputHmlFile,
                                  final File outputFile,
                                  final String subjectId,
                                  final GlClient glclient) {
        checkNotNull(glclient);
        this.inputHmlFile  = inputHmlFile;
        this.outputFile    = outputFile;
        this.subjectId     = subjectId;
        this.glclient      = glclient;
    }



    @Override
    public Integer call() throws Exception {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = reader(inputHmlFile);
            writer = writer(outputFile);

            Hml hml = HmlReader.read(reader);
            for (Sample sample : hml.getSample()) {
                String sampleId = sample.getId();
                List<String> genotypes = new ArrayList<String>();
                if(!subjectId.isEmpty() && subjectId.equals(sampleId)){
	                for (Typing typing : sample.getTyping()) {
	                    String geneFamily = typing.getGeneFamily();
	                    for (AlleleAssignment alleleAssignment : typing.getAlleleAssignment()) {
	                        String alleleDb = alleleAssignment.getAlleleDb();
	                        String alleleVersion = alleleAssignment.getAlleleVersion();
	
	                        ListMultimap<String, Haploid> haploidsByLocus = ArrayListMultimap.create();
	                        for (Object child : alleleAssignment.getPropertyAndHaploidAndGenotypeList()) {
	                            if (child instanceof Haploid) {
	                                Haploid haploid = (Haploid) child;
	                                haploidsByLocus.put(haploid.getLocus(), haploid);
	                            }
	                        }
	                        for (String locus : haploidsByLocus.keySet()) {
	                            List<Haploid> haploids = haploidsByLocus.get(locus);
	
                                String genotype = toGenotype(haploids.get(0), haploids.size() > 1 ? haploids.get(1) : null);
                                genotypes.add(genotype);                         
	                        }
	                    }
	                }
	                String glstring = Joiner.on("^").join(genotypes);
	                writer.println(sampleId + "\t" + glstring);
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
    
    static String toGenotype(final Haploid haploid0, final Haploid haploid1) {
        StringBuilder sb = new StringBuilder();
        sb.append(haploid0.getLocus());
        sb.append("*");
        sb.append(haploid0.getType());
        if (haploid1 != null) {
            sb.append("+");
            sb.append(haploid1.getLocus());
            sb.append("*");
            sb.append(haploid1.getType());
        }
        return sb.toString();
    }




    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help = new Switch("h", "help", "display help message");

        FileArgument Hmlfile      = new FileArgument("i", "hml-file", "hml file file, default stdin; at least one of expected or observed file must be provided", false);
        FileArgument outputFile   = new FileArgument("o", "output-file", "output file, default stdout", false);
        StringArgument sample     = new StringArgument("s", "sample-id", "sample ID", false);
        
        ArgumentList arguments = new ArgumentList(about, help, Hmlfile, outputFile, sample);
        CommandLine commandLine = new CommandLine(args);

        ExtractMugs validateLdHml = null;
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

            // todo: allow for configuration of glclient
            validateLdHml = new ExtractMugs(Hmlfile.getValue(), outputFile.getValue(), sample.getValue(),  LocalGlClient.create());
        }
        catch (CommandLineParseException e) {
            if (about.wasFound()) {
                About.about(System.out);
                System.exit(0);
            }
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(0);
            }
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
        try {
            System.exit(validateLdHml.call());
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
