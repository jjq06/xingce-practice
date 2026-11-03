package com.example.myapplication.practice;

import android.os.Bundle;

import com.example.myapplication.model.Question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次练习的内存会话状态：题目顺序、逐题作答、当前题号、计时与提交标记。
 *
 * <p>本类只维护内存状态，不接触数据库。持久化由成员 A 的
 * {@code data.PracticeRepository} 负责，页面把这里的数据组装成
 * {@code PracticeRecord} 与 {@code AnswerDetail} 后交给 Repository。</p>
 *
 * <p>为了满足“答题过程中切页不丢答案”，{@link #saveState(Bundle)} 与
 * {@link #restore(Bundle, List)} 以题目 ID 数组为基准保存和恢复状态：
 * 恢复时由调用方按 ID 重新查出同样的题目，题目顺序不会因为数据库随机
 * 排序而改变。</p>
 */
public class PracticeSession {

    /** 每题分值，与企划案 6.1 一致：答对 10 分，答错或未作答 0 分。 */
    public static final int POINTS_PER_QUESTION = 10;

    private static final String KEY_QUESTION_IDS = "session_question_ids";
    private static final String KEY_ANSWERS = "session_answers";
    private static final String KEY_INDEX = "session_index";
    private static final String KEY_STARTED_AT = "session_started_at";
    private static final String KEY_SUBMITTED = "session_submitted";
    private static final String KEY_SUBMITTED_AT = "session_submitted_at";

    public final List<Question> questions;
    public final long startedAt;
    public final Map<Long, String> answers = new HashMap<>();
    public int currentIndex;
    public boolean submitted;
    public long submittedAt;

    public PracticeSession(List<Question> questions) {
        this(questions, System.currentTimeMillis());
    }

    public PracticeSession(List<Question> questions, long startedAt) {
        this.questions = questions == null ? new ArrayList<Question>() : questions;
        this.startedAt = startedAt;
    }

    public int size() {
        return questions.size();
    }

    public boolean isEmpty() {
        return questions.isEmpty();
    }

    /** 当前题；题目列表为空时返回 {@code null}。 */
    public Question current() {
        return currentIndex >= 0 && currentIndex < questions.size() ? questions.get(currentIndex) : null;
    }

    /** 某题的已选答案（A～D）；未作答返回 {@code null}。 */
    public String answerOf(long questionId) {
        return answers.get(questionId);
    }

    public boolean isAnswered(Question question) {
        return question != null && answers.containsKey(question.id);
    }

    /** 记录当前题的选择，已有选择会被覆盖；提交后不再接受修改。 */
    public void selectAnswer(String option) {
        Question question = current();
        if (question == null || submitted) {
            return;
        }
        if (option == null) {
            answers.remove(question.id);
        } else {
            answers.put(question.id, option);
        }
    }

    public int answeredCount() {
        int answered = 0;
        for (Question question : questions) {
            if (answers.containsKey(question.id)) {
                answered++;
            }
        }
        return answered;
    }

    public int unansweredCount() {
        return questions.size() - answeredCount();
    }

    /** 已用秒数；负数按 0 处理，避免系统时间回拨时显示异常。 */
    public long elapsedSeconds(long now) {
        return Math.max(0L, (now - startedAt) / 1000L);
    }

    public void markSubmitted(long submittedAt) {
        this.submitted = true;
        this.submittedAt = submittedAt;
    }

    public boolean canGoPrev() {
        return currentIndex > 0;
    }

    public boolean canGoNext() {
        return currentIndex >= 0 && currentIndex < questions.size() - 1;
    }

    /** 切到指定题号；越界时返回 {@code false}，状态不变。 */
    public boolean moveTo(int index) {
        if (index < 0 || index >= questions.size()) {
            return false;
        }
        currentIndex = index;
        return true;
    }

    /** 把会话写入 {@code onSaveInstanceState} 的 Bundle。 */
    public void saveState(Bundle outState) {
        int count = questions.size();
        long[] ids = new long[count];
        String[] values = new String[count];
        for (int i = 0; i < count; i++) {
            Question question = questions.get(i);
            ids[i] = question.id;
            values[i] = answers.get(question.id);
        }
        outState.putLongArray(KEY_QUESTION_IDS, ids);
        outState.putStringArray(KEY_ANSWERS, values);
        outState.putInt(KEY_INDEX, currentIndex);
        outState.putLong(KEY_STARTED_AT, startedAt);
        outState.putBoolean(KEY_SUBMITTED, submitted);
        outState.putLong(KEY_SUBMITTED_AT, submittedAt);
    }

    /** Bundle 中是否保存过会话（用于区分“首次进入”和“重建”）。 */
    public static boolean hasState(Bundle state) {
        return state != null && state.containsKey(KEY_QUESTION_IDS);
    }

    /** 取出保存过的题号顺序；没有记录时返回空数组。 */
    public static long[] savedQuestionIds(Bundle state) {
        if (state == null) {
            return new long[0];
        }
        long[] ids = state.getLongArray(KEY_QUESTION_IDS);
        return ids == null ? new long[0] : ids;
    }

    /**
     * 用保存的状态和重新查出的题目还原会话。
     *
     * @param state     之前由 {@link #saveState(Bundle)} 写入的 Bundle
     * @param questions 按 {@link #savedQuestionIds(Bundle)} 顺序重新查出的题目
     */
    public static PracticeSession restore(Bundle state, List<Question> questions) {
        long startedAt = state == null
                ? System.currentTimeMillis()
                : state.getLong(KEY_STARTED_AT, System.currentTimeMillis());
        PracticeSession session = new PracticeSession(questions, startedAt);

        String[] values = state == null ? null : state.getStringArray(KEY_ANSWERS);
        if (values != null) {
            for (int i = 0; i < session.size() && i < values.length; i++) {
                if (values[i] != null) {
                    session.answers.put(session.questions.get(i).id, values[i]);
                }
            }
        }

        int savedIndex = state == null ? 0 : state.getInt(KEY_INDEX, 0);
        session.currentIndex = Math.max(0, Math.min(savedIndex, Math.max(0, session.size() - 1)));
        session.submitted = state != null && state.getBoolean(KEY_SUBMITTED, false);
        session.submittedAt = state == null ? 0L : state.getLong(KEY_SUBMITTED_AT, 0L);
        return session;
    }
}
