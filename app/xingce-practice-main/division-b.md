# 分工 B（答题与评分）边界文件说明

本文说明三件事：

1. 分工 B **可以直接改**的文件（已按企划案实现）；
2. 哪些文件是**边界文件**（B 不能改），为什么属于边界；
3. 在这些边界文件上，**B 还需要做什么**（谁来做、做到什么程度、怎么验收）。

依据：`行测随身练_完整企划案.pdf` 第七章「三人分工与文件／接口边界」——成员 B 负责 `practice/`、`PracticeActivity`、`ResultActivity` 及其 XML 布局，完成选题、答题状态保存、题目切换、进度显示、提交提醒、计时、评分和逐题解析，通过成员 A 的 Repository 读取题目与保存记录。

---

## 一、B 的自有文件（已完成，可自由修改）

| 文件 | 内容 |
| --- | --- |
| `practice/PracticeSession.java` | 会话状态：题目顺序、逐题作答、当前题号、计时起点、提交标记；`saveState/restore` 支持切页与进程重建后恢复 |
| `practice/ScoreCalculator.java` | 判分：每题 10 分、未答 0 分、满分、正确率、用时格式化 |
| `ui/PracticeActivity.java` | 答题页：选题、选项、上一题/下一题、进度条、计时、未答完提醒、提交、跳结果页 |
| `ui/ResultActivity.java` | 结果页：按记录 ID 展示得分、满分、答对数、正确率、用时 |
| `layout/activity_practice.xml` | 答题页布局 |
| `layout/activity_result.xml` | 结果页布局 |

这 6 个文件不引用任何新增依赖（没有 RecyclerView / ViewModel），因此**不需要**改动任何构建文件。

---

## 二、边界文件总表

| 边界文件 | 归属 | 为什么是边界（一句话） | B 需要做什么 |
| --- | --- | --- | --- |
| `data/PracticeRepository.java` | A | 练习记录的写库与查询是 A 的数据层交付，B 只调用 | 向 A 要 2 个方法；约定 `save` 幂等语义；A 未交付前用现有降级逻辑 |
| `data/QuestionRepository.java` | A | 题库查询接口由 A 定义，C 也在用 | 只用现有 `findByCategory`/`findById`；确认 `category=null` 语义 |
| `data/AppDatabaseHelper.java` | A | 建表、列名、版本升级都由 A 负责，B 只按列写值 | 核对列名/可空性；要加列只能提给 A |
| `model/PracticeRecord.java` | 共享 | 三人共用模型，改字段会同时打断 A 的 SQL 与 C 的历史列表 | 只用现有 8 个字段并填满，不私自加字段 |
| `model/AnswerDetail.java` | 共享 | 同上 | 只填 `questionId/userAnswer/correct`，`recordId` 由 A 回填 |
| `AndroidManifest.xml` | C | 应用入口与组件声明由 C 统一维护，B 的两个页面已声明 | 不改动 |
| `app/build.gradle.kts`、`gradle/libs.versions.toml` | C | 构建与依赖一改影响三人 | 不改动；确需新依赖先找 C |
| `res/values/strings.xml`、`colors.xml`、`themes.xml` | C | 公共资源与视觉一致性是 C 的交付 | 文案写在自己页面的 XML/Java 里，不动公共资源 |
| `ui/MainActivity.java`、`activity_main.xml` | C | 首页与导航是 C 的交付 | 无代码改动；把 Intent 契约交给 C |
| `ui/WrongBookActivity.java`、`history`、`settings` 及布局 | C | 复习与设置页面是 C 的交付 | 无代码改动；错题重练由 C 按契约启动 B 的页面 |
| `assets/sample_questions.txt` | A | 内置题库内容与数量是 A 的交付 | 不改动；反馈题量/类别不满足联调 |
| `README.md` | C | 集成与说明书由 C 统一撰写 | 提供“答题与评分”章节素材 |

---

## 三、逐个边界文件的说明与 B 的待办

### 1. `data/PracticeRepository.java`（最重要的边界）

**为什么属于边界**
- 企划案把「保存／读取练习记录、保存／查询错题」列为**成员 A 提供给其他成员的接口**，文件在 `data/`，属于 A 的交付范围。
- 企划案要求「页面不直接拼接 SQL」「评分由 ScoreCalculator 独立完成」，B 只能通过 Repository 落库；如果 B 自己写 SQL，等于绕过 A 的数据层，A 的验收（事务、重启不丢）会失去意义。
- 这个类同时被 B（提交/结果）和 C（历史/错题本）使用，B 单方面修改会和 C 的调用冲突。

**B 现在怎么用它**（已写进代码，不越界）
- `PracticeActivity.submit()`：`new PracticeRepository(this).save(record, details)`，用返回的 `recordId` 跳结果页；
- `ResultActivity.onCreate()`：`new PracticeRepository(this).findById(recordId)`；
- 两处都 `catch (UnsupportedOperationException)` 与 `catch (RuntimeException)`：A 未实现时页面**弹窗显示本次得分并提示“未写入数据库”，不崩溃、不写库**。

**B 需要 A 补齐的接口（请照这个签名提需求）**

```java
/** 一次事务内写入 practice_records + answer_details（+ 答错题写入/更新 wrong_questions），返回新记录 ID。 */
long save(PracticeRecord record, List<AnswerDetail> details);

/** 按记录 ID 读取总体成绩；不存在返回 null。 */
PracticeRecord findById(long recordId);

/** 按记录 ID 读取逐题作答明细，顺序与提交时一致；没有明细返回空列表（不要返回 null）。 */
List<AnswerDetail> findAnswerDetails(long recordId);

/** 错题重练答对后移出错题本；返回受影响行数，题目不在错题本时不报错。 */
int removeWrongQuestion(long questionId);
```

**必须与 A 约定的语义**（写进接口注释或 `docs/`，否则联调必吵）
1. `save` 必须是一个事务：记录、明细、错题三者要么全成功要么全回滚；
2. `save` 成功返回自增主键（>0），失败抛 `RuntimeException`，**不要返回 0 或 -1 静默失败**（B 以 `>0` 判定成功）；
3. `details` 中 `userAnswer` 为 `null` 表示未作答，A 必须允许该列 NULL；
4. `recordId` 由 A 在 `save` 内部回填到每条 `AnswerDetail`（B 传入时 `recordId=0`）；
5. 幂等：同一轮练习重复提交不得产生两条记录。B 侧已做第一道门闩（提交成功后 `session.submitted=true`、按钮禁用，见 `PracticeActivity.submit()`）；**第二道需要 A 提供**，二选一：
   - 以 `(category, started_at)` 建唯一索引，冲突时返回已有记录 ID；或
   - 由 B 传入客户端生成的 `submissionToken`，A 建唯一索引（需要 `AppDatabaseHelper` 加列）。

**验收口径**：提交后杀掉应用重开，历史页能看到这条记录；连点两次提交只产生 1 条记录；未答完提交后明细里 `user_answer` 为 NULL。

---

### 2. `data/QuestionRepository.java`

**为什么属于边界**：题库查询是 A 的数据层接口，C 的分类列表也在用；B 新增方法会改变 A 的类，属于越界。

**B 现在用的方法**（只读，不改）
```java
List<Question> findByCategory(String category, int limit);  // category 为 null 时 B 当作“不限类别”
Question findById(long id);
```

**B 需要做的**
- 与 A 确认 `category == null` 表示“不限类别”这一语义（`PracticeActivity` 依赖它做“综合练习”），若 A 要改成必填，B 的调用要同步；
- 与 A 确认 `findByCategory` 的随机排序（`ORDER BY RANDOM()`）保持不变——B 的会话恢复靠“记住题号再按 ID 查回”，不依赖数据库顺序；
- 若 B 后续需要批量按 ID 查题（例如错题 30 题以上），向 A 提 `List<Question> findByIds(long[] ids)`，**不要**自己在 B 里循环拼 SQL。

---

### 3. `data/AppDatabaseHelper.java`

**为什么属于边界**：建表语句、列名、`DATABASE_VERSION` 与 `onUpgrade` 是 A 的数据层交付；B 改表等于改 A 的验收物。

**B 需要做的**
- 核对 B 填写的字段与列一一对应（现在一致）：

| B 写入的对象字段 | 表.列 |
| --- | --- |
| `record.category` | `practice_records.category`（NOT NULL，B 兜底填“综合练习”/“错题重练”） |
| `record.startedAt / submittedAt` | `started_at / submitted_at` |
| `record.questionCount / correctCount / score` | 同名列 |
| `record.durationSeconds` | `duration_seconds` |
| `detail.questionId / userAnswer / correct` | `answer_details.question_id / user_answer / is_correct` |

- 确认 `answer_details.user_answer` 允许 NULL（未作答），`is_correct` 用 0/1；
- **提醒 A 一个坑**：`onUpgrade` 当前直接 `throw new IllegalStateException`，一旦为了幂等或练习模式加列，旧安装升级会崩，必须由 A 同步实现升级逻辑——这属于 A 的边界，B 不能代改。

---

### 4. `model/PracticeRecord.java` 与 5. `model/AnswerDetail.java`

**为什么属于边界**：这两个是**三人共享的模型**。A 用它做 SQL 映射，B 用它提交，C 用它渲染历史列表；加/改字段会让三个人同时返工。企划案明确要求「变更公共模型或接口前须在小组内说明」。

**B 需要做的**
- `PracticeRecord` 只使用现有 8 个字段，提交时**全部填值**（`id` 由 A 回填）；
- `AnswerDetail` 只填 `questionId / userAnswer / correct`，`recordId` 交给 A；
- 不要为了显示方便在模型里加 `accuracy`、`mode`、`wrong` 之类的派生字段——正确率由 `ScoreCalculator.accuracyPercent()` 现算即可；
- 如果确实需要新字段（例如幂等 `submissionToken`），先在小群说明，由 A 改表 + 改模型，B 再使用。

---

### 6. `AndroidManifest.xml`

**为什么属于边界**：清单文件是全应用入口，C 负责导航与集成，且三人同时改极易冲突。B 的两个 Activity（`.ui.PracticeActivity`、`.ui.ResultActivity`）**已经声明**。

**B 需要做的**：无需改动。如果将来要锁竖屏、加 `configChanges`、改主题或 `exported`，先找 C，不要自己动。

---

### 7. `app/build.gradle.kts` 与 `gradle/libs.versions.toml`

**为什么属于边界**：构建文件与版本目录是公共的，改错会导致全组无法编译；且 A、C 的代码也在同一个 module 里。

**B 需要做的**：不改动。当前 B 的实现没有新增依赖：
- 结果页用 `ScrollView + 代码动态添加 View`，而不是 `RecyclerView`，正是为了不碰构建文件；
- 若后续逐题解析要做成列表并想用 `androidx.recyclerview`，**先跟 C 说要加依赖**，由 C 同时改 `libs.versions.toml` 与 `app/build.gradle.kts`，并确认离线可编译。

---

### 8. `res/values/strings.xml`、`colors.xml`、`themes.xml`

**为什么属于边界**：企划案把 `res/values/` 公共资源划给 C，用于整体视觉一致性；文案抽取是全局决定，B 单方面加 string 会与 C 的命名/整理冲突。

**B 需要做的**
- 现状：B 两个页面的文案**直接写在自己的 XML 与 Java 里**，没有动 `strings.xml`，因此不会与 C 冲突；
- 合并前把待抽取的文案清单交给 C（练习页标题、按钮、未答完提醒、结果页各项标签），由 C 统一决定是否抽取；
- 如果 C 要统一抽取，由 C 修改 B 的布局/代码——这属于 C 的改动范围。

---

### 9. `ui/MainActivity.java`、`activity_main.xml` 与 10. 错题本/历史/设置页面

**为什么属于边界**：首页导航、错题本、历史、设置都是 C 的交付；但 **C 需要启动 B 的答题页**，这是唯一强耦合点。

**B 需要做的**：代码不改；把下面的 Intent 契约在合并前提给 C，并要求 C 按此传参。

---

### 11. `assets/sample_questions.txt`

**为什么属于边界**：内置题库的内容、数量、类别是 A 的交付（企划案第十二条：题目来源由 A 组织）。

**B 需要做的**
- 不改文件；
- **反馈问题**：现在只有 2 题，类别是“资料分析／数量关系”，与企划案“常识判断、言语理解、数量关系各 10 道”不一致。B 的验收（切题、进度、判分、未答完提醒）需要 ≥10 题才能演示，建议 A 先补到 30 题；
- 在 A 补齐前，B 用首页“导入题目”手动导入测试文件进行联调。

---

### 12. `README.md`

**为什么属于边界**：C 负责 README 与 APK 集成。

**B 需要做的**：给 C 提供“答题与评分”一节的素材：进入练习的两种方式（分类 / 错题重练）、判分规则（每题 10 分、未答 0 分）、已知限制（见第六节）。

---

## 四、B 对外发布的契约（Intent 常量，已冻结）

| 键名 | 类型 | 由谁传入 | 含义 | 缺省 |
| --- | --- | --- | --- | --- |
| `category` | String | C（首页/分类） | 练习类别；不传或 null 表示不限类别 | null |
| `question_count` | int | C（首页/设置） | 本次题量 | `PracticeActivity.DEFAULT_QUESTION_COUNT`（10） |
| `wrong_question_ids` | long[] | C（错题本） | 错题重练的题目 ID；传了则忽略类别与题量 | 无 |
| `record_id` | long | B（答题页）/C（历史页） | 结果页要展示的练习记录 ID | 无（≤0 时结果页给出提示） |

常量位置：`PracticeActivity.EXTRA_CATEGORY / EXTRA_QUESTION_COUNT / EXTRA_WRONG_QUESTION_IDS`、`ResultActivity.EXTRA_RECORD_ID`。
**键名一旦发给 C 就不要再改**，改名会让 C 的页面静默失效（`getStringExtra` 返回 null 不会报错）。

---

## 五、完整调用链与当前断点

```
C 首页  --Intent(category,question_count)-->  B PracticeActivity
                                                 |
                                                 +--> A QuestionRepository.findByCategory / findById     ✅ 已可用
                                                 |
                                            作答（PracticeSession + ScoreCalculator）                 ✅ 已实现
                                                 |
                                                 +--> A PracticeRepository.save(record, details)         ❌ A 未实现（抛 UnsupportedOperationException）
                                                 |
                                           Intent(record_id) --> B ResultActivity
                                                 |
                                                 +--> A PracticeRepository.findById(recordId)            ❌ A 未实现（返回 null）
                                                 |
                                                 +--> A PracticeRepository.findAnswerDetails(recordId)   ❌ 方法不存在（逐题解析阻塞点）
```

B 已做的降级：断点处不崩溃，弹窗显示本次得分并说明“未写入数据库”；结果页在拿不到记录时显示记录 ID 与原因。

---

## 六、验收对照（企划案 B 的四条）

| 验收项 | 现状 | 依赖 |
| --- | --- | --- |
| 做题过程中切页不丢答案 | ✅ 已实现：`onSaveInstanceState` 存题号+答案，重建时按 ID 查回同序题目 | 无 |
| 未答完有提醒 | ✅ 已实现：提交前统计未答数并弹确认框 | 无 |
| 单选判分正确 | ✅ 已实现：`ScoreCalculator` 每题 10 分、未答 0 分、满分=题数×10 | 无 |
| 提交后结果可回看 | ⚠️ 代码就绪，数据未通 | A 的 `save` / `findById` |
| 重复提交不重复写库 | ⚠️ B 侧门闩已做，数据库幂等未做 | A 的唯一索引/幂等键 |
| 逐题解析（用户答案、正确答案、解析） | ❌ 未完成 | A 的 `findAnswerDetails` |

---

## 七、A 交付后 B 要改的地方（只需要改 B 自己的文件）

1. `ResultActivity`：把“逐题解析待接入”的提示替换为列表渲染，其余不动。示意：

```java
List<AnswerDetail> details = new PracticeRepository(this).findAnswerDetails(recordId);
QuestionRepository questions = new QuestionRepository(this);
for (AnswerDetail d : details) {
    Question q = questions.findById(d.questionId);   // 题干、选项、正确答案、解析
    // 每题一个 TextView：题干 / 用户答案 / 正确答案 / 解析 / 对错着色
}
```

> 这里 `Question` 只是只读使用；若题量大，向 A 提 `findByIds(long[])` 批量查询，避免循环查库。

2. `PracticeActivity`：无需修改（`save` 签名不变）。

---

## 八、为什么这些文件“属于边界”——三条通用理由

1. **归属与验收**：文件在谁的交付清单里，谁对它的质量与验收负责。B 改 A 的数据层，等于替 A 交付并承担 A 的验收风险。
2. **并发冲突**：三人并行开发，同一个文件被两人改必然互相覆盖；企划案明确要求「避免三人同时修改同一个文件」。
3. **契约稳定性**：模型字段、Repository 方法签名、Intent 键名是三方共同依赖的契约，任何一方单方面改动都会让另外两方的代码“编译通过但运行错误”。

---

## 九、B 的待办清单

- [ ] 向 A 提需求：`findAnswerDetails(long)`、`removeWrongQuestion(long)`、`save` 的事务与幂等语义；
- [ ] 向 A 确认：`findByCategory(null, n)` 语义、`answer_details.user_answer` 允许 NULL、`recordId` 由 A 回填；
- [ ] 向 C 公布第四节 Intent 契约（键名、类型、缺省值）；
- [ ] 向 C 反馈：`assets` 只有 2 题且类别与企划案不一致，联调前需补到 30 题；
- [ ] A 交付 `findAnswerDetails` 后，在 `ResultActivity` 接入逐题解析；
- [ ] A 交付 `save` 后，跑一次完整闭环：首页 → 练习 → 提交 → 结果 → 重启 → 历史 → 打开结果；
- [ ] 合并前把 B 两个页面的文案清单交给 C（是否抽到 `strings.xml` 由 C 定）。
