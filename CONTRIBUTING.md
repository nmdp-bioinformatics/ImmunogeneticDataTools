# Contributing

## Project layout

This is a Maven multi-module reactor:

| Module | Purpose |
|---|---|
| [`ld-validation`](ld-validation) | Core HLA linkage disequilibrium detection library |
| [`ld-tools`](ld-tools) | Command-line tools built on `ld-validation` |
| [`ld-service`](ld-service) | REST wrapper (Spring Boot) around `ld-validation`, plus an auto-generated client (`ld-service/ld-client`) |

Each module has its own README with more detail on what's actually in it.

## Building and testing

```
mvn clean install
```

builds the full reactor. To work on one module without rebuilding everything:

```
mvn -pl <module> -am test        # e.g. -pl ld-validation
mvn -pl <module> -am verify      # also runs JaCoCo coverage reporting for ld-validation
```

Requires Java 25 (LTS) and Maven 3.8.6+ — see the [root README](README.md) for local JDK
setup if `java`/`mvn` don't pick up the right version automatically.

Some tests depend on which IMGT/HLA reference snapshot (`hladb`) resolves at test time;
if a test's expected allele/G-group counts look wrong after a new IMGT/HLA release, check
the `hladb` Maven property and the `org.dash.hladb` system property before assuming it's
a real regression.

### Running `ld-service` for local iteration

`ld-service/README.md`'s "Running it" only documents the packaged jar, which is what an
end user of the service actually wants. For a faster edit-run loop while working on
`ld-service` itself, `spring-boot:run` skips the packaging step:

```
mvn -f ld-service/pom.xml spring-boot:run
```

`-f` (point at this module's own `pom.xml` directly) matters here, not just style: the
more obvious `mvn -pl ld-service -am spring-boot:run` looks equivalent but fails with
"Unable to find a suitable main class" — the root `pom.xml` (artifactId `ld-multimodule`)
itself inherits from `spring-boot-starter-parent`, so a bare `plugin:goal` invocation (as
opposed to a lifecycle phase) tries to run `spring-boot:run` against every project
`-pl ld-service -am` pulls into the reactor, including that root aggregator — which has
no main class and fails before the reactor ever reaches `ld-service`. `-f` builds only
this module, sidestepping the aggregator entirely — which means `ld-validation` needs to
already be installed to your local repo (`mvn install` from the root at least once; plain
`mvn clean package` doesn't put it there), since `-f` doesn't build it alongside `ld-service`
in the same reactor pass the way `-am` would.

Don't use `spring-boot:run` to judge real performance, though — it launches with
`-XX:TieredStopAtLevel=1` (Spring Boot Maven Plugin's own dev-mode default, not anything
configured here), which disables the JVM's C2 JIT compiler to speed up startup for a quick
edit-run loop, at a real cost to throughput on long-running CPU-bound work (e.g. parsing a
large custom frequency file — see `ld-service/README.md`'s note on `LOADING_REFERENCE_DATA`
taking minutes for real reference data). Use the packaged jar for that instead.

## Branch / PR workflow

- One branch per unit of work, targeted at this repo's `master`.
- Open a PR; CircleCI runs the full reactor build + test suite automatically. **PRs
  opened from a fork don't trigger CI on `nmdp-bioinformatics/ImmunogeneticDataTools`** —
  if you're working from a personal fork and don't see a CircleCI check appear, that's
  why; ask a maintainer to help land it, or push your branch directly to this repo if you
  have access.
- A green CI run is necessary but not sufficient for merging — it only proves existing
  tests still pass, not that the approach is right. Non-trivial changes get a real review
  regardless of CI status.
- Prefer verifying behavior for real over trusting `mvn test` alone where it matters —
  e.g. actually booting `ld-service` and hitting an endpoint after a dependency bump,
  not just checking that unit tests are green. Several real, previously-undetected bugs
  in this project were only caught this way.

## Commit messages

Scale detail to how subtle or risky the change is. A one-line summary is fine for a
small, self-evident fix (a typo, a version bump with no surprises). For a real bug fix or
anything non-obvious, explain the root cause, what was tried, and what verification
actually checked — not just what changed.

## License

LGPL v3 (see the header in any source file for the full notice, and
<https://www.gnu.org/licenses/lgpl.html> for the license text).
