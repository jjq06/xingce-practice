# 行测随身练

一款离线运行的 Android 单选题练习应用。用户可以练习内置题目、导入符合模板的 TXT 题库，提交后查看成绩和逐题解析，并通过历史记录与错题本复习。

## 当前功能

- **题库**：首次建库时导入三类共 30 道自编示例题；首页显示总题量及各分类题量。题目和练习数据保存在本机 SQLite 中。
- **导入**：通过系统文件选择器读取 UTF-8、Tab 分隔的 TXT 文件。逐行校验格式，报告错误行；合法行继续导入，重复题按指纹跳过。
- **练习**：支持综合练习、按分类练习和错题重练；可切换题目、保留已选答案、查看进度与计时。未答完提交时会提醒。
- **结果与复习**：提交后保存练习记录、逐题答案和错题；结果页显示得分、正确率、用时及解析。历史页可按记录 ID 回看旧结果；错题答对后会从错题本移除。
- **设置**：默认每次练习 10 题，可修改为正整数并保存在 `SharedPreferences`。普通练习实际最多取 100 题；题库不足时只使用已有题目。错题重练读取当前错题列表，不受该题数设置限制。

应用不需要账号或网络服务。首版只支持 A～D 四选一单选题，不支持 PDF、Word 或图片识题。

## 运行与构建

1. 用 Android Studio 打开**本仓库根目录**（含 `settings.gradle.kts` 的目录），安装项目所需的 Android SDK 和 Gradle 工具链。
2. 连接 Android 设备或启动模拟器，运行 `app` 配置。应用最低支持 API 26，目标 API 为 35。
3. 也可在仓库根目录执行：

   ```powershell
   .\gradlew.bat :app:assembleDebug
   .\gradlew.bat :app:testDebugUnitTest
   ```

   调试 APK 输出在 `app/build/outputs/apk/debug/`。如 Windows 中文目录触发 Android 路径检查，可在构建命令后附加 `"-Pandroid.overridePathCheck=true"`。

仓库内的 `app/xingce-practice-main/` 是成员 B 合并时带入的**嵌套工程副本**；当前实际构建入口是根目录，练习和结果页的代码已接入根工程。不要在嵌套工程中修改正式应用。

## 快速体验

首次启动后，首页应显示三类示例题。点击“综合练习”或某个分类开始作答；提交后查看成绩与解析，再从“历史记录”打开旧结果。答错或未答的题目会出现在“错题本”，可点击“重练错题”。“设置”可调整下一次练习的题数。

要导入自己的题目，在首页选择“导入题目”，选取符合 [题库格式说明](docs/question-format.md) 的 UTF-8 TXT 文件。可直接用[原创 30 题题库](docs/行测练习题库_原创30题.txt)体验导入；它与内置示例题不同。也可从 [导入模板](docs/import-template.txt) 开始编辑；[错误示例](docs/import-errors-example.txt) 可用于检查行号提示。文件每行一题，八列顺序为：

```text
分类<Tab>题干<Tab>选项A<Tab>选项B<Tab>选项C<Tab>选项D<Tab>答案<Tab>解析
```

分隔符必须是真正的 Tab；答案只能是大写 `A`、`B`、`C` 或 `D`。题目数据应使用自编内容或取得使用许可的内容。

## 项目结构与协作

| 位置 | 内容 |
| --- | --- |
| `app/src/main/java/com/example/myapplication/model/` | 题目、练习记录、逐题答案模型 |
| `data/` | SQLite、题库导入及 Repository |
| `practice/` | 练习会话和计分逻辑 |
| `MainActivity.java`、`ui/` | 首页、导入、练习、结果、错题本、历史和设置页面 |
| `app/src/main/res/layout/`、`res/values/` | XML 布局和公共资源 |
| `docs/` | 题库格式、[模块接口约定](docs/interfaces.md)及[成员 A 交接说明](docs/member-a-handoff.md) |

页面通过 Repository 读写数据，跨页面使用 `AppContract` 中的 Intent 键传类别、练习模式或记录 ID。修改公共模型、数据库结构或接口前，请先同步更新接口文档并与其他模块负责人协调。

## 验证状态与待办

在当前根工程中，`:app:assembleDebug` 和 `:app:testDebugUnitTest` 已通过。成员 A 的数据层与导入页另有[测试记录](docs/member-a-test-report.md)。仍需在真机上验证完整流程，重点检查首次安装、重复启动、导入错误、未答提交、重复提交、错题重练、重启后历史回看和飞行模式运行，并保留截图或录屏。当前 README 不将这些未执行的真机测试视为已验收。
