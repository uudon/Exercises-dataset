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
- Independent structural check: passed; GLB header/length, buffer bounds, all
  11 region IDs, and explicit biceps/triceps/quadriceps/hamstrings nodes were
  verified.
- `./gradlew :app:testDebugUnitTest --tests '*BodyModelValidatorTest'`: passed.
- `./tools/verify-body-models.sh`: intentionally blocked (exit 8). Both local
  models report unconfirmed license/source metadata and APK redistribution
  authorization. The supplied source has no formal external URL or version;
  this report does not claim release authorization.
