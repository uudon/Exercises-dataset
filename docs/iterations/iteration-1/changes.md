# Iteration 1 变更记录

状态：进行中，未达到一期验收。

## 已完成的可追踪变更

- 固定上游 commit `7455efae41b330c265e7cd4b78dfa848e7ce5ebd`。
- 将 1324 张 JPG 和 1324 个 GIF 放入 APK assets；一期家庭目录仍由 overlay 白名单控制。
- 增加动作 ID 到本地缩略图/GIF 的 `MediaResolver` 映射。
- 列表使用本地缩略图，详情使用本地 GIF；列表不加载 GIF，详情离开/后台时停止并释放 Coil loader。
- 增加 `verifyMediaResources` Gradle 任务和实际资源报告。
- 提交：`751715a feat: add offline local exercise media`。

## 未完成

- 飞行模式首次启动和真实设备媒体生命周期验证。
- 运行时加载速度和内存测量。
- Gym visual APK 分发授权确认。
- lint、仪器测试和一期完整验收报告。
