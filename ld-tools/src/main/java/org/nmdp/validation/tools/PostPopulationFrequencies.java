///*
//
//    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)
//
//    This library is free software; you can redistribute it and/or modify it
//    under the terms of the GNU Lesser General Public License as published
//    by the Free Software Foundation; either version 3 of the License, or (at
//    your option) any later version.
//
//    This library is distributed in the hope that it will be useful, but WITHOUT
//    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
//    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
//    License for more details.
//
//    You should have received a copy of the GNU Lesser General Public License
//    along with this library;  if not, write to the Free Software Foundation,
//    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.
//
//    > http://www.gnu.org/licenses/lgpl.html
//
//*/
//package org.nmdp.validation.tools;
//
//import static org.dishevelled.compress.Readers.reader;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.concurrent.Callable;
//
//import org.dash.valid.gl.GLStringConstants;
//import org.dishevelled.commandline.ArgumentList;
//import org.dishevelled.commandline.CommandLine;
//import org.dishevelled.commandline.CommandLineParseException;
//import org.dishevelled.commandline.CommandLineParser;
//import org.dishevelled.commandline.Switch;
//import org.dishevelled.commandline.Usage;
//import org.dishevelled.commandline.argument.FileArgument;
//import org.dishevelled.commandline.argument.StringArgument;
//
//import io.swagger.client.model.HFCurationRequest1;
//import io.swagger.client.model.HaplotypeFrequency1;
//import io.swagger.client.model.HaplotypeFrequencyData1;
//import io.swagger.client.model.License1;
//
///**
// * PostPopulationFrequencies
// *
// */
//public class PostPopulationFrequencies implements Callable<Integer> {
//	
//    private final File inputFile;
//    private final String accessId;
//    private final String cohortId;
//
//    private static final String USAGE = "post-population-frequencies [args]";
//
//
//    /**
//     * Post population frequencies to the frequency curation service
//     *
//     * @param inputFile input file
//     * @param accessId
//     * @param cohortId
//     */
//    public PostPopulationFrequencies(File inputFile, String accessId, String cohortId) {
//        this.inputFile = inputFile;
//        this.accessId = accessId;
//        this.cohortId = cohortId;
//    }
//    
//    @Override
//    public Integer call() throws Exception {
//    	postPopulationFrequencies(reader(inputFile));
//    	
//    	return 0;
//	}  
//    
//	public void postPopulationFrequencies(BufferedReader reader) throws IOException {
//		String row;
//		String[] columns;
//
//		HashMap<String, HaplotypeFrequencyData1> populationMap = new HashMap<String, HaplotypeFrequencyData1>();
//		HaplotypeFrequencyData1 haplotypeFrequencyData;
//                
//        License1 license = new License1();
//        license.setTypeOfLicense(io.swagger.client.model.License1.TypeOfLicenseEnum.CC0);
//                
//		while ((row = reader.readLine()) != null) {		
//			columns = row.split(GLStringConstants.COMMA);
//						
//			String race = columns[0];
//			String haplotype = columns[1];
//			Double frequency = new Double(columns[2]);
//			
//			if (populationMap.containsKey(race)) {
//				haplotypeFrequencyData = populationMap.get(race);
//			}
//			else {
//				haplotypeFrequencyData = new HaplotypeFrequencyData1();
//		        haplotypeFrequencyData.setLicense(license);
//			}
//									
//	        HaplotypeFrequency1 hapFrequency = new HaplotypeFrequency1();
//	        hapFrequency.setFrequency(new BigDecimal(frequency));
//	        hapFrequency.setHaplotypeString(haplotype);
//	        haplotypeFrequencyData.addHaplotypeFrequencyListItem(hapFrequency);
//
//	        populationMap.put(race, haplotypeFrequencyData);
//		}
//		
//		reader.close();
//		
//		for (String populationId : populationMap.keySet()) {
//	        HFCurationRequest1 hfCurationRequest = new HFCurationRequest1();
//
//	        hfCurationRequest.setAccessID(accessId);
//	        hfCurationRequest.setPopulationID(populationId);
//	        hfCurationRequest.setHaplotypeFrequencyData(populationMap.get(populationId));
//	        hfCurationRequest.setCohortID(cohortId);
//	        
//	        //HFCurationResponse1 response = api.hfcPost(hfCurationRequest);
//	        //System.out.println(response);
//	        
//	        System.out.println("Population: " + hfCurationRequest.getPopulationID() + " and haplotypes: " + hfCurationRequest.getHaplotypeFrequencyData().getHaplotypeFrequencyList().size());
//	        
//	        System.out.println("Here's the first two...");
//	        
//	        System.out.println(hfCurationRequest.getHaplotypeFrequencyData().getHaplotypeFrequencyList().get(0));
//	        System.out.println(hfCurationRequest.getHaplotypeFrequencyData().getHaplotypeFrequencyList().get(1));
//		}
//	}
//
//    /**
//     * Main.
//     *
//     * @param args command line args
//     */
//    public static void main(final String[] args) {
//        Switch about = new Switch("a", "about", "display about message");
//        Switch help  = new Switch("h", "help", "display help message");
//        FileArgument inputFile = new FileArgument("i", "input-file", "input file, default stdin", true);
//        StringArgument accessId = new StringArgument("s", "accessId", "accessId", true);
//        StringArgument cohortId = new StringArgument("c", "cohortId", "cohortId", true);
//
//        ArgumentList arguments  = new ArgumentList(about, help, inputFile, accessId, cohortId);
//        CommandLine commandLine = new CommandLine(args);
//
//        PostPopulationFrequencies postPopulationFrequencies = null;
//        try
//        {
//            CommandLineParser.parse(commandLine, arguments);
//            if (about.wasFound()) {
//                About.about(System.out);
//                System.exit(0);
//            }
//            if (help.wasFound()) {
//                Usage.usage(USAGE, null, commandLine, arguments, System.out);
//                System.exit(0);
//            }
//            postPopulationFrequencies = new PostPopulationFrequencies(inputFile.getValue(), accessId.getValue(), cohortId.getValue());
//        }
//        catch (CommandLineParseException | IllegalArgumentException e) {
//            Usage.usage(USAGE, e, commandLine, arguments, System.err);
//            System.exit(-1);
//        }
//        try {
//            System.exit(postPopulationFrequencies.call());
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }
//    
//}
