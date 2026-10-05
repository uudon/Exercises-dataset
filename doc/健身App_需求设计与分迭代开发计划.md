# 健身 App 需求设计与分迭代开发计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建一款中文 Android 个人健身工具，一期完成家庭徒手、哑铃训练闭环，二期加入健身房器械。

**Architecture:** 单 Android 应用，Compose 展示，ViewModel 管理页面状态，Repository 组织业务，Room 持久化动作、计划和训练记录。开源数据作为可替换的动作目录，个人记录独立保存；一期离线运行，不依赖账号和后台。

**Tech Stack:** Kotlin、Jetpack Compose、Navigation、ViewModel、StateFlow、Room、Kotlin Serialization；测试使用 JUnit、Room instrumentation tests 和 Compose UI tests。

**Spec:** 本文件第 1—6 节是设计规格，第 7—11 节是执行计划，两部分一起阅读。

日期：2026-10-05。状态：供审阅的规划稿，尚未实施。用户已确定分期范围；Android 原生、本地存储和交互细节为本稿的建议决策。当前未提供 Android 工程，因此下文为新项目建议路径，不代表已检查现有代码。

## Global Constraints

- 一期仅家庭徒手、哑铃训练；健身房器械入口在二期开放。
- 一期免登录、本地运行，不加入云同步、AI 自动计划、会员、饮食和社交。
- 默认中文；动作 ID 使用 String，保留前导零；个人记录以 UUID 标识。
- 哑铃重量按单只记录，输入文案为“单只重量（kg）”；不自动乘以 2。
- 同时支持次数与时长；计划目标与实际成绩独立保存。
- 开源媒体未确认适用授权前不得随 APK 分发或从仓库在线加载。
- 本次只交付文档，不创建外部项目，不编写产品代码。

## Review Focus

1. 应用进程被系统终止：已保存的组成绩不丢失，重新打开可继续活动训练。
2. 数据源字段缺失、ID 重复或升级：导入拒绝重复 ID，缺失说明显示明确提示，个人历史不受覆盖。
3. 徒手动作实际需要单杠或训练凳：必须额外标注，不能作为无设备家庭动作展示。
4. 时间和时区变化：历史保留训练时的本地日期，休息计时不因手动改时钟出现负值。
5. 备份损坏、版本不支持或中途失败：导入前校验，失败不改变现有数据。

---

## 1. 产品定位与分期

目标用户为自己及少量希望记录训练的用户。核心价值是训练时少操作、下次能参考上次成绩。完整用户路径：查找动作 → 创建计划 → 开始训练 → 逐组记录 → 结束 → 查看历史。

一期成功标准：无需联网和登录即可完成该路径；下一次训练显示同一动作上次实际完成的组、重量、次数或时长。

| 方案 | 成本与取舍 | 结论 |
| --- | --- | --- |
| Android 本地原生 | 适合用户现有开发经验；离线可用；数据需自行备份 | 本稿推荐 |
| 跨平台应用 | 可覆盖 iOS；需增加跨平台工具和验证成本 | 有明确 iOS 需求时再比较 |
| 带账号后台 | 可自动同步；需后端、隐私和账号维护 | 一期不采用 |

二期复用一期计划和记录，加入杠铃、绳索、史密斯、固定器械等内容。现有家庭数据不迁移到另一套系统。

## 2. 开源资源接入

来源：https://github.com/hasaneyldrm/exercises-dataset

已查阅仓库 README、NOTICE：README 描述约 1,324 个动作、中文说明、肌肉及器械字段和 180×180 图片/GIF。尚未逐条核验 JSON、实际资产数量和中文质量；实施前以固定 commit 的实际数据为准，不将 README 数量写成导入成功断言。

| 资源 | 用途 | 接入规则 |
| --- | --- | --- |
| data/exercises.json | 动作目录 | 读取 ID、name、equipment、body_part、target、secondary_muscles、中文说明 |
| instruction_steps.zh | 分步详情 | 存在且有效时使用；否则展示 instructions.zh；两者均缺失时提示暂无中文说明 |
| exercises.schema.json | 数据校验参考 | 读取实际 schema 后建立导入验证 |
| images、videos | 动作示范 | 授权核实后接入；videos 中资源实际为 GIF，不按普通视频处理 |
| LICENSE、NOTICE.md | 来源与许可 | 保留许可说明、来源、版本和署名 |

数据及说明采用 MIT；图片/GIF 属于 Gym visual，仓库再分发许可不等于本 App 已取得授权。授权未确认时使用文字及通用占位图；确认后保留 © Gym visual — https://gymvisual.com/ 署名并遵守适用分辨率条款。不得用放大或 AI 重制规避授权。

许可原文：https://github.com/hasaneyldrm/exercises-dataset/blob/main/NOTICE.md

增加独立 overlay 文件补充 nameZh、aliasesZh、requiredEquipment、homeEligible、reviewStatus。禁止修改源数据来假装这些字段原本存在。家庭列表采用经过人工审核的白名单，不仅凭 equipment=body weight 判断；需要单杠、训练凳等动作在未声明具备对应设备时隐藏。先整理 30—50 个常用候选动作供人工审核，不将此数字设为训练处方。

数据版本清单记录 commit SHA、校验摘要、导入日期、导入数量、审核数量及排除原因。中文名称、说明和动作匹配需人工检查。首期不提供未经审核的预设训练课程。

## 3. 页面和交互规格

底部三项：训练、动作库、记录。设置和备份从训练页右上角进入。

| 页面 | 内容 | 关键交互 |
| --- | --- | --- |
| 训练首页 | 活动训练、计划列表 | 活动训练存在时优先显示“继续训练”；其他计划不能另开活动训练 |
| 动作库 | 名称、部位、器械、收藏 | 中文别名或英文搜索；部位和器械条件取交集；无结果可清空筛选 |
| 动作详情 | 步骤、目标肌肉、器械、来源 | 收藏；加入计划；媒体不可用时仍可读说明 |
| 计划编辑 | 名称、动作及目标组 | 通过上移/下移调整顺序；删除计划不删除历史 |
| 训练执行 | 当前动作、上次成绩、当前组 | 手动确认保存每组；保存成功后开始休息；可跳过、返回修改 |
| 训练结束 | 已完成组、训练总用时 | 有已完成组才可完成；空训练可取消；总用时不宣称等于运动时长 |
| 历史详情 | 开始时间、实际组、完成状态 | 复制为新计划；允许编辑实际成绩，明确标注已修改 |
| 设置 | 家庭设备、导出、恢复、来源许可 | 默认只有徒手和哑铃；文件操作用系统文件选择器 |

空状态给出下一步，例如“还没有计划，创建你的第一个计划”。数字输入提供单位和错误提示，不能依赖键盘限制。颜色不作为唯一状态标识；字号随系统设置缩放。

## 4. 数据模型与规则

| 数据对象 | 主要字段 | 关系与用途 |
| --- | --- | --- |
| Exercise | id:String、原始英文名、中文名、别名、部位、器械、说明、来源版本 | 动作目录；源数据和 overlay 合并读取 |
| WorkoutPlan | id:UUID、name、createdAt、updatedAt | 计划名称去空格后 1—40 字符 |
| PlanExercise | id:UUID、planId、exerciseId、position | 同一动作可在计划中重复，按独立条目记录 |
| PlannedSet | id:UUID、planExerciseId、position、mode、targetReps、targetSeconds、targetWeight、restSeconds | 每组有独立目标 |
| WorkoutSession | id:UUID、planId 可空、planNameSnapshot、status、startedAt、finishedAt、localDate、zoneId | 状态 ACTIVE、COMPLETED、CANCELLED；最多一个 ACTIVE |
| SessionExercise | id:UUID、sessionId、exerciseId、nameSnapshot、equipmentSnapshot、position | 创建训练时保存计划快照，计划之后改变不影响此训练 |
| SessionSet | id:UUID、sessionExerciseId、position、mode、targetSnapshot、actualReps、actualSeconds、actualWeight、status | 状态 PENDING、COMPLETED、SKIPPED；实际值与目标分开 |
| RestState | sessionId、bootMarker、deadlineElapsedMs、durationSeconds | 存储休息截止点；设备重启后休息标为结束 |

重量在数据库中使用整数克，界面 kg 小数最多两位；一期家庭范围 0—200 kg。次数范围 1—999；计时范围 1—7200 秒；休息范围 0—600 秒。上述是产品输入边界，不代表建议训练强度。重量为空表示未记录，0 表示明确无额外负重。REPS 必须有次数且无时长；DURATION 必须有时长且无次数。

计划至少一个动作、每动作至少一个组才能启动。启动通过数据库事务创建完整快照并检查唯一活动训练。单组保存为 upsert，重复点击不会创建两条记录。保存成功前不开始休息。完成训练统计只纳入 COMPLETED 组，跳过组不作为零成绩。

休息基于 SystemClock.elapsedRealtime 截止点计算，不靠每秒数据库写入，也不依赖页面一直存活。进程重建、同次开机可继续；设备重启后提示休息已结束。暂停训练先保存已输入且确认的数据，停止休息；继续时不恢复旧休息。历史日期使用开始时保存的 localDate 和 zoneId。

上次成绩取当前训练之前最近一条 COMPLETED session 中同 exerciseId 的完成组，不纳入当前活动训练或取消训练；重复动作按历史出现顺序显示，并标注条目。二期体重/负重/辅助重量不混算。

## 5. 架构与文件职责

建议单 app 模块，包名示例 com.don.homefitness；正式开工先检查工程 AGENTS.md、现有代码和构建配置。已有工程优先沿用现有结构，不盲目重建。

路径基准：app/src/main/java/com/don/homefitness/。单元测试对应 app/src/test/java/com/don/homefitness/，设备测试对应 app/src/androidTest/java/com/don/homefitness/。

| 建议路径 | 职责 |
| --- | --- |
| App.kt、MainActivity.kt、navigation/AppNavHost.kt | 应用初始化、三入口导航 |
| data/catalog/ExerciseDto.kt、CatalogImporter.kt、CatalogRepository.kt | 源数据解析、验证、查询 |
| data/db/FitnessDatabase.kt、entity/、dao/ | Room 结构、事务和迁移 |
| feature/catalog/CatalogScreen.kt、ExerciseDetailScreen.kt、CatalogViewModel.kt | 动作查询和详情 |
| feature/plan/PlanRepository.kt、PlanEditorScreen.kt、PlanEditorViewModel.kt | 计划持久化与编辑 |
| feature/training/TrainingRepository.kt、TrainingScreen.kt、TrainingViewModel.kt、RestTimer.kt | 训练状态、组成绩、计时 |
| feature/history/HistoryRepository.kt、HistoryScreen.kt、SessionDetailScreen.kt | 历史查询和编辑 |
| feature/backup/BackupService.kt、BackupDocument.kt、BackupScreen.kt | 文件导出、验证和恢复 |
| core/model/SetMode.kt、WeightUnit.kt、core/validation/SetValidator.kt | 公共类型及输入规则 |
| app/src/main/assets/catalog/exercises.json、exercise-overlay.json、source-manifest.json | 固定版本数据与审核补充 |

可选媒体模块仅在授权确认后增加 MediaResolver 和独立媒体包。不对整个动作列表自动播放 GIF；仅详情页当前可见媒体播放，离开后释放资源。

## 6. 构建基线和质量原则

建议 minSdk 24，其余 JDK、AGP、Gradle、Kotlin、compileSdk、targetSdk 和库版本在开工时核对官方兼容性及发布要求，写入 docs/dependency-baseline.md；当前文档不声称具体版本是最新。使用项目 Gradle Wrapper，依赖版本集中管理，不使用动态版本。

功能逻辑以有意义的测试先行；每项先看到预期断言失败，再最小实现、运行通过。纯文案、颜色和布局调整以人工/界面验证为主，不写镜像实现的单元测试。

## 7. 迭代总览

| 迭代 | 可使用成果 | 前置条件 | 粗略工作量 |
| --- | --- | --- | --- |
| 1 家庭动作库 | 离线搜索、筛选、详情、收藏 | 工程及数据审核 | 4—6 人日 |
| 2 我的计划 | 创建、修改、复制可启动的计划 | 迭代 1 | 3—5 人日 |
| 3 训练闭环 | 逐组记录、休息、暂停、恢复、结束 | 迭代 2 | 5—8 人日 |
| 4 历史和备份 | 查询、上次成绩、复制、恢复 | 迭代 3 | 4—6 人日 |
| 5 健身房目录 | 器械筛选与场景切换 | 一期验收 | 3—5 人日 |
| 6 健身房体验 | 替换动作、单位切换、趋势 | 迭代 5 | 4—6 人日 |

估算为规划值，含研发和验证，不是承诺日期；不含媒体购买等待、专业内容审核等待和上架审核。每期结束根据实际问题更新后续估算。

## 8. 一期实施任务

所有路径相对第 5 节基准；测试路径按同包对应测试源集。每个任务最后提交相关源码、测试和文档，避免 git add .；提交信息见任务末尾。提交前保留实际测试输出。

### 迭代 1 / Task 1：工程、数据导入与家庭筛选

**Files:** 创建 App.kt、MainActivity.kt、navigation/AppNavHost.kt、data/catalog/ExerciseDto.kt、CatalogImporter.kt、CatalogRepository.kt、data/db/FitnessDatabase.kt 及动作/收藏 entity 与 dao；创建三个 catalog assets；测试 CatalogImporterTest.kt、HomeEligibilityTest.kt、CatalogDaoTest.kt。

**Interfaces:** 输入固定版本 JSON、overlay、用户设备集合 Set<String>。输出 suspend fun importCatalog(json: String, overlay: String): ImportResult；fun observeExercises(query: String, bodyPart: String?, equipment: String?, availableEquipment: Set<String>): Flow<List<Exercise>>；suspend fun setFavorite(exerciseId: String, favorite: Boolean)。ImportResult 含 acceptedCount、rejectedCount、errors，重复 ID 使整次导入失败。

- [ ] 核对工程、AGENTS.md、构建基线和 Wrapper；记录数据 commit、实际字段、资源许可及家庭白名单审核结果。
- [ ] 写失败测试：保留 id="0001"；重复 ID 导入拒绝且数据库不变；中文步骤缺失退回段落；需要单杠动作在设备仅徒手/哑铃时不出现；两个筛选条件取交集。
- [ ] 运行 ./gradlew :app:testDebugUnitTest --tests '*CatalogImporterTest' --tests '*HomeEligibilityTest'，确认因预期业务缺失失败，不能把 SDK 缺失视为红灯测试成功。
- [ ] 实现上述接口及原子导入；数据库版本 1；目录首次导入成功后才标记完成，更新只替换目录，不覆盖用户收藏。
- [ ] 运行同一单测命令通过；执行 ./gradlew :app:connectedDebugAndroidTest 验证导入事务和收藏持久化。
- [ ] 实现动作列表、详情、中文搜索和收藏；无结果、无中文说明、无媒体均有可用状态。
- [ ] 设备验证离线首次启动、搜索、清空筛选、收藏重启保存、大字体详情；抽检白名单的设备标注和中文名称。
- [ ] ./gradlew :app:assembleDebug :app:lintDebug 通过；提交 feat: add offline home exercise catalog。

**验收:** 家庭列表中不存在未经审核或缺少设备的动作；动作数据版本可追溯；授权未确认时 APK 不含开源媒体。

### 迭代 2 / Task 2：计划编辑与复制

**Files:** 创建 feature/plan/PlanRepository.kt、PlanEditorScreen.kt、PlanEditorViewModel.kt；增加计划、动作条目、目标组 entity/dao；创建 core/model/SetMode.kt、core/validation/SetValidator.kt；测试 SetValidatorTest.kt、PlanRepositoryTest.kt、PlanEditorTest.kt。

**Interfaces:** 消费 Task 1 动作查询。输出 suspend fun savePlan(draft: PlanDraft): UUID；fun observePlans(): Flow<List<WorkoutPlan>>；suspend fun duplicatePlan(planId: UUID): UUID；suspend fun deletePlan(planId: UUID)。PlanDraft 为名称及有序动作条目，每条含 exerciseId 和有序 PlannedSetDraft。

- [ ] 写失败测试：REPS 拒绝空次数/同时填写时长；DURATION 拒绝空时长/同时填写次数；拒绝负重、超界和三位小数；空计划不能启动；同动作两条目独立保存。
- [ ] 运行 ./gradlew :app:testDebugUnitTest --tests '*SetValidatorTest'，确认断言失败。
- [ ] 实现输入验证及计划保存/复制/删除接口；数据库版本 2，新增表，禁止 destructive migration。
- [ ] 运行验证单测及设备端 PlanRepositoryTest：顺序保存、复制生成新 UUID、删除不误删另一计划、v1→v2 保留收藏。
- [ ] 实现计划编辑、上移/下移、添加删除动作和组；保存错误展示在对应字段；未保存退出弹出保存/放弃选择。
- [ ] 设备验证徒手次数、计时动作、哑铃重量计划均可保存并重开；完成构建/lint/报告。
- [ ] 提交 feat: add editable workout plans。

**验收:** 计划名称、动作顺序和各组目标重开不变；组目标不能污染其他动作。

### 迭代 3 / Task 3：训练事务与恢复

**Files:** 创建 feature/training/TrainingRepository.kt、TrainingScreen.kt、TrainingViewModel.kt、RestTimer.kt；新增 session、session exercise、session set、rest state entity/dao；测试 TrainingRepositoryTest.kt、RestTimerTest.kt、TrainingFlowTest.kt。

**Interfaces:** 消费 Task 2 的完整计划。输出 suspend fun startSession(planId: UUID): UUID；fun observeActiveSession(): Flow<SessionDetail?>；suspend fun saveSet(sessionSetId: UUID, result: SetResult)；suspend fun skipSet(sessionSetId: UUID)；suspend fun finishSession(sessionId: UUID)；suspend fun cancelSession(sessionId: UUID)。SetResult 含 mode、reps:Int?、seconds:Int?、weightGrams:Int?。RestTimer.remainingSeconds(nowElapsedMs: Long, bootMarker: String): Int，时钟通过接口注入。

- [ ] 写设备失败测试：重复开始只产生一个 ACTIVE；训练快照不受计划编辑/删除影响；同组重复保存只有一条；结束必须至少有一个完成组；取消记录不出现在完成统计。
- [ ] 运行 ./gradlew :app:connectedDebugAndroidTest，定位这些测试的预期失败。
- [ ] 实现训练事务和数据库 v2→v3 迁移；每次确认组后立即落盘；保存异常保留输入并提示重试。
- [ ] 写 RestTimerTest：后台 65 秒后剩余量正确、到期返回 0、改变系统墙钟不影响结果、bootMarker 改变返回 0。
- [ ] 运行 ./gradlew :app:testDebugUnitTest --tests '*RestTimerTest' 先失败，再实现计时，重跑通过。
- [ ] 实现执行页、确认组、跳过、暂停继续、取消完成；暂停清理休息；已有 ACTIVE 时开始其他计划提示继续或取消。
- [ ] 运行设备测试通过；人工测试确认组后强制停止再打开、旋转、切后台、设备重启，核对组不丢失和休息表现。
- [ ] 完成构建/lint/报告；提交 feat: add resumable workout sessions。

**验收:** 从计划到完成历史形成闭环；重启不重复启动、不重复保存；恢复的是实际已确认成绩。

### 迭代 4 / Task 4：历史、上次成绩及复制

**Files:** 创建 feature/history/HistoryRepository.kt、HistoryScreen.kt、SessionDetailScreen.kt；修改 TrainingViewModel.kt 展示上次成绩；测试 HistoryRepositoryTest.kt、HistoryFlowTest.kt。

**Interfaces:** 消费 Task 3 SessionDetail。输出 fun observeHistory(date: LocalDate?): Flow<List<SessionSummary>>；suspend fun previousPerformance(exerciseId: String, beforeStartedAt: Instant): PreviousPerformance?；suspend fun copySessionToPlan(sessionId: UUID): UUID；suspend fun updateCompletedSet(setId: UUID, result: SetResult)。PreviousPerformance 包含来源 sessionId、开始日期及有序完成组。

- [ ] 写失败测试：上次成绩排除 ACTIVE/CANCELLED 和当前训练；跳过组不成为 0；历史名称使用快照；改变时区不更改已有 localDate。
- [ ] 设备运行 HistoryRepositoryTest 确认失败；实现查询、复制和实际组编辑，修改时写 modifiedAt；历史复制仅复制完成组为新计划目标，保持原始历史不变。
- [ ] 重跑设备测试通过；实现日历/列表、详情、上次成绩卡；无历史显示暂无记录。
- [ ] 人工完成两次同动作训练，核对第二次提示来源和成绩；删除原计划后历史仍可打开；运行构建/lint并提交 feat: add workout history and previous performance。

### 迭代 4 / Task 5：备份恢复与一期验收

**Files:** 创建 feature/backup/BackupService.kt、BackupDocument.kt、BackupScreen.kt；测试 BackupValidationTest.kt、BackupRestoreTest.kt；增加 docs/backup-format.md。

**Interfaces:** suspend fun exportBackup(output: OutputStream)；suspend fun validateBackup(input: InputStream): ValidatedBackup；suspend fun restoreBackup(backup: ValidatedBackup)。备份 formatVersion=1，包含用户设置、收藏、计划、历史和必要名称快照，不包含第三方媒体和活动训练。

- [ ] 写失败测试：损坏 JSON、重复 UUID、悬空关系、未知 formatVersion 均拒绝且原数据库不变；文件导出后恢复数量及实际成绩相同。
- [ ] 运行 ./gradlew :app:testDebugUnitTest --tests '*BackupValidationTest' 确认失败；实现格式校验。
- [ ] 实现系统文件选择器导出及恢复；恢复采用替换而非合并，明确显示会覆盖的数据数量；有 ACTIVE 时阻止恢复，须先完成或取消。
- [ ] 在数据库事务中恢复，任一步失败回滚；记录已移除动作的快照但不允许由其直接启动新训练，提示选择替代动作。
- [ ] 单测和 BackupRestoreTest 设备测试通过；人工验证取消文件选择、无存储空间、打开错误文件、覆盖确认和成功恢复。
- [ ] 运行一期完整验收；输出报告与 APK；提交 feat: add transactional backup and restore。

**一期总验收:** 飞行模式从安装完成动作查询、建计划、训练、恢复、历史、备份闭环；无严重数据丢失；所有质量门通过，人工内容审核和来源信息完整。

## 9. 二期实施任务

### 迭代 5 / Task 6：健身房目录与场景

**Files:** 修改 CatalogRepository.kt、CatalogViewModel.kt、exercise-overlay.json；新增 core/model/TrainingLocation.kt、feature/settings/EquipmentSettingsScreen.kt；测试 CatalogLocationTest.kt、CatalogUpdateTest.kt。

**Interfaces:** TrainingLocation=HOME/GYM；Task 1 observeExercises 增加 location: TrainingLocation；家庭模式仍以已审核白名单和家庭可用设备筛选，健身房模式以器械类别查询。

- [ ] 锁定新的源数据版本，审核新增器械中文映射；保留同一 exerciseId，不重复创建家庭动作。
- [ ] 写失败测试：家庭模式不混入固定器械；切换健身房可查询杠铃/绳索等；目录升级不改用户记录；被移除动作保留历史快照。
- [ ] 单测先失败；实现场景过滤、增量目录更新及器械中文标签；单测和数据库升级测试通过。
- [ ] 界面增加家庭/健身房场景选择，不自动隐藏已有计划；动作不可用时提示更换，不能静默删除。
- [ ] 验证一期回归、构建/lint/报告；提交 feat: add gym exercise catalog。

**验收:** 同一套记录系统可使用家庭和健身房动作；目录升级没有历史数据丢失。

### 迭代 6 / Task 7：替换、单位与成绩趋势

**Files:** 新增 feature/plan/ExerciseReplacement.kt、core/model/WeightUnit.kt、core/model/LoadType.kt、feature/history/ExerciseTrendScreen.kt；修改计划编辑及训练记录输入；测试 ExerciseReplacementTest.kt、WeightConversionTest.kt、TrendQueryTest.kt。

**Interfaces:** suspend fun replacePlanExercise(planExerciseId: UUID, newExerciseId: String)；fun toGrams(value: BigDecimal, unit: WeightUnit): Long；fun fromGrams(grams: Long, unit: WeightUnit): BigDecimal；fun observeExerciseTrend(exerciseId: String, loadType: LoadType): Flow<List<TrendPoint>>。LoadType=EXTERNAL/BODYWEIGHT/ASSISTANCE；TrendPoint 含日期、实际值、单位和记录模式。

- [ ] 写失败测试：替换计划动作不改已开始训练；不同记录模式替换后要求重新确认目标，不能盲目继承；1 lb 转换约为 454 g；单位来回切换不改数据库原值。
- [ ] 写趋势失败测试：辅助重量、额外负重和徒手不混算；取消训练/跳过组不参与；计时和次数不画在同一数值轴。
- [ ] 运行对应单测确认失败；实现转换和替换确认；数据库内部保留整数克，界面四舍五入显示，不以显示值反复覆盖原值。
- [ ] 采用数据库 v3→v4 增加 LoadType，旧家庭记录按其原语义映射，测试迁移保留全部历史。
- [ ] 实现趋势：按同动作/同负重类型/同模式展示完成记录，明确是成绩变化，不输出未经验证的体能或热量判断。
- [ ] 运行单测、设备端趋势和迁移测试、一期回归；输出报告；提交 feat: add gym replacement and performance trends。

**验收:** 单位切换、动作替换和趋势不改变历史含义；一期家庭训练体验保持可用。

## 10. 每期研发、测试和审查流程

不在本次文档阶段启动 Agent。执行方式在开工前选择：原生执行由一个实施者逐任务完成；如选择多 Agent，再按 subagent-driven-development 分派实施者和独立审查者，避免多人同时修改共享文件。

每个迭代按以下清单推进：

- [ ] 阅读规格、当前工程规则与前一期遗留问题。
- [ ] 确认改动范围、文件、接口及迁移版本。
- [ ] 写关键失败测试并保存预期失败证据。
- [ ] 实现最小功能并运行针对性测试。
- [ ] 修复问题，复测失败项和受影响流程。
- [ ] ./gradlew :app:assembleDebug :app:lintDebug 成功。
- [ ] ./gradlew :app:testDebugUnitTest 成功。
- [ ] 有设备/模拟器时执行 ./gradlew :app:connectedDebugAndroidTest；缺少环境标记“未执行”，不得报告通过。
- [ ] 人工验证该期主流程、错误状态及恢复路径；一期验收至少覆盖 minSdk 对应系统与较新系统。
- [ ] 审查数据一致性、迁移、事务、时间、隐私、媒体授权和生命周期。
- [ ] 关闭阻塞项，交付 APK、变更总结、测试报告、审查报告及 TODO 清单。
- [ ] 更新下一期范围，提交并记录 commit SHA。

阻塞：不能构建、主流程中断、记录丢失/重复、恢复破坏数据、未经授权的媒体分发。高优先级：筛选错误、成绩含义错误、计时恢复错误。上述问题未解决则不宣布该迭代验收通过；低优先级问题可明确记录后交付。

## 11. 交付报告模板与开工条件

每期文件位于 docs/iterations/iteration-N/：

| 文件 | 必填内容 |
| --- | --- |
| changes.md | 完成内容、未完成内容、接口/数据结构变更、构建及 commit、用户可见变化 |
| test-report.md | 环境、命令、用例、实际结果、证据、未执行项、缺陷与复测 |
| review-report.md | 审查范围、问题路径、严重级别、修复结果、残余风险、结论 |
| todo.md | 未完成任务、原因、责任角色、目标迭代、是否阻塞 |

测试报告表格：用例编号 / 场景 / 步骤 / 预期 / 实际 / 通过或失败或未执行 / 证据。审查结论必须对应实际检查，不能用“无问题”代替证据。

开工前检查：

- [ ] 用户审阅本稿的设计和迭代范围，确认是否需要改动。
- [ ] 确定目标 Android 仓库/工程和执行方式。
- [ ] 核对工程版本、包名、AGENTS.md 和可运行的设备环境。
- [ ] 固定源数据版本，完成首批家庭动作人工审核。
- [ ] 确认媒体授权状态；未确认则使用无媒体方案。

建议首先执行迭代 1，不提前铺设二期页面。迭代 1 验收后再进入计划编辑；一期全部验收后才开放健身房范围。

文档自审：需求均有对应迭代；一、二期范围一致；接口名称前后一致；恢复、升级、设备筛选、时间变化及备份失败均有测试归属。本文中的任务为规划，不代表已经实现或测试通过。
