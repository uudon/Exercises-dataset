# Iteration 7 Task 6 TODO

- [ ] Obtain formal source URL/repository, source version/commit, license, attribution, and APK redistribution authorization for both body models; rerun `./gradlew :app:verifyBodyModels`.
- [ ] Run `./gradlew :app:connectedDebugAndroidTest` with an actually attached API 26 baseline device/emulator and record the device identity.
- [ ] Run manual lifecycle checks: background, leave home, return, rotation, process recreation, renderer pause/dispose, and state recovery.
- [ ] Measure cold model load, stable frame rate, touch response, model-page PSS, rotation PSS, and background PSS on API 26, a newer device, and a low-performance device.
- [ ] Execute upgrade regression proving APK resource updates preserve plans, favorites, training records, and history without destructive migration.
- [ ] Re-review the complete TC-036—TC-044 evidence and update acceptance only after all blockers are closed.

Current status: static/build checks passed; authorization, device, performance, and manual lifecycle evidence remain blocked or not run.
