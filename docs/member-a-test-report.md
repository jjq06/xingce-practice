# 成员 A 测试记录

日期：2026-10-30。代码范围：题库与本地数据、导入页面。测试没有代替成员 B/C 的完整业务联调。

## 已通过

| 检查 | 结果 |
| --- | --- |
| 当前中文工作目录构建应用 APK、Android 测试 APK | 成功 |
| QuestionImporterTest | 7 项通过 |
| 原有 ExampleUnitTest | 1 项通过 |
| DataRepositoryTest | 8 项通过 |
| ImportActivityTest | 3 项通过 |
| 原有 ExampleInstrumentedTest | 1 项通过 |
| git diff --check | 通过 |

本地单元测试共 8 项通过，其中新增 7 项。Android 测试共 12 项通过，其中新增 11 项。Android 测试设备为本次启动的 Pixel_10 模拟器，系统报告 Android 17 / API 37。编译仍使用原工程声明的 compileSdk 36.1、targetSdk 35、minSdk 26，没有升级项目依赖。

解析测试覆盖：UTF-8 中文、BOM、CRLF、空行物理行号、缺列、多列、具体空字段、非法答案、多选答案、答案大小写、空解析、重复指纹、流关闭、非法 UTF-8。

数据库测试覆盖：初装 30 题和每类题量、重复打开不重复导入、部分合法文件导入与文件内去重、未知分类/ID、提交汇总回算、未答保存、同一记录重复提交、答案顺序、错题计数及答对移除、重开数据库后的题目/记录/错题保留、非法提交不写入、触发器模拟失败后的事务回滚、v1 → v2 保留原有题目和记录、历史排序。

导入界面测试覆盖：后台导入、旋转恢复、结果文本、按钮恢复可用、新增/重复/错误行统计、重复导入、错误编码不写库、扩展名拒绝、空文件。测试文件由测试代码写入应用缓存，通过文件 URI 直接送入导入 ViewModel；没有假称已经手动走通系统文件选择器。

## 复现命令

先配置 Android SDK 和兼容的 JDK。项目的 Gradle daemon 使用 Java 21；本次 SDK 位于本机环境变量中，未提交 local.properties 或个人 SDK 路径。

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest "-Pandroid.overridePathCheck=true" --offline --no-configuration-cache --console=plain
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`，测试包输出：`app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`。首次缺依赖时去掉 `--offline`。

单元测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --no-configuration-cache --console=plain
```

Windows 当前中文目录下，AGP 首先要求允许路径检查参数。绕过检查后 APK 可构建，但 Gradle 单元测试报告测试类 ClassNotFoundException。本次把相同源代码、Gradle/AGP 版本与依赖放到英文临时目录，以上 8 项测试全部通过；另用 javac/JUnit 直接运行新增解析测试，7 项全部通过。这是本地测试路径限制，不是测试断言失败。建议成员 C 将正式构建目录设为英文路径。

设备测试正常可使用 `:app:connectedDebugAndroidTest`。本机该任务在准备额外运行器依赖时耗时较长，因此本次用已构建测试 APK 直接执行相同 AndroidJUnitRunner：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.example.myapplication.test/androidx.test.runner.AndroidJUnitRunner
```

输出为 `OK (12 tests)`。数据库测试使用随机命名的独立库；导入页面测试只清理测试自己创建的唯一类别和临时文件，不清空用户历史。

## 后续集成验收

成员 C 应在 Android 15 真机完成系统文件选择器选取/取消、文件管理器兼容、状态栏/导航栏视觉检查和飞行模式验收；本次没有 Android 15 真机可用。成员 B/C 页面仍为原框架，完整“导入—答题—结果—错题重练”演示待其接入。当前调试 APK 用于数据与导入模块验证，不是完整首版 APK。
