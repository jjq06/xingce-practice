# 行测随身练：模块接口约定（v1）

本文件是数据导入、练习、首页与记录页面的共同开发约定。包名暂定 `com.example.myapplication`，改包名须三方同步修改 `AppContract`、Manifest 和调用方。公共签名或字段变更先更新本文件，再同步代码。

## 分层与责任

| 层 | 路径 | 责任 |
| --- | --- | --- |
| 页面 | `MainActivity`、`ui/*Activity` | 渲染、用户操作、Intent 跳转；不写 SQL |
| 数据 | `data/QuestionRepository`、`PracticeRepository` | 查询与写入 SQLite；对页面只返回模型或 ID |
| 导入 | `data/QuestionImporter` | 解析 UTF-8 TXT、逐行校验、计算去重指纹 |
| 业务 | `practice/PracticeSession`、`ScoreCalculator` | 当前作答状态、正确数与分数 |
| 存储 | `data/AppDatabaseHelper` | 表结构与数据库版本 |

依赖方向：页面 → Repository／业务类 → `AppDatabaseHelper`。跨页面只传类别、模式、记录 ID；题目和答案从 Repository 查询，不把整个对象放进 Intent。

## 模型与存储字段

| Java 模型 | 字段 | 约定 |
| --- | --- | --- |
| `Question` | `id`, `category`, `stem`, `optionA`～`optionD`, `answer`, `explanation`, `source`, `fingerprint` | `id` 为 SQLite 正整数主键；`answer` 为大写 A～D；解析可为空；指纹由分类、题干、四选项生成 |
| `PracticeRecord` | `id`, `category`, `startedAt`, `submittedAt`, `questionCount`, `correctCount`, `score`, `durationSeconds` | 时间为 Unix 毫秒；用时为秒；每题 10 分；`category` 为本次练习类别，综合练习使用 `全部` |
| `AnswerDetail` | `id`, `recordId`, `questionId`, `userAnswer`, `correct` | 未答题的 `userAnswer == null`；`correct == false`；每道已提交题均保存一条 |

数据库表名与列名固定为 `questions`、`practice_records`、`answer_details`、`wrong_questions`，列定义见 `AppDatabaseHelper.onCreate()`。数据库版本当前为 1。更改表结构必须提高版本并提供迁移，不得直接清库。

## Repository 和导入签名

以下签名以代码为准；页面不得自行拼 SQL。

```java
new QuestionRepository(context).count();
new QuestionRepository(context).categories();
new QuestionRepository(context).findByCategory(category, limit);
new QuestionRepository(context).findById(questionId);
new QuestionRepository(context).insertAll(questions);

new QuestionImporter().parse(reader, source); // ParseResult.questions / errors

new PracticeRepository(context).save(record, answers); // 返回 recordId
new PracticeRepository(context).findById(recordId);
new PracticeRepository(context).findAnswers(recordId);
new PracticeRepository(context).history();
new PracticeRepository(context).wrongQuestionIds();
```

`category == null` 表示全部分类；`findByCategory` 随机取最多 `limit` 题，`limit` 必须大于零，题库不足时返回已有题目。`findById` 查询不到返回 `null`。`insertAll` 返回实际新增数，重复指纹跳过。`parse` 返回合法题与含行号的错误文本；合法行即使文件其他行出错也保留。`Reader` 由 `parse` 关闭。

`PracticeRepository` 的五个方法目前是**已定签名、待实现功能**，会抛出 `UnsupportedOperationException`，调用方应在对应功能完成后接入。实现时 `save` 必须在一个事务内写入记录、每题答案并更新错题本；提交一次只产生一条记录。`findById` 查不到返回 `null`；`findAnswers`、`history`、`wrongQuestionIds` 无数据时返回空列表。`history` 按提交时间降序。错题本的同一题仅存一条，答对后移除。

## 页面参数与设置

所有键使用 `AppContract`，不要在 Activity 中重复硬编码。

| 目标页面 | 参数 | 类型与含义 |
| --- | --- | --- |
| `PracticeActivity` | `EXTRA_CATEGORY` | 可选 `String`；缺省 `null` 代表全部分类 |
| `PracticeActivity` | `EXTRA_PRACTICE_MODE` | 必填 `String`；`MODE_NORMAL` 或 `MODE_WRONG_BOOK`。练习页接入时须校验缺失或非法值 |
| `ResultActivity` | `EXTRA_RECORD_ID` | 必填 `long`；提交后获得的记录 ID；历史页也用它打开旧结果 |

错题本进入练习时传 `MODE_WRONG_BOOK`，普通入口传 `MODE_NORMAL`。`ResultActivity.EXTRA_RECORD_ID` 是向后兼容别名，新代码使用 `AppContract.EXTRA_RECORD_ID`。

设置用 `getSharedPreferences(AppContract.PREFS_NAME, MODE_PRIVATE)`；默认题数键为 `PREF_DEFAULT_QUESTION_COUNT`，默认值 10。写入前校验为正整数。当前仅定义题数设置；新增设置须先在 `AppContract` 增加键与默认值。

## TXT 题库

固定八列、Tab 分隔、UTF-8 编码，详细格式及示例见 [question-format.md](question-format.md)。首版仅支持单选 A～D。题库导入模块负责文件选择、来源标记、错误行报告和去重；练习模块不读取文件。

## 模块交接

- 数据导入模块维护 `model/`、`data/`、导入页和示例题，向另外两组提供模型及 Repository。
- 练习模块维护 `practice/`、练习页、结果页；从 Repository 获取题目，提交后只把记录 ID 传结果页。
- 首页与记录模块维护首页、错题本、历史、设置；通过 Repository 查询，不依赖练习页内部状态。

修改公共模型字段、Repository 签名、Intent 键或数据库版本时，同一提交更新本文件，并通知其他模块负责人。
