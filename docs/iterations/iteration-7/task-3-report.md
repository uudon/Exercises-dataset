# Iteration 7 Task 3 report

Date: 2026-10-07

Status: renderer integration complete; release remains blocked by the existing body-model authorization gate.

## Implemented

- Added production `MuscleModelViewport` overloads and `rememberProductionMuscleModelRenderer()`. The viewport uses SceneView 4.52.0 `rememberEngine()`, `rememberModelLoader(engine)`, `rememberModelInstance(modelLoader, path)`, and `SceneView` for the local `body/male/body.glb` and `body/female/body.glb` assets.
- Replaced the runtime hard-coded region table with the asset-backed `body/muscle-regions.json` map. `ValidatedMuscleRegionMap` validates canonical coverage, duplicate regions/nodes, and non-empty gender-specific node sets; its `fromRegions` API also rejects nodes absent from a supplied gender model-node set.
- Added a gesture listener bridge. SceneView tap dispatch is gated by `GestureIntentBridge`, so scroll/rotation and scale callbacks cannot select a region. The bridge has focused unit coverage.
- Implemented real highlighting through each selected renderable node's `materialInstances`: original lists are retained, highlight material instances are created with Filament `MaterialInstance.duplicate` and `baseColorFactor`, and non-selected nodes are restored to their original material lists.
- Kept engine/model ownership with SceneView's Compose `remember` APIs. Renderer disposal clears its logical asset/listener state; it does not manually destroy SceneView's engine or claim ownership of SceneView-managed resources.

## Verification

- `./gradlew :app:testDebugUnitTest --tests '*MuscleGestureIntentTest' --tests '*MuscleModelLifecycleTest' --offline`: passed.
- `./gradlew :app:assembleDebug :app:lintDebug --offline`: passed.
- `./gradlew :app:verifyMediaResources :app:verifyOfflineContract --offline`: passed.
- `git diff --check`: passed.
- `./gradlew :app:verifyBodyModels --offline`: blocked with exit 8 because both model manifest entries still have unconfirmed license/source metadata and blocked APK redistribution authorization. This is the documented Task 1 gate, not a renderer/build failure.

## Remaining blocker

Obtain and record formal source URL/version, license confirmation, attribution, and APK redistribution authorization for both bundled GLBs, then rerun `verifyBodyModels`. No release authorization is claimed by this Task 3 implementation.
