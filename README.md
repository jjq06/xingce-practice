# 行测随身练

根据《行测随身练_完整企划案》建立的 Android Java 项目框架。

## 当前内容

- Java + XML，多 Activity 页面入口（首页、导入、练习、结果、错题本、历史、设置）。
- `model/` 数据对象、`data/` SQLite 表与 Repository 边界、`practice/` 会话与计分规则。
- UTF-8 TXT 题库解析与系统文件选择导入。导入会报告错误行并跳过重复题。
- `assets/sample_questions.txt` 是自编格式演示题，尚未作为完整练习题库自动加载。

## 尚待完成

练习作答和提交、记录事务保存、结果解析、错题本、历史记录及设置的实际功能。页面目前会明确显示占位文案。不要将当前框架当作已交付的完整首版。

## 构建

用 Android Studio 打开工程，选择已安装的 Android SDK 后运行 `gradlew.bat :app:assembleDebug`。

题库文件格式见 [docs/question-format.md](docs/question-format.md)。

团队协作的公共模型、Repository、Intent 和设置键约定见 [docs/interfaces.md](docs/interfaces.md)。
