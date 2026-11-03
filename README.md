# 行测随身练

根据《行测随身练_完整企划案》建立的 Android Java 项目框架。

## 当前内容

- Java + XML，多 Activity 页面入口（首页、导入、练习、结果、错题本、历史、设置）。
- `model/` 数据对象、`data/` SQLite 表与 Repository 边界、`practice/` 会话与计分规则。
- UTF-8 TXT 题库解析与系统文件选择导入。导入会报告错误行并跳过重复题。
- 成员 A 已完成三类各 10 道自编示例题的自动入库、TXT 校验与去重、后台导入页，以及记录和错题的事务持久化。
- 成员 C 已接入首页题量与分类入口、历史记录列表、错题列表和默认题数设置；列表查询在后台线程执行，返回页面后会刷新。
- 数据模块的接口、模板与验证说明见 [成员 A 交接说明](docs/member-a-handoff.md)。

## 尚待完成

成员 B 的练习作答、提交和结果解析已接入根工程，成员 C 的错题重练和历史回看入口也已传入约定的模式或记录 ID。仍需完成真机联调、飞行模式测试、截图和彩排，不要将当前版本当作已交付的完整首版。

## 构建

用 Android Studio 打开工程，选择已安装的 Android SDK 后运行 `gradlew.bat :app:assembleDebug`。当前 Windows 中文目录需要增加参数 `"-Pandroid.overridePathCheck=true"`；Gradle 单元测试如出现测试类加载失败，请将工程放到英文路径再运行，详见成员 A 测试记录。

题库文件格式见 [docs/question-format.md](docs/question-format.md)。

团队协作的公共模型、Repository、Intent 和设置键约定见 [docs/interfaces.md](docs/interfaces.md)。
