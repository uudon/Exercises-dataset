# Iteration 7 Task 6 test report

Date: 2026-10-07

## Result summary

| Area | Status | Evidence |
| --- | --- | --- |
| Unit tests | PASSED | `./gradlew :app:testDebugUnitTest` — `BUILD SUCCESSFUL` |
| Debug APK | PASSED | `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL` |
| Android-test APK | PASSED | `./gradlew :app:assembleDebugAndroidTest` — `BUILD SUCCESSFUL` |
| Android-test Kotlin compile | PASSED | `./gradlew :app:compileDebugAndroidTestKotlin` — `BUILD SUCCESSFUL` |
| Lint | PASSED | `./gradlew :app:lintDebug` — `BUILD SUCCESSFUL` |
| Media resources | PASSED | `./gradlew :app:verifyMediaResources` — 1,324 mappings, local files and SHA-256 verified |
| Offline contract | PASSED | `./gradlew :app:verifyOfflineContract` — local-only contract passed |
| Body models | BLOCKED RELEASE / TECHNICALLY VALID | `./gradlew :app:verifyBodyModels` — expected exit 8 for authorization; report records `runtimeValid=true`, `structureValid=true`, actual GLB metrics, and `licenseStatus=blocked` |
| Diff whitespace | PASSED | `git diff --check` |
| Connected Android test | NOT RUN | `adb devices -l` returned no attached devices/emulators |

## Build artifacts

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; 195,163,905 bytes; SHA-256 `e891be02fcf422c7f373a219bc3b11f2b8b7a74734ef4edfb68c711c1cbc9101`.
- Android-test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`; 1,174,211 bytes; SHA-256 `7186cff679b8082f676dada2a0751fc19e79528e3d0f36b544a21b13c949741a`.

## Generated body-model statistics

| Model | Bytes | Meshes | Triangles | Selectable meshes |
| --- | ---: | ---: | ---: | ---: |
| MALE | 730,028 | 36 | 28,928 | 20 |
| FEMALE | 805,784 | 41 | 31,872 | 22 |
| Total | 1,535,812 | 77 | 60,800 | 42 |

Source: `GLB/fitness_human_glb/stats.json`, cross-checked by `tools/verify-body-models.sh` and recorded in `app/src/main/assets/body/model-check-report.json`.

The mapping validator treats this as technically usable model structure with
release authorization blocked; valid action mappings are not erased solely by
the release gate. Malformed or missing GLB structure still disables actions.

## Explicit non-results

- API 26 device behavior was not manually executed.
- Lifecycle UI evidence (background, leaving home, return, rotation, process recreation, renderer pause/dispose/recovery) was not manually executed; unit lifecycle coverage is not a device result.
- Frame-rate, loading-time, and PSS/memory measurements were not executed because no device/emulator was available.
- Body model authorization evidence was not executed/supplied. The model gate remains blocked and is not represented as passed.
- No performance, device, authorization, or manual lifecycle item is marked passed.
