# 3D 肌肉探索首页实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有离线健身 App 中增加以本地 3D 男女人体模型为入口的肌肉探索首页，并把肌肉区域显式映射到一期家庭动作库。

**Architecture:** 使用 SceneView 作为 Compose 集成层、Filament 作为 Android 3D 渲染引擎、本地 GLB/glTF 作为模型格式。渲染器只负责模型、相机、命中和高亮；肌肉映射、动作过滤和页面状态分别由独立的资源验证器、Repository 和 ViewModel 负责。模型失败时不影响现有动作库、计划和训练记录。

**Tech Stack:** Kotlin、Jetpack Compose、ViewModel、StateFlow、SceneView、Google Filament、glTF/GLB、Room 现有数据层、JUnit、Compose UI Test、Android instrumentation、ADB/Profiler。

**Spec:** `docs/superpowers/specs/2026-10-06-3d-muscle-home-design.md`

## Global Constraints

- 所有模型、材质、纹理、区域映射和辅助资源必须随 APK 本地提供；禁止远程 URL、在线下载和 CDN 降级。
- 当前 `minSdk=26`；必须验证 API 26 基线设备和较新 Android 设备。
- 男性使用不透明运动内裤；女性使用不透明运动内衣和内裤；模型必须保持标准、健康、自然比例和健身教学风格。
- 模型必须有独立肌肉 mesh、独立点击代理或可验证的三角形命中映射；不得依赖名称模糊匹配。
- 男女模型共用 `muscleGroupId` 语义；模型切换不清除肌肉选择、场景、设备过滤和动作查询条件。
- 一期只展示家庭设备条件下的徒手、哑铃动作；健身房器械保持二期场景。
- 主要训练动作和辅助参与动作必须分组展示；动作详情继续复用现有本地图片、GIF、中文步骤和加入计划流程。
- 模型授权、APK 分发授权、资源校验、性能数据和设备测试未完成时不得标记验收通过。
- 每项产品逻辑遵循 RED → GREEN → REFACTOR；每个任务独立提交，避免把未执行的检查写成通过。

## Review Focus

- 模型只有整体网格时，命中测试不能可靠区分肌肉；Task 1 必须验证网格分区或点击代理，并覆盖失败路径。
- 旋转手势可能误触肌肉选择；Task 3 必须用位移阈值和事件消费测试固定行为。
- 男女模型节点名可能不同但逻辑区域必须一致；Task 2/3 必须测试同一 `muscleGroupId` 在两个模型上的映射。
- 模型加载或 GPU 初始化失败不能阻塞动作库；Task 4 必须测试错误状态和文字入口。
- 后台渲染资源泄漏会造成高内存；Task 3/6 必须测试 pause、dispose、返回恢复和 PSS/帧率观测。

## 文件边界

| 文件 | 职责 |
| --- | --- |
| `app/src/main/assets/body/model-manifest.json` | 男女模型路径、SHA-256、字节数、来源和授权状态 |
| `app/src/main/assets/body/muscle-regions.json` | 区域 ID、逻辑肌肉 ID、中文名称和男女 mesh 节点 |
| `app/src/main/assets/body/muscle-action-map.json` | 肌肉群到主要/辅助动作 ID 的显式映射 |
| `app/src/main/assets/body/model-check-report.json` | 模型和映射资源实际校验结果 |
| `app/src/main/assets/body/NOTICE.md` | 模型、材质、工具和渲染库署名/许可证 |
| `app/src/main/java/com/don/homefitness/data/body/BodyModelManifest.kt` | 本地模型 manifest 序列化模型 |
| `app/src/main/java/com/don/homefitness/data/body/BodyModelValidator.kt` | 模型、节点、映射和文件完整性校验 |
| `app/src/main/java/com/don/homefitness/feature/muscle/MuscleActionMapper.kt` | `muscleGroupId` 到动作 ID 的显式查询和排序 |
| `app/src/main/java/com/don/homefitness/feature/muscle/MuscleExplorerUiState.kt` | 人体首页不可变状态、相机和错误状态 |
| `app/src/main/java/com/don/homefitness/feature/muscle/MuscleModelRenderer.kt` | SceneView/Filament 的渲染边界和生命周期接口 |
| `app/src/main/java/com/don/homefitness/feature/muscle/MuscleExplorerViewModel.kt` | 性别、选择、场景、动作面板和一次性错误状态 |
| `app/src/main/java/com/don/homefitness/feature/muscle/MuscleExplorerScreen.kt` | 3D 视口、手势、文字列表和动作面板 UI |
| `app/src/main/java/com/don/homefitness/navigation/AppNavHost.kt` | 首页默认入口和动作库次级路由 |

### Task 1: 本地模型资产与渲染 Spike

**Files:**

- Create: `app/src/main/assets/body/model-manifest.json`
- Create: `app/src/main/assets/body/NOTICE.md`
- Create: `app/src/main/assets/body/male/body.glb`（仅在来源和 APK 分发授权确认后）
- Create: `app/src/main/assets/body/female/body.glb`（仅在来源和 APK 分发授权确认后）
- Create: `app/src/main/java/com/don/homefitness/data/body/BodyModelManifest.kt`
- Create: `app/src/main/java/com/don/homefitness/data/body/BodyModelValidator.kt`
- Create: `app/src/test/java/com/don/homefitness/data/body/BodyModelValidatorTest.kt`
- Modify: `app/build.gradle.kts`（仅在 Spike 资产授权通过后锁定依赖版本）
- Create: `tools/verify-body-models.sh`

**Interfaces:**

- `data class BodyModelEntry(val gender: BodyGender, val assetPath: String, val sha256: String, val bytes: Long, val licenseStatus: String)`
- `enum class BodyGender { MALE, FEMALE }`
- `class BodyModelValidator { fun validate(manifestJson: String, assetReader: (String) -> ByteArray): BodyModelCheckReport }`
- `data class BodyModelCheckReport(val valid: Boolean, val errors: List<String>, val modelCount: Int, val totalBytes: Long)`

- [ ] **Step 1: 记录资产门禁测试**：在 `BodyModelValidatorTest` 中写入缺失模型、SHA-256 不匹配、远程路径、许可证未确认和两个模型有效的测试。
- [ ] **Step 2: 运行失败测试**：运行 `./gradlew :app:testDebugUnitTest --tests '*BodyModelValidatorTest'`；预期因 manifest、validator 和模型资产尚不存在而失败，不能把依赖下载失败当作业务 RED。
- [ ] **Step 3: 核对候选模型**：记录模型来源 URL、commit、许可证、署名、网格/材质数量、节点名和 APK 分发授权；授权未确认时只保留 manifest/validator，不复制模型文件。
- [ ] **Step 4: 实现验证器**：在 `BodyModelValidator.kt` 校验仅允许 `android_asset` 相对路径、文件存在、大小、SHA-256、`MALE/FEMALE` 唯一性和 `licenseStatus=confirmed`；在 `tools/verify-body-models.sh` 输出 JSON 报告。
- [ ] **Step 5: 验证失败和通过分支**：重新运行测试；无合法模型时预期只通过失败/阻塞分支，不能生成“已通过”的资源报告。
- [ ] **Step 6: 做最小渲染 Spike**：只有资产授权确认后，锁定兼容版本并创建最小本地 GLB SceneView/Filament 页面，验证加载、旋转、缩放、恢复默认视角和节点命中。
- [ ] **Step 7: 记录真实指标**：在 API 26 和一台较新设备上记录模型加载时间、APK 增量、三角形/材质/纹理数量、帧率和 PSS；不预填目标值。
- [ ] **Step 8: Commit**：`git add` 资产 manifest、validator、Spike、测试和报告，提交 `feat: add local body model spike`；若授权未确认，则提交只包含阻塞记录和 validator 的可回滚变更。

### Task 2: 肌肉区域资源与显式动作映射

**Files:**

- Create: `app/src/main/assets/body/muscle-regions.json`
- Create: `app/src/main/assets/body/muscle-action-map.json`
- Create: `app/src/main/assets/body/model-check-report.json`
- Create: `app/src/main/java/com/don/homefitness/data/body/MuscleRegion.kt`
- Create: `app/src/main/java/com/don/homefitness/data/body/MuscleActionMapping.kt`
- Create: `app/src/main/java/com/don/homefitness/data/body/BodyMappingValidator.kt`
- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleActionMapper.kt`
- Create: `app/src/test/java/com/don/homefitness/data/body/BodyMappingValidatorTest.kt`
- Create: `app/src/test/java/com/don/homefitness/feature/muscle/MuscleActionMapperTest.kt`
- Modify: `app/src/main/java/com/don/homefitness/data/catalog/CatalogRepository.kt`

**Interfaces:**

- `data class MuscleRegion(val regionId: String, val muscleGroupId: String, val displayNameZh: String, val meshNodeIdsByGender: Map<BodyGender, Set<String>>)`
- `data class MuscleActionMapping(val muscleGroupId: String, val primaryExerciseIds: List<String>, val secondaryExerciseIds: List<String>)`
- `class MuscleActionMapper { fun actionsFor(muscleGroupId: String, location: TrainingLocation, availableEquipment: Set<String>): MuscleActionGroups }`
- `data class MuscleActionGroups(val primary: List<CatalogExercise>, val secondary: List<CatalogExercise>)`

- [ ] **Step 1: 写映射失败测试**：覆盖重复 `regionId`、未知 `muscleGroupId`、男性/女性节点缺失、未知动作 ID、target 不匹配、主要/辅助重复和合法胸部/背部映射。
- [ ] **Step 2: 运行 RED**：运行 `./gradlew :app:testDebugUnitTest --tests '*BodyMappingValidatorTest' --tests '*MuscleActionMapperTest'`；预期映射文件和实现不存在导致失败。
- [ ] **Step 3: 固定 11 个逻辑肌肉 ID**：使用 `chest`, `shoulders`, `back`, `biceps`, `triceps`, `forearms`, `abs`, `glutes`, `quadriceps`, `hamstrings`, `calves`，男女模型共享语义。
- [ ] **Step 4: 实现映射校验**：校验 region、mesh、动作 ID、`target`、`secondary_muscles` 和许可证报告；禁止运行时按中文或英文名称模糊匹配。
- [ ] **Step 5: 实现动作分组查询**：`MuscleActionMapper` 调用现有 CatalogRepository 的家庭/健身房过滤；一期只返回徒手/哑铃并保持主要动作在前。
- [ ] **Step 6: 运行 GREEN**：重新运行两个测试类和 `./gradlew :app:verifyMediaResources :app:verifyOfflineContract`；映射缺失时预期失败，合法资源时通过。
- [ ] **Step 7: Commit**：提交 `feat: add explicit muscle action mappings`。

### Task 3: 渲染器生命周期与手势意图

**Files:**

- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleExplorerUiState.kt`
- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleModelRenderer.kt`
- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleGestureIntent.kt`
- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleExplorerViewModel.kt`
- Create: `app/src/test/java/com/don/homefitness/feature/muscle/MuscleGestureIntentTest.kt`
- Create: `app/src/test/java/com/don/homefitness/feature/muscle/MuscleExplorerViewModelTest.kt`
- Create: `app/src/test/java/com/don/homefitness/feature/muscle/MuscleModelLifecycleTest.kt`

**Interfaces:**

- `data class CameraOrbit(val azimuth: Float, val elevation: Float, val distance: Float)`
- `data class MuscleExplorerUiState(val gender: BodyGender, val selectedMuscleGroupId: String?, val camera: CameraOrbit, val scene: TrainingLocation, val isModelReady: Boolean, val errorMessage: String?, val panelExpanded: Boolean)`
- `enum class GestureIntent { TAP, ROTATE, SCALE }`
- `fun classifyGesture(downX: Float, downY: Float, upX: Float, upY: Float, scaleDelta: Float, tapSlopPx: Float): GestureIntent`
- `interface MuscleModelRenderer { fun load(gender: BodyGender); fun setHighlight(muscleGroupId: String?); fun resetCamera(); fun onResume(); fun onPause(); fun dispose() }`

- [ ] **Step 1: 写手势 RED 测试**：拖动超过 `tapSlopPx` 必须为 `ROTATE`，双指变化为 `SCALE`，短点击为 `TAP`；旋转不能产生区域选择回调。
- [ ] **Step 2: 写状态/lifecycle RED 测试**：切换性别保留 `selectedMuscleGroupId`、场景和筛选；`ON_PAUSE` 调用 `onPause`，`ON_STOP` 调用 `dispose`，返回恢复相机和选择。
- [ ] **Step 3: 运行 RED**：运行 `./gradlew :app:testDebugUnitTest --tests '*MuscleGestureIntentTest' --tests '*MuscleExplorerViewModelTest' --tests '*MuscleModelLifecycleTest'`，预期失败。
- [ ] **Step 4: 实现纯手势分类和 UiState**：不在渲染器里做业务筛选；使用不可变 `StateFlow`，把一次性加载错误作为 effect/snackbar 或明确状态字段。
- [ ] **Step 5: 实现渲染器边界**：把 SceneView/Filament 对象封装在 `MuscleModelRenderer`；渲染层只返回命中的 `regionId` 和相机变化，不直接访问 Room。
- [ ] **Step 6: 实现生命周期**：后台停止渲染循环并释放帧资源；离开页面销毁节点和纹理；返回后按保存的性别、相机和逻辑肌肉 ID重新加载。
- [ ] **Step 7: 运行 GREEN**：重新运行单测；再执行 `./gradlew :app:assembleDebug :app:lintDebug`。
- [ ] **Step 8: Commit**：提交 `feat: add muscle renderer lifecycle`。

### Task 4: 3D 人体探索首页与导航

**Files:**

- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleExplorerScreen.kt`
- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleTextList.kt`
- Create: `app/src/androidTest/java/com/don/homefitness/MuscleExplorerScreenTest.kt`
- Modify: `app/src/main/java/com/don/homefitness/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/don/homefitness/MainActivity.kt`（仅必要的依赖传递）
- Modify: `app/src/main/java/com/don/homefitness/feature/catalog/CatalogScreen.kt`（保留为次级入口）

**Interfaces:**

- `@Composable fun MuscleExplorerScreen(viewModel: MuscleExplorerViewModel, onOpenCatalog: () -> Unit, onOpenExercise: (String) -> Unit)`
- `@Composable fun MuscleTextList(regions: List<MuscleRegion>, selectedId: String?, onSelect: (String) -> Unit)`

- [ ] **Step 1: 写 Compose/设备失败测试**：默认首页可见“3D 人体探索”；动作库入口可点击；文字肌肉列表选择区域；男女切换保持选择；模型错误显示文字入口。
- [ ] **Step 2: 运行 RED**：执行 `./gradlew :app:assembleDebugAndroidTest` 和指定 `MuscleExplorerScreenTest`；预期因路由和页面不存在失败。
- [ ] **Step 3: 实现首页布局**：顶部性别切换、恢复视角、动作库入口；中部 3D 视口；底部中文肌肉列表和错误兜底；不把动作库从工程中删除。
- [ ] **Step 4: 接入导航**：将 `muscle` 设为 `startDestination`，保留 `catalog` 次级路由；保持计划、训练、历史路由不变。
- [ ] **Step 5: 实现模型/文字入口切换**：模型初始化失败时不循环重试，显示文字列表；点击区域只改变逻辑肌肉选择，不直接绕过 ViewModel。
- [ ] **Step 6: 运行 GREEN**：执行 Compose/UI 测试、`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`。
- [ ] **Step 7: Commit**：提交 `feat: make 3d muscle explorer the home screen`。

### Task 5: 动作面板与现有详情/计划链路

**Files:**

- Create: `app/src/main/java/com/don/homefitness/feature/muscle/MuscleExercisePanel.kt`
- Create: `app/src/test/java/com/don/homefitness/feature/muscle/MuscleExercisePanelStateTest.kt`
- Modify: `app/src/main/java/com/don/homefitness/feature/catalog/CatalogScreen.kt` 或抽取公开的详情路由/组件
- Modify: `app/src/main/java/com/don/homefitness/feature/plan/PlanScreen.kt`（仅接入加入计划回调）
- Modify: `app/src/main/java/com/don/homefitness/navigation/AppNavHost.kt`

**Interfaces:**

- `data class MusclePanelState(val muscleGroupId: String, val displayNameZh: String, val primary: List<CatalogExercise>, val secondary: List<CatalogExercise>, val emptyReason: String?)`
- `@Composable fun MuscleExercisePanel(state: MusclePanelState, onExerciseClick: (String) -> Unit, onOpenCatalog: () -> Unit)`

- [ ] **Step 1: 写映射/筛选测试**：家庭场景不返回固定器械；主要动作先于辅助动作；空结果显示明确下一步；动作 ID保持原 CatalogRepository 记录。
- [ ] **Step 2: 运行 RED**：执行对应测试和 `./gradlew :app:testDebugUnitTest --tests '*MuscleExercisePanelStateTest'`。
- [ ] **Step 3: 实现面板**：显示肌肉中文名称、主要动作、辅助动作标签和空状态；不自动播放 GIF。
- [ ] **Step 4: 接入详情**：点击动作使用现有本地详情逻辑，继续通过 MediaResolver 按动作 ID取 JPG/GIF和中文步骤；不添加网络降级。
- [ ] **Step 5: 接入计划**：从详情/面板进入加入计划路径，不能复制另一套动作或训练目标数据。
- [ ] **Step 6: 运行 GREEN**：执行单测、Compose 测试、媒体校验和离线契约扫描。
- [ ] **Step 7: Commit**：提交 `feat: connect muscle actions to offline catalog`。

### Task 6: 离线、生命周期、性能和发布验收

**Files:**

- Modify: `tools/verify-offline-contract.sh`
- Modify: `tools/verify-body-models.sh`
- Modify: `app/src/main/assets/body/model-check-report.json`
- Create: `docs/iterations/iteration-7/changes.md`
- Create: `docs/iterations/iteration-7/test-report.md`
- Create: `docs/iterations/iteration-7/review-report.md`
- Create: `docs/iterations/iteration-7/todo.md`
- Modify: `doc/健身App开发文档/03_TODO.md`
- Modify: `doc/健身App开发文档/05_测试用例.md`
- Modify: `doc/健身App开发文档/06_代码审查清单.md`
- Modify: `doc/健身App开发文档/07_迭代验收标准.md`

- [ ] **Step 1: 写发布验收测试清单**：覆盖首次飞行模式、男女切换、正/侧/背面命中、旋转不误触、映射正确、详情本地媒体、后台释放、错误兜底和低性能设备。
- [ ] **Step 2: 执行资源验证**：运行 `./gradlew :app:verifyBodyModels :app:verifyMediaResources :app:verifyOfflineContract`；模型缺失/损坏/授权未确认时预期阻塞。
- [ ] **Step 3: 执行构建和测试**：运行 `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`；保存完整输出和 APK SHA-256。
- [ ] **Step 4: 执行设备测试**：在 API 26 基线、较新设备和低性能设备上运行 `./gradlew :app:connectedDebugAndroidTest`；记录设备、帧率、PSS、加载时间和失败项。
- [ ] **Step 5: 执行人工生命周期测试**：强制后台、离开首页、返回、旋转和进程重建，确认停止渲染、释放资源和恢复视角/选择。
- [ ] **Step 6: 更新报告**：把实际模型数量、字节数、APK 增量、帧率、内存、授权和未执行项写入 iteration-7 报告；不把目标值或理论兼容性写成结果。
- [ ] **Step 7: 独立代码审查**：检查第三方许可证、远程访问、输入边界、模型资源释放、数据库/计划兼容性和一期回归。
- [ ] **Step 8: Commit**：在所有可执行质量门通过后提交 `docs: record 3d muscle explorer verification`；授权或设备测试阻塞时只能提交阻塞报告。

## Execution Order

严格按 Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 执行。Task 1 的模型授权和网格分区是硬门槛；未通过时只允许完成验证器、文字入口和阻塞报告，不允许把占位模型或二维图片提交为产品实现。

## Self-Review Checklist

- [x] 规格中的男女模型、服装风格、旋转/缩放/前侧背面、区域点击和文字兜底均有任务。
- [x] 规格中的本地离线、显式映射、主要/辅助动作、家庭/健身房分期均有任务。
- [x] 规格中的许可证、模型分区、包体、帧率、PSS、后台释放和 API 26 均有任务或验收门槛。
- [x] 现有动作库、详情、计划、训练、历史和备份边界没有被替换，只通过路由和回调接入。
- [x] 未执行的设备、授权和性能检查在计划中保持未执行，不提前标记通过。
