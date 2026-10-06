# Iteration 7 Task 3 report

Date: 2026-10-07

Status: renderer lifecycle and failure handling complete; release remains blocked by the existing body-model authorization gate. Device evidence is still outstanding.

## Implemented

- Added production `MuscleModelViewport` overloads and `rememberProductionMuscleModelRenderer()`. The viewport uses SceneView 4.52.0 `rememberEngine()`, `rememberModelLoader(engine)`, `rememberModelInstance(modelLoader, path)`, and `SceneView` for the local `body/male/body.glb` and `body/female/body.glb` assets.
- Replaced the runtime hard-coded region table with the asset-backed `body/muscle-regions.json` map. At app startup, `ValidatedMuscleRegionMap` reads the JSON node lists from both packaged GLB JSON chunks and rejects region nodes absent from the actual male/female assets. Region IDs remain separate from explicit muscle-group IDs for highlighting.
- Added a gesture listener bridge. SceneView tap dispatch is gated by `GestureIntentBridge`, so scroll/rotation and scale callbacks cannot select a region. The bridge has focused unit coverage.
- Implemented real highlighting through each selected renderable node's `materialInstances`: original lists are retained, highlight material instances are created with Filament `MaterialInstance.duplicate` and `baseColorFactor`, non-selected nodes are restored immediately, and duplicated instances are destroyed through the owning Filament `Engine` on restoration/dispose.
- Added lifecycle ownership in `MuscleModelViewport` with `LocalLifecycleOwner`/`LifecycleEventObserver`: start/resume load and restore the model, pause pauses rendering, and stop/dispose releases state. Repeated lifecycle events are idempotent, listeners are cleared on dispose, and load failures render an error fallback and call `onError`.
- Kept engine/model ownership with SceneView's Compose `remember` APIs. Renderer disposal clears its logical asset/listener state; it does not manually destroy SceneView's engine or claim ownership of SceneView-managed resources.
- Routed region JSON/GLB node-map initialization failures through an explicit remembered renderer state and the viewport's existing `onError`/`errorContent` fallback.
- Replaced the direct `rememberModelInstance` call with a lifecycle-equivalent safe loader that catches asynchronous SceneView model failures, reports them through the same error state, and destroys loaded model assets on disposal.
- Preserved local-only path validation, lifecycle restart/pause/stop behavior, material restoration, node mapping, and SceneView engine ownership.

## Verification

- `./gradlew :app:testDebugUnitTest --tests '*MuscleGestureIntentTest' --tests '*MuscleModelLifecycleTest' --offline`: passed.
- `./gradlew :app:assembleDebug :app:lintDebug --offline`: passed.
- `./gradlew :app:verifyMediaResources :app:verifyOfflineContract --offline`: passed.
- Packaged GLB JSON inspection: passed for both `body/male/body.glb` and `body/female/body.glb`; all mapped nodes exist in their corresponding files.
- `./gradlew :app:verifyBodyModels --offline`: blocked with exit 8 because both model manifest entries still have unconfirmed license/source metadata and blocked APK redistribution authorization. This is the documented Task 1 gate, not a renderer/build failure.
- Focused regression tests cover malformed region JSON, truncated/missing GLB input, and forwarding SceneView model-load failures to the viewport error listener.
- `git diff --check`: passed.

## Evidence limitations

- No device or manual UI run has been performed yet.
- API 26 behavior has not been manually verified.
- No PSS/memory measurement or frame-rate measurement has been performed.
- The current frame-rate policy remains SceneView `OnDemand`; runtime performance evidence requires a device run.
- The Compose `produceState` model-loading error path and the `onError`/`errorContent` rendering were verified by compilation and focused callback/unit coverage; no device/manual Compose UI run was performed.

## Remaining blocker

Obtain and record formal source URL/version, license confirmation, attribution, and APK redistribution authorization for both bundled GLBs, then rerun `verifyBodyModels`. No release authorization is claimed by this Task 3 implementation.
