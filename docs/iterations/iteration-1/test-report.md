# Iteration 1 测试报告

日期：2026-10-05。状态：部分执行，授权与升级回归仍未宣称一期通过。

| 用例/命令 | 实际结果 | 状态 | 证据 |
| --- | --- | --- | --- |
| `./gradlew :app:verifyMediaResources` | 1324 条映射，图片/GIF 缺失为 0，逐文件 SHA-256 和大小校验通过 | 通过（资源校验范围） | `app/src/main/assets/catalog/media-check-report.json` |
| `./gradlew :app:testDebugUnitTest` | 单元测试通过，包含媒体 manifest 和生命周期控制器测试 | 通过（单元测试范围） | Gradle 输出；`app/src/test/` |
| `./gradlew :app:assembleDebug :app:lintDebug :app:verifyMediaResources :app:verifyOfflineContract` | 全部成功；Debug APK 161980227 字节 | 通过（命令范围） | Gradle 输出；报告 JSON |
| 飞行模式首次启动 | API 36 模拟器 `airplane_mode_on=1` 启动成功，列表缩略图和详情 GIF显示 | 通过（模拟器范围） | `/tmp/homefitness-phase-check.png`、`/tmp/homefitness-detail-check.png` |
| GIF 后台停止/资源释放 | 观察到 `gif paused` 和 `gif loader released` | 通过（模拟器日志范围） | `MediaTiming` logcat |
| 加载耗时/运行内存 | 冷启动 1111 ms；缩略图最大样本 229 ms；GIF 首帧 15 ms；PSS 列表/详情/后台 252774/142594/143494 KB | 通过（样本观测范围） | `media-resource-report.json` |
| Android 仪器测试 | 直接在 emulator-5554 运行 5 tests，全部通过 | 通过（单模拟器范围） | `adb shell am instrument` 输出 |
| APK 媒体分发授权 | 尚未确认 | 阻塞 | `app/src/main/assets/catalog/media-authorization-status.json` |
