Immunogenetic Data Tools (HLAHapV)
=======================

[![nmdp-bioinformatics](https://circleci.com/gh/nmdp-bioinformatics/ImmunogeneticDataTools.svg?style=svg)](https://app.circleci.com/pipelines/github/mpresteg/ImmunogeneticDataTools)

#### HLA Linkage Disequilibrium
[Linkage disequilibrium](http://en.wikipedia.org/wiki/Linkage_disequilibrium) is the non-random association of alleles at two or more loci, that descend from a single ancestral chromosome.  The particular linkages referenced here are relevant in the context of [HLA](http://en.wikipedia.org/wiki/Human_leukocyte_antigen) and immunogenetics.

HLA typing using Next Generation Sequencing (NGS) is becoming common practice in research and clinical lab settings. HLA typing miss-call occurs when DNA sequences from one of the alleles drop out. 

HLAHapV[1] software was developed to identify common linkages between HLA-B and HLA-C, and HLA-DRB1, HLA-DRB3/4/5, HLA-DQA1 and HLA-DQB1. This information is useful when HLA typing from NGS is reviewed. The software not only validates known linkages, but also sends warning messages when unusual linkage was found. 

The software user can find stronger evidences of the accuracy of his/her HLA typing results when common linkages are found. Also the user can focus on reviewing the unusual HLA linkages whether these are true or likely generated from DNA sequencing drop-outs. 

The results of the software should be used for supporting the evidence, but not used to correct any HLA typing without confirmatory experiments.

*Input:*  Genotype(s) - expressed as GL String

* [GL String](http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3715123/) - Genotype List String: a grammar for describing HLA and KIR genotyping results in a text string
* HML 1.x
* File containing GL Strings separated by newline character
* CSV or Tab-Delimited file containing GL Strings, where first column represents an id associated with the GL String
 
*Output:*  Linked alleles by locus, a frequency and any additional notes, accompanied by GL String and Id (either assigned or generated)

*Future Goals:*

 * Host publicly
 
#### Using the software:
As of release .7, the ability to download the software package and make use of command line tools is available.

From the Releases section of GitHub you may grab the snapshot of the latest release.  E.g:  ld-tools-0.0.1-SNAPSHOT-bin.zip from release .7 at [Releases](https://github.com/nmdp-bioinformatics/ImmunogeneticDataTools/releases)

After un-zipping the software, you may run ./ld-tools-0.0.1-SNAPSHOT/bin/analyze-gl-strings -h for instructions on how to run the software.

*Alternatively - Basic Installation Process from source code:*

If you prefer to compile / package the software from source, follow these instructions...

* Install Git
* Clone the repository (git clone https://github.com/nmdp-bioinformatics/ImmunogeneticDataTools.git)
* Install Java (written for 1.8)
* Install Maven (configured for 3.3.3)
* Run ‘mvn clean package’ from the root of the ImmunogeneticDataTools cloned (local) repository

*Running a Test Data Set:*

* Directory:  'cd ld-validation' from the root of the ImmunogeneticDataTools cloned (local) repository
* Command:  mvn exec:java -Dexec.mainClass="org.dash.valid.LinkageDisequilibriumAnalyzer" -Dexec.args="&lt;filename> &lt;filename>"
* Example:  mvn exec:java -Dexec.mainClass="org.dash.valid.LinkageDisequilibriumAnalyzer" -Dexec.args="contrivedExamples.txt strictExample.txt shorthandExamples.txt fullyQualifiedExample.txt" -Dorg.dash.frequencies="nmdp" -Dorg.dash.hladb="3.18.0" -Djava.util.logging.config.file="logging.properties"

*Properties:*

+ **Name:**  org.dash.frequencies
+ **Value(s):**  wiki, nmdp-2007 (nmdp-2007-std), nmdp (nmdp-std)
+ **Description:**  Specifies the desired frequency set
+ **Note:**  The 2011 NMDP Frequencies (if specifying 'nmdp') are associated with a license agreement, specifying the allowance of use for research, but disallowing re-distribution.  If you wish to use the 2011 NMDP Frequencies, you'll need to install them in your local repository by following the frequency install instructions at the bottom of this file.

+ **Name:**  org.dash.hladb
+ **Value(s):**  Valid HLA DB name (e.g. 3.20.0).  Older versions were not available in XML format and ars reduction logic will fall back on default logic in these instances
+ **Description:**  Specifies the HLA DB version against which to validate common well documented alleles (using CWD 2.0).  If that HLADB version is not available in the CWD 2.0 reference file, then the latest available HLADB version within the reference file is used.  This property also informs the ARS reduction logic

+ **Name:**  org.dash.ars
+ **Value(s):**  Default
+ **Description:**  If specified, applies the antigen recognition site mappings from the time of the NMDP 2011 Frequencies.  Otherwise, tries to use the antigen recognition site mappings associated with the specified HLA DB first, falling back on the default if they aren't available

+ **Name:**  org.dash.linkages
+ **Value(s):**  acb, cb, drb_dq, drb_dqb, drb1_dqb1, fiv_loc, six_loc
+ **Description:**  Specifies the loci across which to detect linkages using provided frequencies

+ **Name:**  java.util.logging.config.file
+ **Value(s):**  logging.properties

*Logs:*

* likely haplotype pairs, sorted by relative frequencies may be found in haplotypePairs.log
* gl strings in which a likely block was not found, may be found in haplotypePairWarnings.log
* haplotype linkage output may be found in linkages.log
* haplotype linkage output for gl strings with missing pairs may be found in linkageWarnings.log
* a consolidation of all of the information contained in the above logs may be found in summary.xml, for easy parsing
* errors and basic logging in immuno.log

*Common Well Documented 2.0 and Common Intermediate Well Documented 3.0 Behavior:*

+ In order to cut down on the amount of warnings produced for ambiguous alleles, the CWD 2.0 and CIWD 3.0 validations are performed only at the protein level

*2011 NMDP Frequency Install Instructions:*
 
+ Access the frequencies at http://frequency.nmdp.org/NMDPFrequencies2011/
+ Login using OpenId
+ Carefully read and accept the license agreement
+ Grab the following files and install at src/main/resources/frequencies/nmdp
+ *A.xlsx, A\~C\~B.xlsx, C.xlsx, B.xlsx, C\~B.xlsx, DRB3-4-5.xlsx, DRB1.xlsx, DQB1.xlsx, DRB3-4-5\~DRB1\~DQB1.xlsx*

*2011 NMDP Frequency Re-Formatting Instructions:*

+ The command line feature of the software (analyze-gl-strings) accepts frequencies in a standard format
+ To turn the 2011 NMDP Frequencies into that format, a command line tool called normalize-frequency-file has been created
+ After using the tool to convert the frequency files, the newly formatted files may be passed into analyze-gl-strings as a command line argument
+ If preferring to run using mvn (w/o command line tools), the newly formatted frequency files can be dropped into resources/frequencies/std and invoked by specifying nmdp-std as the frequency type

## References:
1.  K. Osoegawa et al., HLA Haplotype Validator for quality assessments of HLA typing, Hum. Immunol. (2015),
[http://dx.doi.org/10.1016/j.humimm.2015.10.018](http://dx.doi.org/10.1016/j.humimm.2015.10.018)
