# ld-tools

Command-line entry points built on top of [`ld-validation`](../ld-validation)'s detection
library. Building this module (`mvn package`) produces
`target/ld-tools-0.0.1-SNAPSHOT-bin.zip` (via `maven-assembly-plugin`) and
`target/appassembler/bin/*` (via `appassembler-maven-plugin`) — the same three scripts
either way:

## `analyze-gl-strings`

The main tool. Runs `org.dash.valid.LinkageDisequilibriumAnalyzer` against one or more GL
String files and writes the full report set (`summary.xml`, `linkages.log`,
`haplotypePairs.log`, `detectedFindings.csv`, etc.). Usage, properties, and output format
are documented in the [root README](../README.md).

```
analyze-gl-strings -i <input file> -o <output directory> [-v <hladb version>] [-q <frequency file>]
```

### Heap size for large `-q` files

A custom standard-format frequency file passed via `-q` is loaded fully into memory and
held there for the whole run, at roughly 4–6x its on-disk size. The JVM's default max heap
(~25% of RAM) is not enough for a large one — the NMDP nine-locus release (~1 GB) needs
`-Xmx4g` minimum, `-Xmx6g` comfortably. A too-small heap surfaces as GC thrash followed by
an `OutOfMemoryError` in `HLAFrequenciesLoader.loadStandardReferenceData`. The generated
script honors `JAVA_OPTS`:

```
JAVA_OPTS="-Xmx6g" ./bin/analyze-gl-strings -i input.txt -o output/ -q A~C~B~DRBX~DRB1~DQA1~DQB1~DPA1~DPB1.std.csv
```

See the [root README](../README.md)'s "Memory / heap sizing" note for details. This is the
same nine-locus file `normalize-frequency-file` already raises POI's zip-bomb guard for
(see "Dependencies worth knowing about" below).

## `normalize-frequency-file`

Converts an NMDP haplotype frequency reference file into this project's own
comma-delimited "standard format," so it can be passed to `analyze-gl-strings` via `-q`.
Handles both the legacy per-locus-column `.xls`/`.xlsx` layout and the newer
combined-haplotype layout (one pre-formatted `"Haplotype"` column instead of one column
per locus) — the layout is detected automatically from the file's own header row. Locus
column order for multi-locus files is derived from the input file's own `"~"`-joined name
(e.g. `A~C~B.xlsx`), matching the convention this project's bundled
`ld-validation/src/main/resources/frequencies/nmdp/*.xlsx` files already use — any locus
combination `Locus.lookup()` recognizes works without code changes.

```
normalize-frequency-file -i <input file> -o <output file> [-f single]
```

Use `-f single` for an individual-locus frequency file; omit it (or pass any other value)
for a multi-locus haplotype file.

## `synthetic-gl-string-generator`

Generates synthetic GL Strings from a real NMDP frequency reference file, for building
test fixtures that exercise realistic ambiguity/tie/threshold scenarios without
embedding real, identifiable genotypes. See
`ld-validation/src/test/resources/syntheticExamples.README.txt` for how the committed
`syntheticExamples.txt` fixture was generated and how to regenerate it.

## Dependencies worth knowing about

- `commons-compress` is pinned explicitly here (not left to Maven's default "nearest
  wins" resolution) — `dsh-compress`'s own transitive version doesn't match what
  `poi-ooxml` actually needs, and letting the wrong one win silently breaks any real XLSX
  read.
- `IOUtils.setByteArrayMaxOverride()` is raised in `NormalizeFrequencyFile` above POI's
  default 100MB zip-bomb guard, since some real reference files (e.g. the NMDP
  nine-locus release) exceed it once decompressed. Safe here because this tool only ever
  reads a file the caller names directly on the local filesystem, never an untrusted
  upload.
