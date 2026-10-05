# Iteration 2 测试报告

日期：2026-10-05。状态：核心验证通过，未宣称二期所有手工边界验收通过。

交付 commit：`9335992 fix: complete iteration todo verification`。

| 检查项 | 实际结果 | 状态 | 证据 |
| --- | --- | --- | --- |
| TDD 首次失败测试 | `SetValidatorTest` 首次运行因模型/校验器尚不存在而编译失败，符合 RED 阶段 | 通过（过程证据） | 终端 Gradle 输出 |
| `./gradlew :app:testDebugUnitTest` | 单元测试通过，包含边界校验 | 通过 | Gradle 输出；`app/src/test/` |
| `./gradlew :app:assembleDebug` | Debug APK 构建成功 | 通过 | Gradle 输出 |
| `./gradlew :app:lintDebug` | Lint 成功并生成报告 | 通过 | `app/build/reports/lint-results-debug.html` |
| `./gradlew :app:assembleDebugAndroidTest` | AndroidTest APK 构建成功 | 通过 | Gradle 输出 |
| `adb shell am instrument -w com.don.homefitness.test/androidx.test.runner.AndroidJUnitRunner` | emulator-5554 上 5 tests 全部通过，包含迁移、目录、计划仓储和首页 | 通过（单模拟器） | adb 输出 |
| `./gradlew :app:connectedDebugAndroidTest` | 两个连接设备中的物理设备在安装 APK/卸载测试 APK 阶段失败；未作为总通过依据 | 未完成（环境阻塞） | Gradle 输出：`ShellCommandUnresponsiveException` |
| v1→v2 收藏保留专项迁移 | emulator-5554 通过 | 通过（单模拟器） | `DatabaseMigrationTest` adb 输出 |
| Android Studio/模拟器手工计划流程 | 模拟器完成创建、保存、复制、编辑、未保存确认，以及时长 30 秒、单只哑铃 12.50 kg 重开 | 通过（单模拟器） | `/tmp/homefitness-plan-saved.png`、`/tmp/homefitness-plan-copy.png`、`/tmp/homefitness-unsaved-dialog.png`、`/tmp/homefitness-duration-weight-reopen.png` |
