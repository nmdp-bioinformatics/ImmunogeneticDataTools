package org.dash.valid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.race.DisequilibriumElementByRace;
import org.dash.valid.race.FrequencyByRace;

public class LinkageDisequilibriumWriter {
	
	private static final int EXPECTED_LINKAGES = 2;
	
	private static LinkageDisequilibriumWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(LinkageDisequilibriumWriter.class.getName());
	private static FileHandler handler;

	static {
		try {
			handler = new LinkageDisequilibriumFileHandler();
			FILE_LOGGER.addHandler(handler);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private LinkageDisequilibriumWriter() {
		
	}
	
	public static FileHandler getHandler() {
		return handler;
	}
	
	public static LinkageDisequilibriumWriter getInstance() {
		if (instance == null) {
			instance = new LinkageDisequilibriumWriter();
		}
		
		return instance;
	}
	/**
	 * @param linkagesFound
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public synchronized void reportDetectedLinkages(LinkageDisequilibriumGenotypeList linkedGLString, 
			Map<Object, Boolean> linkagesFound) throws SecurityException, IOException {
		int bcLinkages = 0;
		int drdqLinkages = 0;
		
		List<String> commonRaceElementsFound = new ArrayList<String>();
				
		StringBuffer sb = new StringBuffer("Id: " + linkedGLString.getId() + "\nGL String: " + linkedGLString.getGLString());

		List<String> notCommon = GLStringUtilities.checkCommonWellDocumented(linkedGLString.getGLString());
		
		for (String allele : notCommon) {
			sb.append("\nAllele: " + allele + " is not in the Common Well Documented list\n");
		}
		
		if (linkagesFound == null || linkagesFound.size() == 0) {
			sb.append("\n\n");
			sb.append("NO LINKAGES FOUND\n");
		}
		
		int raceLoop = 0;
		
		for (Object linkages : linkagesFound.keySet()) {
			sb.append("\n\n");
			if (linkagesFound.get(linkages).equals(Boolean.TRUE)) {
				sb.append("We found perfect linkages:\n");
			}
			else {
				sb.append("We found partial linkages:\n");
			}
			sb.append(linkages);
			
			if (linkages instanceof BCDisequilibriumElement) {
				bcLinkages++;
			}
			else if (linkages instanceof DRDQDisequilibriumElement) {
				drdqLinkages++;
			}
			
			if (linkages instanceof DisequilibriumElementByRace) {	
				List<String> raceElementsFound = new ArrayList<String>();

				for (FrequencyByRace frequencyByRace : ((DisequilibriumElementByRace)linkages).getFrequenciesByRace()) {
					raceElementsFound.add(frequencyByRace.getRace());
				}
				
				if (raceLoop == 0) {
					commonRaceElementsFound.addAll(raceElementsFound);
				}
				else {
					commonRaceElementsFound.retainAll(raceElementsFound);
				}
			}
			raceLoop++;
		}
		
		if (bcLinkages < EXPECTED_LINKAGES) {
			sb.append("\n");
			sb.append("WARNING: " + (EXPECTED_LINKAGES-bcLinkages) + " BC Linkage(s) not found\n");
		}
		if (drdqLinkages < EXPECTED_LINKAGES) {
			sb.append("\n");
			sb.append("WARNING: " + (EXPECTED_LINKAGES-drdqLinkages) + " DRDQ Linkage(s) not found\n");
		}
		
		if (commonRaceElementsFound.size() == 0) {
			sb.append("\nWARNING: Common Races not found\n");
		}
		else {
			//for (String commonRace : commonRaceElementsFound) {
				sb.append("\n\n");
				sb.append("Common race element(s) found: " + commonRaceElementsFound + "\n");
			//}
		}
		
		sb.append("\n***************************************\n");
	
		FILE_LOGGER.info(sb.toString());
	}
}
