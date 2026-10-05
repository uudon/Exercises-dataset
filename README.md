# Exercises Dataset

一个面向个人使用的中文家庭健身 Android 应用，按 `doc/健身App_需求设计与分迭代开发计划.md` 逐期实施。

当前正在实现一期 Task 1：离线动作库。项目接入上游动作文本数据，但暂不打包 Gym visual 图片/GIF，详见 `app/src/main/assets/catalog/NOTICE.md`。

技术栈：

- Kotlin 2.4.20
- Android Gradle Plugin 9.4.0
- Jetpack Compose + Material 3
- Compose BOM 2026.09.00
- compileSdk / targetSdk 37
- minSdk 26
- Room 2.8.5 + Kotlin Serialization

## 打开方式

使用 Android Studio 打开此目录，等待 Gradle 同步完成后运行 `app` 配置。

## 架构起点

当前是单 Activity + Compose + ViewModel + StateFlow + Room 的一期基础实现。后续严格按开发文档进入计划、训练、历史和备份迭代。
