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
package org.dash.valid;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.Haplotype;
import org.dash.valid.handler.ProgressConsoleHandler;
import org.dash.valid.report.CommonWellDocumentedWriter;
import org.dash.valid.report.DetectedFindingsWriter;
import org.dash.valid.report.DetectedLinkageFindings;
import org.dash.valid.report.HaplotypePairWriter;
import org.dash.valid.report.LinkageDisequilibriumWriter;
import org.dash.valid.report.SamplesList;
import org.dash.valid.report.SummaryWriter;

public class LinkageDisequilibriumAnalyzer {		
    private static final Logger LOGGER = Logger.getLogger(LinkageDisequilibriumAnalyzer.class.getName());
    
    static {
    	try {
			LogManager.getLogManager().readConfiguration(LinkageDisequilibriumAnalyzer.class.getClassLoader().getResourceAsStream("logging.properties"));
			LOGGER.addHandler(new ProgressConsoleHandler());
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
    }
    
	public static void main(String[] args) {
		try {
			analyzeGLStringFiles(args);
		} catch (IOException e) {
			LOGGER.severe("Unable to process.  Please check log files and configuration.");
			return;
		}
		
		for (Handler handler : LogManager.getLogManager().getLogger(LinkageDisequilibriumWriter.class.getName()).getHandlers()) {
			handler.close();
		}
		
		for (Handler handler : LogManager.getLogManager().getLogger(HaplotypePairWriter.class.getName()).getHandlers()) {
			handler.close();
		}
		
		for (Handler handler : LogManager.getLogManager().getLogger(CommonWellDocumentedWriter.class.getName()).getHandlers()) {
			handler.close();
		}
		
		DetectedFindingsWriter.getInstance().closeWriters();
		
		SummaryWriter.getInstance().closeWriters();
	}
	
	private static void analyzeGLStringFiles(String[] filenames) throws IOException {		
		for (int i=0;i<filenames.length;i++) {
			LOGGER.info("Processing file: " + filenames[i] + " (" + (i+1) + " of " + filenames.length + ")");
			analyzeGLStringFile(filenames[i]);		
		}		
	}
	
	public static List<Sample> analyzeGLStringFile(String name, BufferedReader reader) throws IOException {
		List<LinkageDisequilibriumGenotypeList> glStrings = GLStringUtilities.readGLStringFile(name, reader);
		
		List<Sample> samplesList = detectLinkages(glStrings);
		
		return samplesList;
	}

	/**
	 * @param filename
	 */
	public static void analyzeGLStringFile(String filename) throws IOException {				
		List<LinkageDisequilibriumGenotypeList> glStrings = GLStringUtilities.readGLStringFile(filename);
		List<Sample> samplesList = null;
		
		samplesList = detectLinkages(glStrings);
		
		for (Sample sample : samplesList) {
			DetectedLinkageFindings findings = sample.getFindings();
			LinkageDisequilibriumWriter.getInstance().reportDetectedLinkages(findings);
			HaplotypePairWriter.getInstance().reportDetectedLinkages(findings);
			CommonWellDocumentedWriter.getInstance().reportCommonWellDocumented(findings);
			DetectedFindingsWriter.getInstance().reportDetectedFindings(findings);
		}
		
		SamplesList allSamples = new SamplesList();
		allSamples.setSamples(samplesList);
		
		SummaryWriter.getInstance().reportDetectedLinkages(allSamples);
	}

	/**
	 * @param glStrings
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	private static List<Sample> detectLinkages(List<LinkageDisequilibriumGenotypeList> glStrings) {
		List<Sample> samplesList = new ArrayList<Sample>();
		
		int idx = 1;
		for (LinkageDisequilibriumGenotypeList linkedGLString : glStrings) {
			
			List<Haplotype> knownHaplotypes = GLStringUtilities.buildHaplotypes(linkedGLString);
			
			LOGGER.info("Processing gl string " + idx + " of " + glStrings.size() + " (" + (idx*100)/glStrings.size() + "%)");
			idx++;

			if (knownHaplotypes.size() > 0) {
				samplesList.add(HLALinkageDisequilibrium.hasLinkageDisequilibrium(linkedGLString, knownHaplotypes));

			}
			else {	
				boolean homozygousOnly = Boolean.parseBoolean(System.getProperty("org.dash.homozygous")) == Boolean.TRUE ? Boolean.TRUE : Boolean.FALSE;
				
				if (!linkedGLString.checkAmbiguitiesThresholds()) {
					LOGGER.info("GL String contains an unusual number of ambiguities, proteins and/or uncommon alleles");
				}
				
				if (homozygousOnly && !linkedGLString.hasHomozygous(LinkagesLoader.getInstance().getLoci())) {
					LOGGER.info("Only checking for homozygous.  GL String contains no homozygous typings for the loci in question.  Bypassing record.");
					continue;
				}
	
				samplesList.add(detectLinkages(linkedGLString));
			}
		}
		
		return samplesList;
	}
	
//	public static Sample detectLinkages(MultilocusUnphasedGenotype mug) {
//		LinkageDisequilibriumGenotypeList linkedGLString = new LinkageDisequilibriumGenotypeList(mug.getId(), mug);
//		Sample sample = detectLinkages(linkedGLString);
//
//		return sample;
//	}

	public static Sample detectLinkages(LinkageDisequilibriumGenotypeList linkedGLString) {
		Sample sample = HLALinkageDisequilibrium.hasLinkageDisequilibrium(linkedGLString);
				
		return sample;
	}
}
