package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.AppContract;
import com.example.myapplication.R;
import com.example.myapplication.data.PracticeRepository;
import com.example.myapplication.data.QuestionRepository;
import com.example.myapplication.model.AnswerDetail;
import com.example.myapplication.model.PracticeRecord;
import com.example.myapplication.model.Question;
import com.example.myapplication.practice.PracticeSession;
import com.example.myapplication.practice.ScoreCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 答题页：选题、作答、切题、进度与计时、未答完提醒、提交并跳转结果页。
 *
 * <p>入参遵循 {@code docs/interfaces.md} 与 {@link AppContract}：
 * {@code EXTRA_PRACTICE_MODE} 必填（{@code MODE_NORMAL} 或 {@code MODE_WRONG_BOOK}），
 * {@code EXTRA_CATEGORY} 可选（{@code null} 表示全部分类）。题量取自
 * {@code SharedPreferences} 的 {@code PREF_DEFAULT_QUESTION_COUNT}，不经 Intent 传递。</p>
 *
 * <p>题目读取与记录保存全部经过成员 A 的 Repository，本页不拼 SQL。
 * Repository 的写接口目前抛 {@link UnsupportedOperationException}，本页对此做了降级，
 * 细节见根目录 division-b.md。</p>
 */
public class PracticeActivity extends AppCompatActivity {

    private static final String STATE_RECORD_ID = "state_record_id";
    private static final long TICK_INTERVAL_MS = 1000L;
    /** 题量上限，避免设置页写入异常值时一次查出过多题目。 */
    private static final int MAX_QUESTION_COUNT = 100;

    private TextView titleView;
    private TextView progressView;
    private TextView stemView;
    private TextView statusView;
    private ProgressBar progressBar;
    private RadioGroup optionGroup;
    private RadioButton[] optionButtons;
    private Button prevButton;
    private Button nextButton;
    private Button submitButton;
    private View bodyView;
    private View navView;

    private PracticeSession session;
    private String requestedCategory;
    private boolean wrongPractice;
    private long savedRecordId;
    private boolean bindingOptions;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            timerHandler.postDelayed(this, TICK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        bindViews();
        bindListeners();

        String mode = getIntent().getStringExtra(AppContract.EXTRA_PRACTICE_MODE);
        wrongPractice = AppContract.MODE_WRONG_BOOK.equals(mode);
        boolean modeValid = wrongPractice || AppContract.MODE_NORMAL.equals(mode);
        requestedCategory = getIntent().getStringExtra(AppContract.EXTRA_CATEGORY);

        if (savedInstanceState != null) {
            savedRecordId = savedInstanceState.getLong(STATE_RECORD_ID, 0L);
        }
        if (!modeValid) {
            showEmpty("无法识别本次练习的进入方式（缺少或非法的练习模式参数）。\n请从首页重新进入练习。");
            return;
        }

        String loadError = null;
        try {
            session = restoreSession(savedInstanceState);
        } catch (UnsupportedOperationException e) {
            session = new PracticeSession(null);
            loadError = "错题查询尚未接入：" + e.getMessage()
                    + "\n（data/PracticeRepository 由成员 A 负责）";
        } catch (RuntimeException e) {
            session = new PracticeSession(null);
            loadError = "读取题库失败：" + e.getMessage();
        }

        if (session.isEmpty()) {
            showEmpty(loadError);
        } else {
            render();
        }
    }

    private void bindViews() {
        titleView = findViewById(R.id.practice_title);
        progressView = findViewById(R.id.practice_progress);
        progressBar = findViewById(R.id.practice_progress_bar);
        statusView = findViewById(R.id.practice_status);
        stemView = findViewById(R.id.practice_stem);
        optionGroup = findViewById(R.id.practice_options);
        optionButtons = new RadioButton[]{
                findViewById(R.id.option_a),
                findViewById(R.id.option_b),
                findViewById(R.id.option_c),
                findViewById(R.id.option_d)
        };
        prevButton = findViewById(R.id.practice_prev);
        nextButton = findViewById(R.id.practice_next);
        submitButton = findViewById(R.id.practice_submit);
        bodyView = findViewById(R.id.practice_body);
        navView = findViewById(R.id.practice_nav);
    }

    private void bindListeners() {
        optionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingOptions || session == null || session.isEmpty() || session.submitted) {
                return;
            }
            String option = optionOf(checkedId);
            if (option == null) {
                return;
            }
            session.selectAnswer(option);
            updateProgress();
        });

        prevButton.setOnClickListener(v -> {
            if (session != null) {
                goTo(session.currentIndex - 1);
            }
        });
        nextButton.setOnClickListener(v -> {
            if (session != null) {
                goTo(session.currentIndex + 1);
            }
        });
        submitButton.setOnClickListener(v -> onSubmitClicked());
    }

    // ---------------------------------------------------------------- 会话与选题

    /**
     * 优先按上次保存的题号恢复，保证随机选题的顺序与答案一一对应；
     * 没有历史状态时才按练习模式重新选题。
     */
    private PracticeSession restoreSession(Bundle state) {
        QuestionRepository repository = new QuestionRepository(this);

        if (PracticeSession.hasState(state)) {
            List<Question> restored = new ArrayList<>();
            for (long id : PracticeSession.savedQuestionIds(state)) {
                Question question = repository.findById(id);
                if (question != null) {
                    restored.add(question);
                }
            }
            if (!restored.isEmpty()) {
                return PracticeSession.restore(state, restored);
            }
        }

        List<Question> questions = new ArrayList<>();
        if (wrongPractice) {
            List<Long> wrongIds = new PracticeRepository(this).wrongQuestionIds();
            if (wrongIds != null) {
                for (Long id : wrongIds) {
                    if (id == null) {
                        continue;
                    }
                    Question question = repository.findById(id);
                    if (question != null) {
                        questions.add(question);
                    }
                }
            }
        } else {
            int count = clampQuestionCount(defaultQuestionCount());
            List<Question> loaded = repository.findByCategory(requestedCategory, count);
            if (loaded != null) {
                questions = loaded;
            }
        }
        return new PracticeSession(questions);
    }

    /** 题量来自设置页写入的 SharedPreferences；缺失或异常时回落到契约默认值。 */
    private int defaultQuestionCount() {
        return getSharedPreferences(AppContract.PREFS_NAME, MODE_PRIVATE)
                .getInt(AppContract.PREF_DEFAULT_QUESTION_COUNT, AppContract.DEFAULT_QUESTION_COUNT);
    }

    private static int clampQuestionCount(int count) {
        if (count <= 0) {
            return AppContract.DEFAULT_QUESTION_COUNT;
        }
        return Math.min(count, MAX_QUESTION_COUNT);
    }

    // ---------------------------------------------------------------- 渲染

    private void render() {
        Question question = session.current();
        if (question == null) {
            showEmpty(null);
            return;
        }

        titleView.setText(titleText());
        statusView.setVisibility(View.GONE);
        setPracticeVisible(true);
        stemView.setText(question.stem);

        bindingOptions = true;
        optionGroup.clearCheck();
        RadioButton selected = buttonOf(session.answerOf(question.id));
        if (selected != null) {
            selected.setChecked(true);
        }
        bindingOptions = false;

        optionButtons[0].setText("A. " + question.optionA);
        optionButtons[1].setText("B. " + question.optionB);
        optionButtons[2].setText("C. " + question.optionC);
        optionButtons[3].setText("D. " + question.optionD);

        for (RadioButton button : optionButtons) {
            button.setEnabled(!session.submitted);
        }
        prevButton.setEnabled(session.canGoPrev());
        nextButton.setEnabled(session.canGoNext());
        submitButton.setEnabled(!session.submitted || savedRecordId > 0);
        submitButton.setText(session.submitted ? "查看成绩" : "提交并查看成绩");
        updateProgress();
    }

    private void updateProgress() {
        if (session == null || session.isEmpty()) {
            progressView.setText("");
            progressBar.setProgress(0);
            return;
        }
        long elapsed = session.elapsedSeconds(System.currentTimeMillis());
        progressView.setText("第 " + (session.currentIndex + 1) + "/" + session.size() + " 题 · 已答 "
                + session.answeredCount() + " 题 · 用时 " + ScoreCalculator.formatDuration(elapsed));
        progressBar.setProgress(session.answeredCount() * 100 / session.size());
    }

    private void showEmpty(String error) {
        titleView.setText(titleText());
        setPracticeVisible(false);
        progressView.setText("");
        statusView.setVisibility(View.VISIBLE);
        if (error != null) {
            statusView.setText(error);
        } else {
            statusView.setText("题库为空，暂时没有可练习的题目。\n"
                    + "请先在首页进入“导入题目”导入 UTF-8 TXT 题库（内置题库由成员 A 负责）。");
        }
    }

    private void setPracticeVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        bodyView.setVisibility(visibility);
        navView.setVisibility(visibility);
        progressBar.setVisibility(visibility);
        submitButton.setVisibility(visibility);
    }

    private String titleText() {
        if (wrongPractice) {
            return "错题重练";
        }
        if (requestedCategory != null && !requestedCategory.isEmpty()) {
            return requestedCategory + " · 练习";
        }
        return "综合练习";
    }

    /** 写入 practice_records.category 的值；契约规定综合练习使用“全部”。 */
    private String categoryForRecord() {
        if (wrongPractice) {
            return "错题本";
        }
        if (requestedCategory != null && !requestedCategory.isEmpty()) {
            return requestedCategory;
        }
        return "全部";
    }

    // ---------------------------------------------------------------- 交互

    private void goTo(int index) {
        if (session != null && session.moveTo(index)) {
            render();
        }
    }

    private void onSubmitClicked() {
        if (session == null || session.isEmpty()) {
            return;
        }
        if (session.submitted) {
            openResult();
            return;
        }
        int unanswered = session.unansweredCount();
        if (unanswered > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("还有未作答题目")
                    .setMessage("还有 " + unanswered + " 题没有作答，未作答按 0 分计。现在提交吗？")
                    .setPositiveButton("仍然提交", (dialog, which) -> submit())
                    .setNegativeButton("继续作答", null)
                    .show();
            return;
        }
        submit();
    }

    /**
     * 提交：组装 PracticeRecord 与逐题 AnswerDetail，交给成员 A 的
     * PracticeRepository 在一个事务里写入记录、明细与错题本，成功后再跳结果页。
     *
     * <p>“重复提交不重复写库”：提交成功后会话标记为已提交、按钮不再触发写库；
     * Repository 侧的事务与幂等要求见 docs/interfaces.md 与 division-b.md。</p>
     */
    private void submit() {
        if (session == null || session.isEmpty() || session.submitted) {
            return;
        }
        submitButton.setEnabled(false);
        long submittedAt = System.currentTimeMillis();

        List<Question> questions = session.questions;
        int correctCount = ScoreCalculator.correctCount(questions, session.answers);

        PracticeRecord record = new PracticeRecord();
        record.category = categoryForRecord();
        record.startedAt = session.startedAt;
        record.submittedAt = submittedAt;
        record.questionCount = questions.size();
        record.correctCount = correctCount;
        record.score = ScoreCalculator.scoreOf(correctCount);
        record.durationSeconds = ScoreCalculator.durationSeconds(session.startedAt, submittedAt);

        List<AnswerDetail> details = new ArrayList<>();
        for (Question question : questions) {
            AnswerDetail detail = new AnswerDetail();
            detail.questionId = question.id;
            detail.userAnswer = session.answerOf(question.id);
            detail.correct = ScoreCalculator.isCorrect(question, detail.userAnswer);
            details.add(detail);
        }

        String failure;
        long recordId;
        try {
            recordId = new PracticeRepository(this).save(record, details);
            failure = recordId > 0 ? null : "数据层没有返回练习记录 ID。";
        } catch (UnsupportedOperationException e) {
            recordId = 0L;
            failure = "练习记录保存尚未接入：PracticeRepository.save() 仍是待实现状态，"
                    + "该文件由成员 A 负责，接入说明见根目录 division-b.md。";
        } catch (RuntimeException e) {
            recordId = 0L;
            failure = "练习记录保存失败：" + e.getMessage();
        }

        if (failure != null) {
            savedRecordId = 0L;
            submitButton.setEnabled(true);
            showSummaryDialog(record, failure);
            return;
        }

        savedRecordId = recordId;
        session.markSubmitted(submittedAt);
        openResult();
    }

    private void openResult() {
        if (savedRecordId <= 0) {
            return;
        }
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(AppContract.EXTRA_RECORD_ID, savedRecordId);
        startActivity(intent);
        finish();
    }

    /** 数据层不可用时的兜底：至少把本次得分算给用户看，不崩溃、不写库。 */
    private void showSummaryDialog(PracticeRecord record, String failure) {
        String summary = "得分 " + record.score + " / " + ScoreCalculator.fullScore(record.questionCount)
                + "\n正确 " + record.correctCount + " / " + record.questionCount + " 题（"
                + ScoreCalculator.accuracyPercent(record.correctCount, record.questionCount) + "%）"
                + "\n用时 " + ScoreCalculator.formatDuration(record.durationSeconds)
                + "\n\n" + failure;
        new AlertDialog.Builder(this)
                .setTitle("本次成绩（未写入数据库）")
                .setMessage(summary)
                .setPositiveButton("知道了", null)
                .show();
    }

    private String optionOf(int checkedId) {
        if (checkedId == R.id.option_a) {
            return "A";
        }
        if (checkedId == R.id.option_b) {
            return "B";
        }
        if (checkedId == R.id.option_c) {
            return "C";
        }
        if (checkedId == R.id.option_d) {
            return "D";
        }
        return null;
    }

    private RadioButton buttonOf(String option) {
        if (option == null) {
            return null;
        }
        switch (option.trim().toUpperCase(Locale.ROOT)) {
            case "A":
                return optionButtons[0];
            case "B":
                return optionButtons[1];
            case "C":
                return optionButtons[2];
            case "D":
                return optionButtons[3];
            default:
                return null;
        }
    }

    // ---------------------------------------------------------------- 生命周期

    @Override
    protected void onStart() {
        super.onStart();
        if (session != null && !session.isEmpty() && !session.submitted) {
            timerHandler.post(timerTick);
        }
    }

    @Override
    protected void onStop() {
        timerHandler.removeCallbacks(timerTick);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (session != null) {
            session.saveState(outState);
        }
        outState.putLong(STATE_RECORD_ID, savedRecordId);
    }
}
