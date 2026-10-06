# Task 1 report

## Status

`DONE_WITH_CONCERNS`

Task 1 is implemented up to the explicit asset-authorization gate. No GLB
files were added, no candidate model was downloaded, and no rendering or
dependency change was made.

## Changes

- Added `BodyModelManifest`, `BodyModelEntry`, `BodyGender`, and
  `BodyModelCheckReport`.
- Added `BodyModelValidator`, which parses the manifest and validates:
  relative offline asset paths, one MALE and one FEMALE entry, asset
  existence, declared byte count, SHA-256, `licenseStatus=confirmed`, source
  URL/repository, source commit/version, license, attribution, and confirmed
  APK redistribution authorization.
- Added the blocked `app/src/main/assets/body/model-manifest.json` and
  `NOTICE.md`. Both model slots remain unconfirmed with zero placeholder
  metadata; no human-model bytes are present.
- Added `tools/verify-body-models.sh`. It emits a JSON report to stdout and
  exits nonzero when the blocked manifest cannot pass. It is compatible with
  the macOS system Bash version and does not depend on network access.
- Added focused unit tests for missing models, SHA-256 mismatch, remote paths,
  unconfirmed licenses, missing metadata, blocked APK authorization, duplicate
  genders, metadata normalization, malformed SHA-256, negative byte counts,
  case-insensitive SHA-256, and two valid models.

## Verification

- `./gradlew :app:testDebugUnitTest --tests '*BodyModelValidatorTest'`
  — passed.
- `tools/verify-body-models.sh` — emitted a JSON failure report and exited 16,
  correctly identifying both blocked metadata records, unconfirmed licenses,
  authorization, invalid SHA-256 metadata, and both missing GLB files.
- `git diff --check` — passed.

The offline verifier now covers the corresponding manifest checks for paths,
metadata normalization, SHA-256 format, byte declarations, licensing, and APK
authorization, including lowercasing the manifest SHA-256 before comparing it
to `shasum`. Kotlin remains the authoritative implementation for asset-byte
verification. The checked-in manifest explicitly records all
source/license/attribution/authorization values as `blocked`; it therefore
remains a failing authorization record.

The Gradle test run required access to the existing user-level Gradle wrapper
cache outside the workspace. Existing unrelated Gradle warnings remain.

## Concerns / follow-up gate

The task brief requires source URL/commit, license, attribution, APK
redistribution authorization, and real asset metrics before adding GLBs or a
rendering Spike. Those facts and assets were not authorized or supplied. The
remaining gate is evidenced by the manifest's blocked metadata, missing GLB
files, and verifier errors for unconfirmed license and APK redistribution
authorization, so the offline verification report must remain failing and the
rendering Spike is intentionally deferred.
