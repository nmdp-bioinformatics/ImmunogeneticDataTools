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
