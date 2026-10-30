package com.example.myapplication.data;

import android.content.Context;
import android.database.Cursor;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.myapplication.model.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.StringReader;
import java.util.*;
import static org.junit.Assert.*;

/** 使用独立随机数据库，不清空应用的真实题库和练习记录。 */
@RunWith(AndroidJUnit4.class)
public class DataRepositoryTest {
    private Context context;
    private String name;
    private AppDatabaseHelper helper;
    private QuestionRepository questions;
    private PracticeRepository records;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        name = "member-a-test-" + UUID.randomUUID() + ".db";
        reopen();
    }
    private void reopen() {
        helper = new AppDatabaseHelper(context, name);
        questions = new QuestionRepository(helper);
        records = new PracticeRepository(helper);
    }
    @After public void cleanup() { helper.close(); context.deleteDatabase(name); }
    private PracticeRecord record(int count) {
        PracticeRecord r = new PracticeRecord();
        r.category = "全部"; r.startedAt = 1000; r.submittedAt = 6500; r.questionCount = count;
        return r;
    }
    private AnswerDetail answer(Question q, String value) {
        AnswerDetail a = new AnswerDetail(); a.questionId = q.id; a.userAnswer = value; return a;
    }
    @Test public void seedsThirtyAndPersistsWithoutDuplicates() {
        assertEquals(30, questions.count());
        assertEquals(3, questions.categories().size());
        for (String category : questions.categories()) assertEquals(10, questions.count(category));
        assertEquals(Integer.valueOf(10), questions.categoryCounts().get("常识判断"));
        helper.close(); reopen();
        assertEquals(30, questions.count());
        assertNull(questions.findById(-1));
        assertTrue(questions.findByCategory("不存在", 10).isEmpty());
    }
    @Test public void importsValidRowsAndSkipsDuplicatesIncludingWithinFile() throws Exception {
        String row = "测试\t新增题\t一\t二\t三\t四\tA\t解析";
        QuestionImporter.ParseResult parsed = new QuestionImporter().parse(new StringReader(row + "\n错误行\n" + row), "测试");
        assertEquals(1, parsed.errors.size());
        assertEquals(1, questions.insertAll(parsed.questions));
        assertEquals(0, questions.insertAll(parsed.questions));
        helper.close(); reopen();
        assertEquals(31, questions.count());
    }
    @Test public void savesUnansweredAndCorrectAndWrongWithIdempotency() {
        List<Question> list = questions.findByCategory(null, 3);
        List<AnswerDetail> details = Arrays.asList(answer(list.get(0), list.get(0).answer),
                answer(list.get(1), "A".equals(list.get(1).answer) ? "B" : "A"), answer(list.get(2), null));
        PracticeRecord r = record(3);
        long id = records.save(r, details);
        assertEquals(id, records.save(r, details));
        assertEquals(1, records.history().size());
        assertEquals(10, r.score);
        assertEquals(5, r.durationSeconds);
        assertEquals(2, records.wrongQuestionIds().size());
        helper.close(); reopen();
        assertEquals(3, records.findAnswers(id).size());
        assertNull(records.findAnswers(id).get(2).userAnswer);
        assertEquals(10, records.findById(id).score);
        assertEquals(2, records.wrongQuestionIds().size());
    }
    @Test public void repeatedMistakeIncrementsAndCorrectRetryRemoves() {
        Question q = questions.findByCategory(null, 1).get(0);
        records.save(record(1), Collections.singletonList(answer(q, null)));
        records.save(record(1), Collections.singletonList(answer(q, null)));
        assertEquals(1, records.wrongQuestionIds().size());
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT wrong_count FROM wrong_questions", null)) {
            assertTrue(c.moveToFirst()); assertEquals(2, c.getInt(0));
        }
        records.save(record(1), Collections.singletonList(answer(q, q.answer)));
        assertTrue(records.wrongQuestionIds().isEmpty());
    }
    @Test public void invalidSubmissionDoesNotWritePartialData() {
        Question q = questions.findByCategory(null, 1).get(0);
        AnswerDetail invalid = new AnswerDetail(); invalid.questionId = -1;
        PracticeRecord r = record(2);
        assertThrows(IllegalArgumentException.class, () -> records.save(r, Arrays.asList(answer(q, null), invalid)));
        assertEquals(0, r.id);
        assertTrue(records.history().isEmpty());
        assertTrue(records.wrongQuestionIds().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> records.save(r, Arrays.asList(answer(q, null), answer(q, null))));
    }
    @Test public void databaseFailureRollsBackRecordDetailsAndWrongBook() {
        Question q = questions.findByCategory(null, 1).get(0);
        helper.getWritableDatabase().execSQL("CREATE TRIGGER reject_detail BEFORE INSERT ON answer_details "
                + "BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        PracticeRecord r = record(1);
        assertThrows(android.database.sqlite.SQLiteException.class,
                () -> records.save(r, Collections.singletonList(answer(q, null))));
        assertEquals(0, r.id);
        assertTrue(records.history().isEmpty());
        assertTrue(records.wrongQuestionIds().isEmpty());
    }
    @Test public void migrationKeepsExistingRecordsAndQuestions() throws Exception {
        Question q = questions.findByCategory(null, 1).get(0);
        long id = records.save(record(1), Collections.singletonList(answer(q, null)));
        questions.insertAll(new QuestionImporter().parse(new StringReader("自定义\t旧题\t甲\t乙\t丙\t丁\tA\t"), "旧版").questions);
        helper.getWritableDatabase().setVersion(1);
        helper.close(); reopen();
        assertEquals(31, questions.count());
        assertNotNull(records.findById(id));
        assertEquals(1, records.wrongQuestionIds().size());
        assertEquals(2, helper.getReadableDatabase().getVersion());
    }
    @Test public void emptyQueriesAndHistoryOrder() {
        assertTrue(records.history().isEmpty()); assertTrue(records.findAnswers(999).isEmpty());
        assertNull(records.findById(999)); assertTrue(records.wrongQuestionIds().isEmpty());
        Question q = questions.findByCategory(null, 1).get(0);
        records.save(record(1), Collections.singletonList(answer(q, null)));
        PracticeRecord newer = record(1); newer.submittedAt = 8000;
        long id = records.save(newer, Collections.singletonList(answer(q, q.answer)));
        assertEquals(id, records.history().get(0).id);
    }
}
