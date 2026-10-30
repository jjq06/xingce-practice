# 成员 A 交接说明

完成范围：`model/`、`data/`、`ImportActivity`、`ImportViewModel`、导入 XML、三类 30 道自编示例题，以及 TXT 模板、接口文档和数据层测试。Manifest 仅增加数据库初始化 Application。成员 B 的作答、计时、结果页和成员 C 的首页、历史、错题、设置页面仍由相应成员实现。

## 给成员 B

用 `QuestionRepository.findByCategory(category, limit)` 获取随机题；记录模型中设置题数和起止时间，每题创建 AnswerDetail，未答为 null。调用 `PracticeRepository.save(record, answers)` 获得记录 ID。Repository 会事务保存、校验并回填汇总值；保存成功后务必把 ID 写进会话恢复状态，重复提交传该 ID，不能重新创建 ID 为 0 的记录。结果页用 `findById` 和 `findAnswers` 读取，题目用 `QuestionRepository.findById` 读取。

## 给成员 C

首页使用 `categoryCounts()` 展示分类题量，或 `count(category)` 查询单个类别。历史用 `history()`，结果页只传记录 ID。错题入口用 `wrongQuestionIds()`，配合按 ID 查题；重练答对即从错题本移除。所有 Repository 查询返回独立模型或集合，无数据为 null 或空集合，详见 [接口约定](interfaces.md)。

## 验证入口

- 本地单元测试：`gradlew.bat :app:testDebugUnitTest`。
- 数据库集成测试：连接测试模拟器后运行 `gradlew.bat :app:connectedDebugAndroidTest`；DataRepositoryTest 使用独立随机命名数据库，不清空应用的真实练习记录。
- 手动导入：点击首页“导入题目”，选择 `docs/import-template.txt`；重复导入应显示重复跳过。错误示例第 2 行缺列、第 3 行非法答案，合法行保留。
- 初装应有三类各 10 题；重复打开保持原题 ID。重启后用户题、记录、错题仍在。
- 屏幕旋转不应重复导入；取消选择不应报导入失败。导入页适配系统状态栏和导航栏，并显示可复制的错误明细。
- 真机 Android 15 飞行模式验收由成员 C 集成后执行。

具体测试结果见 [成员 A 测试记录](member-a-test-report.md)。
