Immunogenetic Data Tools
=======================

##Use Cases and Implementations

###1. HLA Linkage Disequilibrium

[Linkage disequilibrium](http://en.wikipedia.org/wiki/Linkage_disequilibrium) is the non-random association of alleles at two or more loci, that descend from a single ancestral chromosome.  The particular linkages referenced here are relevant in the context of [HLA](http://en.wikipedia.org/wiki/Human_leukocyte_antigen) and immunogenetics.

The use case is to enable a clinician or researcher in the immunogenetic or histocompatibility space to be made aware of HLA linkage disequilibrium given a genotype.

*Input:*  Genotype(s) - expressed as GL String

 * [GL String](http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3715123/) - Genotype List String: a grammar for describing HLA and KIR genotyping results in a text string
 
*Output:*  Linked alleles by locus, a frequency and any additional notes

*Future Goals:*

 * Provide a key/handle by which reporting may be linked to input(s) - URI from GL Service or other abstract key?
 
*Basic Installation Process:*

 * Install Git
 * Clone the repository (git clone https://github.com/mpresteg/ImmunogeneticDataTools.git)
 * Install Maven 3.2.5 or greater
 * Run ‘mvn compile package test’ from the root of the ImmunogeneticDataTools cloned (local) repository

*Running a test data set:*

 * mvn exec:java -Dexec.mainClass="org.dash.valid.LinkageDisequilibriumLoader" -Dexec.args="resources/test/stanfordExamples.txt"
 