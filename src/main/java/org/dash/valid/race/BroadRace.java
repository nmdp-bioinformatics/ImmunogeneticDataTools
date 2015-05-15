package org.dash.valid.race;

public enum BroadRace {
	AFA,
	API,
	NAM,
	HIS,
	CAU;
	
	public static boolean contains(String race) {
		try {
			if (BroadRace.valueOf(race) != null) {
				return true;
			}
		}
		catch (IllegalArgumentException ex) {
			return false;
		}

		return false;
	}
}
