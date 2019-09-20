package org.nmdp.validation.tools;

import java.io.IOException;

import junit.framework.TestCase;

public class FormatConversionTest extends TestCase {
	
	public void testCSVToJSON() throws IOException {
		//BufferedReader reader = new BufferedReader(new InputStreamReader(
		//		FormatConversionTest.class.getResourceAsStream("fullyQualifiedExample.txt")));
		
//		File file = new File("/Users/mpresteg/bin/datasets/hapTest_removedID.txt");
//		BufferedReader reader = new BufferedReader(new FileReader(file));
//		
//		String line;
//		String[] parts;
//		
//		Genotypes genotypes = new Genotypes();
//		while ((line = reader.readLine()) != null) {
//			parts = line.split("[\t,]");
//			
//			Genotype genotype = new Genotype();
//			genotype.setId(parts[0]);
//			genotype.setGlString(parts[1]);
//			genotypes.addGenotypeItem(genotype);
//		}
//		
//		ObjectMapper objMapper = new ObjectMapper();
//		String json = objMapper.writerWithDefaultPrettyPrinter().writeValueAsString(genotypes);
//		
//		System.out.println(json);
//		
//		FileWriter writer = new FileWriter("/Users/mpresteg/bin/datasets/hapTest.json");
//		writer.write(json);
//		
//		reader.close();
//		writer.close();
	}
}
