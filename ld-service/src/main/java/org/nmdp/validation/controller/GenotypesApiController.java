package org.nmdp.validation.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;

import org.dash.valid.LinkageDisequilibriumAnalyzer;
import org.dash.valid.Sample;
import org.dash.valid.gl.GLStringUtilities;
import org.dash.valid.gl.LinkageDisequilibriumGenotypeList;
import org.dash.valid.gl.haplo.HaplotypePair;
import org.dash.valid.race.RelativeFrequencyByRace;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.annotations.ApiParam;
import io.swagger.api.GenotypesApi;
import io.swagger.model.FindingData;
import io.swagger.model.Genotype;
import io.swagger.model.Genotypes;
import io.swagger.model.HaplotypePairData;
import io.swagger.model.SampleData;
import io.swagger.model.Samples;

@Controller
public class GenotypesApiController implements GenotypesApi {
    @Override
    public ResponseEntity<Samples> submitGenotypes(@ApiParam(value = "Genotypes" ,required=true )  @Valid @RequestBody Genotypes genotypes) {
        List<Genotype> genotypeList = genotypes.getGenotype();
        Samples samples = new Samples();
        SampleData sampleData = null;
    	for (Genotype genotype : genotypeList) {
    		LinkageDisequilibriumGenotypeList linkedGLString = GLStringUtilities.inflateGenotypeList(genotype.getId(), genotype.getGlString(), null);
    		Sample sample = LinkageDisequilibriumAnalyzer.detectLinkages(linkedGLString);
    		sampleData = populateSwaggerObject(sample);
    		samples.addSampleItem(sampleData);
    	}

        return ResponseEntity.ok(samples);
    }

	public SampleData populateSwaggerObject(Sample sample) {
		SampleData sampleData;
		sampleData = new SampleData();
		sampleData.setId(sample.getId());
		sampleData.setGlString(sample.getGlString());
		sampleData.setProcessedGlString(sample.getProcessedGlString());
		for (HaplotypePair pair : sample.getFindings().getLinkedPairs()) {
			HaplotypePairData pairData = new HaplotypePairData();
			pairData.setHaplotype1(pair.getHaplotypes().get(0).getHaplotypeString());
			pairData.setHaplotype2(pair.getHaplotypes().get(1).getHaplotypeString());
			for (RelativeFrequencyByRace freqByRace : pair.getFrequencies()) {
				FindingData finding = new FindingData();
				finding.setRace(freqByRace.getRace());
				finding.setFrequency(new BigDecimal(freqByRace.getFrequency()));
				finding.setRelativeFrequency(new BigDecimal(freqByRace.getRelativeFrequency()));
				finding.setHaplotype1Frequency(new BigDecimal(freqByRace.getHap1Frequency()));
				finding.setHaplotype2Frequency(new BigDecimal(freqByRace.getHap2Frequency()));
				pairData.addFindingItem(finding);

			}
			sampleData.addHaplotypePairItem(pairData);
		}
		return sampleData;
	}
}
