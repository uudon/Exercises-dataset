# Iteration 2 变更报告

日期：2026-10-05。状态：实现进行中，未宣称迭代验收通过。

## 已实现

- 新增 Room v2 计划、计划动作、训练组表和 1→2 非 destructive migration。
- 新增计划名称、动作排序、动作增删、训练组增删、次数/时长模式、单只哑铃重量、组间休息校验。
- 新增计划编辑、复制、删除和本地观察列表；重复动作条目使用独立主键。
- 新增 SetValidator 单元测试和 PlanRepository 设备测试。

## 未完成或待验证

- 设备测试在 APK 安装阶段被 `ShellCommandUnresponsiveException` 和测试 APK 删除内部错误阻塞，未进入断言阶段。
- 未在真实设备或 Android Studio 中完成手工创建、重开、编辑、复制流程证据。
- v1→v2 实际迁移专项测试尚未执行；当前仅完成迁移 SQL 与 Room schema 实现。
- 未保存退出的“保存/放弃”二次确认尚未实现。
