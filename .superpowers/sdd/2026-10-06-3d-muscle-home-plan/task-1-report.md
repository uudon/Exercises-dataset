# Task 1 report

## Status

`DONE_WITH_CONCERNS`

The supplied male and female GLBs are integrated into the offline asset tree
and pass the file-integrity gate. The supplied README is the only provenance
and licensing source used; no external authorization or metadata was invented.

## Changes

- Added `BodyModelManifest`, `BodyModelEntry`, `BodyGender`, and
  `BodyModelCheckReport`.
- Added `BodyModelValidator`, which parses the manifest and validates:
  relative offline asset paths, one MALE and one FEMALE entry, asset
  existence, declared byte count, SHA-256, `licenseStatus=confirmed`, source
  URL/repository, source commit/version, license, attribution, and confirmed
  APK redistribution authorization.
- Added `app/src/main/assets/body/male/body.glb` and
  `app/src/main/assets/body/female/body.glb` from the supplied local package.
- Updated `app/src/main/assets/body/model-manifest.json` with actual byte sizes
  and SHA-256 values, local-package provenance, README-based license and
  attribution text, and redistribution authorization confirmed by the user's
  explicit integration request.
- Updated `NOTICE.md` because its previous “not included” statement became
  false after the supplied assets were integrated.
- Added `tools/verify-body-models.sh`. It emits a JSON report to stdout and
  exits nonzero when the blocked manifest cannot pass. It is compatible with
  the macOS system Bash version and does not depend on network access.
- Added focused unit tests for missing models, SHA-256 mismatch, remote paths,
  unconfirmed licenses, missing metadata, blocked APK authorization, duplicate
  genders, metadata normalization, malformed SHA-256, negative byte counts,
  case-insensitive SHA-256, and two valid models.

## Model review

Both files are technically valid glTF 2.0 GLB containers and have the
expected package sizes. The models expose generic selectable `upper_arm` and
`thigh` regions. They do not provide independent biceps, triceps, quadriceps,
or hamstring nodes, so they cannot yet support the plan's required explicit
muscle-level hit mapping for those groups.

## Verification

- `./gradlew :app:testDebugUnitTest --tests '*BodyModelValidatorTest'` — passed
  (`BUILD SUCCESSFUL`).
- `tools/verify-body-models.sh` — passed with `valid: true`, `modelCount: 2`,
  and `totalBytes: 1487700`.
- `git diff --check` — passed.

The model verifier covers manifest and byte-integrity checks; it does not
establish muscle-node granularity or device rendering compatibility.

The Gradle test run required access to the existing user-level Gradle wrapper
cache outside the workspace. Existing unrelated Gradle warnings remain.

## Concerns / follow-up gate

- Explicit biceps/triceps/quadriceps/hamstring node mapping is blocked by the
  supplied model's generic `upper_arm`/`thigh` regions.
- Rendering Spike, API 26/new-device loading, frame-rate, PSS, APK delta, and
  lifecycle measurements remain unexecuted.
- The supplied package provides no commit/version or external URL; provenance
  is therefore recorded as the local package path only.
