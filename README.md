Immunogenetic Data Tools
=======================

##Use Cases and Implementations

###1. HLA Linkage Disequilibrium

[Linkage disequilibrium](http://en.wikipedia.org/wiki/Linkage_disequilibrium) is the non-random association of alleles at two or more loci, that descend from a single ancestral chromosome.  The particular linkages referenced here are relevant in the context of [HLA](http://en.wikipedia.org/wiki/Human_leukocyte_antigen) and immunogenetics.

HLA typing using Next Generation Sequencing (NGS) is becoming common practice in research and clinical lab settings. HLA typing miss-call occurs when DNA sequences from one of the alleles drop out. 

HLA Linkage Disequilibrium Validation software was developed to identify common linkages between HLA-B and HLA-C, and HLA-DRB1, HLA-DRB3/4/5, HLA-DQA1 and HLA-DQB1. This information is useful when HLA typing from NGS is reviewed. The software not only validates known linkages, but also sends warning messages when unusual linkage was found. 

The software user can find stronger evidences of the accuracy of his/her HLA typing results when common linkages are found. Also the user can focus on reviewing the unusual HLA linkages whether these are true or likely generated from DNA sequencing drop-outs. 

The results of the software should be used for supporting the evidence, but not used to correct any HLA typing without confirmatory experiments.

*Input:*  Genotype(s) - expressed as GL String

* [GL String](http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3715123/) - Genotype List String: a grammar for describing HLA and KIR genotyping results in a text string
* File containing GL Strings separated by newline character
* CSV or Tab-Delimited file containing GL Strings, where first column represents an id associated with the GL String
* [MultiLocusUnphasedGenotype](http://gl.immunogenomics.org/gl-ontology-content/MultilocusUnphasedGenotype.html)
 
*Output:*  Linked alleles by locus, a frequency and any additional notes, accompanied by GL String and Id (either assigned or generated)

*Future Goals:*

 * Provide a key/handle by which reporting may be linked to input(s) - URI from GL Service or other abstract key?
 
*Basic Installation Process:*

* Install Git
* Clone the repository (git clone https://github.com/mpresteg/ImmunogeneticDataTools.git)
* Install Java (written for 1.7)
* Install Maven (configured for 3.2.5)
* Run ‘mvn compile package test’ from the root of the ImmunogeneticDataTools cloned (local) repository

*Running a Test Data Set:*

* Command:  mvn exec:java -Dexec.mainClass="org.dash.valid.LinkageDisequilibriumLoader" -Dexec.args="&lt;filename> &lt;filename>"
* Example:  mvn exec:java -Dexec.mainClass="org.dash.valid.LinkageDisequilibriumLoader" -Dexec.args="resources/test/contrivedExamples.txt resources/test/strictExample.txt resources/test/shorthandExamples.txt resources/test/fullyQualifiedExample.txt"

*Logs:*

* linkage output may be found in linkages.log
* errors and basic logging in immuno.log
 