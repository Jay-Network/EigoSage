# EigoSage Bugs

## Open

(None yet)

## Resolved

- **BUG-001: Release AAB CI broken since 2026-07-23** — `android-build.yml`'s "Decode keystore" step wrote to `keystore/eigosage-release.jks` without creating the directory, failing every release build (debug + tests unaffected). Found by jworks:22/jworks-codex:22's CI sweep, handed off 2026-09-25. Fixed v0.8.4: added `mkdir -p keystore`. [@solo] (2026-09-25)
