# Iteration 1 测试报告

日期：2026-10-05。状态：部分执行，未宣称一期通过。

| 用例/命令 | 实际结果 | 状态 | 证据 |
| --- | --- | --- | --- |
| `./gradlew :app:verifyMediaResources` | 1324 条映射，图片/GIF 缺失为 0，逐文件 SHA-256 和大小校验通过 | 通过（资源校验范围） | `app/src/main/assets/catalog/media-check-report.json` |
| `./gradlew :app:testDebugUnitTest` | 单元测试通过，包含媒体 manifest 和生命周期控制器测试 | 通过（单元测试范围） | Gradle 输出；`app/src/test/` |
| `./gradlew :app:assembleDebug` | Debug APK 构建成功，161830068 字节 | 通过（构建范围） | `docs/iterations/iteration-1/media-resource-report.json` |
| 飞行模式首次启动 | 尚未执行 | 未执行 | 无 |
| 真实设备 GIF 后台停止/内存释放 | 尚未执行 | 未执行 | 无 |
| 加载耗时/运行内存 | 尚未执行 | 未执行 | 无 |
| APK 媒体分发授权 | 尚未确认 | 阻塞 | `app/src/main/assets/catalog/media-authorization-status.json` |
