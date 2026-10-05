# Implementation Plan: Complete Iteration 1 and Iteration 2 TODO

## Overview

Close all verifiable TODO items for I1-T01 and I2-T01, preserving explicit blockers for work that requires media distribution authorization or a functioning physical/emulator deployment target.

## Dependency Graph

1. Reproduce the current baseline and inspect local-only resource/data paths.
2. Complete missing I2 behavior and tests: unsaved editor exit, migration coverage, and repository/UI verification.
3. Add reproducible I1 measurement and static validation evidence for media, APK size, loading, and memory where the host permits it.
4. Run build, lint, unit/device tests, review documentation, and commit/push the atomic completion.

## Task List

### Task 1: Baseline and evidence audit

- [x] Compare `doc/健身App开发文档/03_TODO.md` with implementation and existing reports.
- [x] Run resource verification, unit tests, build, and lint.
- [x] Identify device/authorization blockers without marking them complete.

### Task 2: Finish plan editor behavior

- [x] Add unsaved-exit save/discard confirmation.
- [x] Add focused tests for update/reopen and validation error behavior.
- [x] Add a real Room v1→v2 migration test or an executable migration verification path.

### Task 3: Complete reproducible offline-media evidence

- [x] Add/execute static no-remote-resource validation.
- [x] Record actual resource counts, sizes, APK size, and host-side timing evidence.
- [x] Keep device-only offline/GIF lifecycle/memory checks explicitly unexecuted if deployment remains blocked; available emulator evidence is recorded.

### Checkpoint: I1/I2 verification

- [x] Unit tests pass.
- [x] Debug APK builds.
- [x] Lint passes.
- [x] Device test/manual evidence is either successful or recorded as blocked with the exact error.

### Task 4: Documentation and delivery

- [x] Update both iteration TODOs, test reports, review reports, and acceptance status.
- [x] Run diff/security checks.
- [x] Commit with a conventional message and push to `origin/main`.

## Acceptance Criteria

- All verifiable I1/I2 TODO items are checked with command or file evidence.
- Authorization, device-only lifecycle, and upgrade checks are never checked without actual evidence.
- Existing favorites/plans/history are not deleted by static media or Room migration changes.
- The repository is clean after the delivery commit.

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Connected devices reject APK installation | Cannot prove manual/device flows | Retry once after a clean install path; retain exact failure evidence if still blocked |
| Gym visual APK redistribution authorization is not available | I1 release acceptance remains blocked | Preserve assets and attribution; do not claim legal approval |
| Room migration fixture setup is incomplete | Upgrade acceptance remains unverified | Add an executable migration test or document the exact missing fixture |
