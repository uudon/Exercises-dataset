# Task 1 report

## Status

`DONE_WITH_CONCERNS`

The supplied male and female GLBs are integrated into the offline asset tree
and pass the byte-integrity checks, but the release gate remains blocked. The
supplied README/package terms are the only evidence recorded; the user's
request to integrate the files is not treated as legal authorization, and no
formal external authorization or metadata was invented.

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
  and SHA-256 values, unresolved formal source URL/repository and
  commit/version fields, README/package-terms-only license and attribution
  text, and blocked formal APK redistribution authorization.
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
expected package sizes. The regenerated male and female GLBs expose the
required 11 logical regions with independent selectable nodes, including
separate biceps/triceps and quadriceps/hamstrings geometry. Their runtime
structure and recorded mesh/material/triangle metrics are valid; release
authorization remains a separate blocked gate.

## Verification

- `./gradlew :app:testDebugUnitTest --tests '*BodyModelValidatorTest'` — passed
  (`BUILD SUCCESSFUL`).
- `tools/verify-body-models.sh` — failed as required for the release gate;
  it reported `valid: false`, `modelCount: 2`, and `totalBytes: 1487700`,
  with unresolved source URL/repository, source commit/version, unconfirmed
  license status, and blocked APK redistribution authorization for both models.
- `git diff --check` — passed.

The model verifier covers manifest and byte integrity plus GLB header/chunk,
node, mesh, material, triangle, selectable-mesh, and required-region checks;
it does not establish device rendering compatibility or release authorization.

The Gradle test run required access to the existing user-level Gradle wrapper
cache outside the workspace. Existing unrelated Gradle warnings remain.

## Concerns / follow-up gate

- Explicit biceps/triceps/quadriceps/hamstring node mapping is blocked by the
  supplied model's generic `upper_arm`/`thigh` regions.
- Rendering Spike, API 26/new-device loading, frame-rate, PSS, APK delta, and
  lifecycle measurements remain unexecuted.
- Formal source URL/repository and source commit/version remain unresolved.
- APK redistribution authorization remains blocked: the README/package terms
  state that distribution is permitted, but no formal external authorization
  was supplied. The user's integration request is not legal evidence.
