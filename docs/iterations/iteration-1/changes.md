# Iteration 1 变更记录

状态：进行中，授权与升级回归未完成，未达到一期发布验收。

## 已完成的可追踪变更

- 固定上游 commit `7455efae41b330c265e7cd4b78dfa848e7ce5ebd`。
- 将 1324 张 JPG 和 1324 个 GIF 放入 APK assets；一期家庭目录仍由 overlay 白名单控制。
- 增加动作 ID 到本地缩略图/GIF 的 `MediaResolver` 映射。
- 列表使用本地缩略图，详情使用本地 GIF；列表不加载 GIF，详情离开/后台时停止并释放 Coil loader。
- 增加 `verifyMediaResources` Gradle 任务和实际资源报告。
- 增加 `verifyOfflineContract` 离线契约检查；生产代码仅通过 APK assets 读取媒体。
- 在 API 36 模拟器飞行模式下完成启动、缩略图、GIF、后台 pause/release 和性能样本验证。
- TODO 验证与报告更新提交：`9335992 fix: complete iteration todo verification`。
- 提交：`751715a feat: add offline local exercise media`。

## 未完成

- 真实物理设备媒体生命周期验证；模拟器证据已完成。
- 多设备性能基线和详情切换耗时。
- Gym visual APK 分发授权确认。
- 新版 APK 覆盖安装对计划、收藏、训练记录和历史的完整升级回归。
- 损坏图片/GIF 候选包的安装运行时异常处理验证。
