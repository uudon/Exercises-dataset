# Iteration 1 遗留 TODO

- [x] 在 Android API 36 模拟器飞行模式下完成首次启动、缩略图和详情 GIF 验证；真实物理设备未执行。
- [x] 记录冷启动、列表缩略图样本和 GIF 首帧样本耗时；详情切换耗时未单独测量。
- [x] 记录列表、详情和后台 PSS 样本，以及后台 pause/release 日志。
- [x] 执行 `lintDebug` 和可用模拟器仪器测试；全量 Gradle connected task 仍受另一台物理设备安装故障影响。
- [ ] 确认 Gym visual 媒体随 APK 分发授权并更新授权状态文件。
- [ ] 执行新版 APK 覆盖安装，证明计划、收藏、训练记录和历史不被覆盖。
