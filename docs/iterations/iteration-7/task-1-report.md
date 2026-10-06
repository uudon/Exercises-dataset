# I7 Task 1 model-geometry report

日期：2026-10-07。状态：模型几何门槛完成；正式发布授权门禁仍阻塞。

## 产物

`GLB/fitness_human_glb/generate.py` now uses an offline standard-library-only
procedural generator. Male and female GLBs each expose independent selectable
nodes for the shared logical regions `chest`, `shoulders`, `back`, `biceps`,
`triceps`, `forearms`, `abs`, `glutes`, `quadriceps`, `hamstrings`, and
`calves`. Biceps/triceps and quadriceps/hamstrings use separate geometry, not
renamed generic nodes. Clothing remains opaque and there are no textures,
remote references, or network inputs.

| Model | Bytes | Meshes | Triangles | Selectable meshes |
| --- | ---: | ---: | ---: | ---: |
| Male | 730,028 | 36 | 28,928 | 20 |
| Female | 805,784 | 41 | 31,872 | 22 |

The generated GLBs were copied to `app/src/main/assets/body/{male,female}`;
the manifest SHA-256 and byte counts were updated.

## Verification

- Generator structural assertions: passed for both GLBs.
- Final local integrity verifier: technical inspection passed; it reads the
  actual GLB bytes, validates header/length, JSON and BIN chunks, buffer and
  accessor bounds, node/mesh/material/index references, node region IDs, and
  generated metrics. It does not trust `stats.json`.
- Required region coverage: 11 regions for each model.
- Current generated metrics: 1,535,812 total bytes; male 730,028 bytes / 36
  meshes / 28,928 triangles / 20 selectable meshes; female 805,784 bytes / 41
  meshes / 31,872 triangles / 22 selectable meshes.
- `./gradlew :app:testDebugUnitTest --tests '*BodyModelValidatorTest'`: passed.
- `./tools/verify-body-models.sh`: intentionally blocked (exit 8) only by the
  separate release authorization gate. Both local models are runtime-valid and
  structure-valid; license/source metadata and APK redistribution authorization
  remain unconfirmed. The supplied source has no formal external URL or
  version; this report does not claim release authorization.
