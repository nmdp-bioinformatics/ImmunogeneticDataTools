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

 * Support [HML 1.0](https://bioinformatics.bethematchclinical.org/HLA-Resources/HML/) as input
 
*Basic Installation Process:*

* Install Git
* Clone the repository (git clone https://github.com/nmdp-bioinformatics/ImmunogeneticDataTools.git)
* Install Java (written for 1.8)
* Install Maven (configured for 3.2.5)
* Run ‘mvn compile package test’ from the root of the ImmunogeneticDataTools cloned (local) repository

*Running a Test Data Set:*

* Command:  mvn exec:java -Dexec.mainClass="org.dash.valid.LinkageDisequilibriumAnalyzer" -Dexec.args="&lt;filename> &lt;filename>"
* Example:  mvn exec:java -Dexec.mainClass="org.dash.valid.LinkageDisequilibriumAnalyzer" -Dexec.args="contrivedExamples.txt strictExample.txt shorthandExamples.txt fullyQualifiedExample.txt" -Dorg.dash.frequencies="nmdp" -Dorg.dash.hladb="3.18.0" -Djava.util.logging.config.file="logging.properties"

*Properties:*

+ **Name:**  org.dash.frequencies
+ **Value(s):**  wiki, nmdp-2007, nmdp
+ **Description:**  Specifies the desired frequency set
+ **Note:**  The 2011 NMDP Frequencies (if specifying 'nmdp') are associated with a license agreement, specifying the allowance of use for research, but disallowing re-distribution.  If you wish to use the 2011 NMDP Frequencies, you'll need to install them in your local repository by following the frequency install instructions at the bottom of this file.

+ **Name:**  org.dash.hladb
+ **Value(s):**  3.20.0, 3.19.0, 3.18.0, 3.15.0, 3.12.0, 3.11.0, 3.10.0, 3.9.0, 3.8.0, 3.7.0, 3.6.0, 3.5.0, 3.4.0, 3.3.0, 3.2.0, 3.1.0, 3.0.0
+ **Description:**  Specifies the HLA DB version against which to validate common well documented alleles

+ **Name:**  org.dash.ars
+ **Value(s):**  hladb
+ **Description:**  If specified, applies the antigen recognition site mappings from the HLA DB property specified.  Otherwise, uses the antigen recognition site mappings associated with the NMDP 2011 frequencies

+ **Name:**  org.dash.linkages
+ **Value(s):**  abc, bc, drb_dq, drb_dqb, drb1_dqb1
+ **Description:**  Specifies the loci across which to detect linkages using provided frequencies

+ **Name:**  java.util.logging.config.file
+ **Value(s):**  logging.properties

*Logs:*

* likely haplotype pairs, sorted by relative frequencies may be found in haplotypePairs.log
* gl strings in which a likely B/C or DR/DQ block was not found, may be found in haplotypePairWarnings.log
* haplotype linkage output may be found in linkages.log
* haplotype linkage output for gl strings with missing pairs may be found in linkageWarnings.log
* errors and basic logging in immuno.log


*2011 NMDP Frequency Install Instructions:*
 
+ Access the frequencies at http://frequency.nmdp.org/NMDPFrequencies2011/
+ Login using OpenId
+ Carefully read and accept the license agreement
+ Grab the following files and install at src/main/resources/frequencies/nmdp
+ *A.xlsx, A~C~B.xlsx, B.xlsx, C.xlsx, C~B.xlsx, DQB1.xlsx, DRB1.xlsx, DRB3-4-5.xlsx, DRB3-4-5~DRB1~DQB1.xlsx*

 
