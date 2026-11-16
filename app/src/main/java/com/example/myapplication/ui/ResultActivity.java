package com.example.myapplication.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.AppContract;
import com.example.myapplication.R;
import com.example.myapplication.data.PracticeRepository;
import com.example.myapplication.data.QuestionRepository;
import com.example.myapplication.model.AnswerDetail;
import com.example.myapplication.model.PracticeRecord;
import com.example.myapplication.model.Question;
import com.example.myapplication.practice.ScoreCalculator;

import java.util.List;

/**
 * 结果页：按练习记录 ID 回看成绩与逐题解析。
 *
 * <p>入参为 {@link AppContract#EXTRA_RECORD_ID}；提交后的答题页和历史记录页都用同一个键，
 * 旧结果因此可以随时重新打开。{@link #EXTRA_RECORD_ID} 是 {@code docs/interfaces.md}
 * 约定的向后兼容别名。</p>
 *
 * <p>成绩来自 {@code PracticeRepository.findById}，逐题解析来自
 * {@code PracticeRepository.findAnswers}。两个方法目前都是“已定签名、待实现”，
 * 本页对 {@link UnsupportedOperationException} 做了降级提示，数据层实现后无需再改本页。</p>
 */
public class ResultActivity extends AppCompatActivity {

    /** 向后兼容别名；新代码请使用 {@link AppContract#EXTRA_RECORD_ID}。 */
    public static final String EXTRA_RECORD_ID = AppContract.EXTRA_RECORD_ID;

    private TextView statusView;
    private TextView categoryView;
    private TextView scoreView;
    private TextView correctView;
    private TextView accuracyView;
    private TextView timeView;
    private TextView detailsTitleView;
    private LinearLayout summaryView;
    private LinearLayout detailsView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(getColor(R.color.home_mint));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_result);
        bindViews();
        findViewById(R.id.result_back).setOnClickListener(v -> finish());

        long recordId = getIntent().getLongExtra(AppContract.EXTRA_RECORD_ID, -1L);
        if (recordId <= 0L) {
            showMessage("没有收到练习记录 ID，无法显示本次成绩。");
            return;
        }

        PracticeRecord record = null;
        String error = null;
        try {
            record = new PracticeRepository(this).findById(recordId);
        } catch (UnsupportedOperationException e) {
            error = "练习记录查询尚未接入：PracticeRepository.findById() 仍是待实现状态，"
                    + "该文件由成员 A 负责，接入说明见根目录 division-b.md。";
        } catch (RuntimeException e) {
            error = "读取练习记录失败：" + e.getMessage();
        }

        if (record == null) {
            String message = "找不到练习记录（ID=" + recordId + "）。";
            if (!TextUtils.isEmpty(error)) {
                message = message + "\n" + error;
            }
            showMessage(message);
            return;
        }
        renderSummary(record);
        renderDetails(recordId);
    }

    private void bindViews() {
        statusView = findViewById(R.id.page_status);
        categoryView = findViewById(R.id.result_category);
        scoreView = findViewById(R.id.result_score);
        correctView = findViewById(R.id.result_correct);
        accuracyView = findViewById(R.id.result_accuracy);
        timeView = findViewById(R.id.result_time);
        detailsTitleView = findViewById(R.id.result_details_title);
        summaryView = findViewById(R.id.result_summary);
        detailsView = findViewById(R.id.result_details);
    }

    private void renderSummary(PracticeRecord record) {
        statusView.setVisibility(View.GONE);
        summaryView.setVisibility(View.VISIBLE);
        detailsTitleView.setVisibility(View.VISIBLE);
        detailsView.setVisibility(View.VISIBLE);

        int fullScore = ScoreCalculator.fullScore(record.questionCount);
        categoryView.setText(TextUtils.isEmpty(record.category) ? "练习记录" : record.category);
        scoreView.setText(record.score + " / " + fullScore + " 分");
        correctView.setText("答对 " + record.correctCount + " / " + record.questionCount + " 题");
        accuracyView.setText("正确率 "
                + ScoreCalculator.accuracyPercent(record.correctCount, record.questionCount) + "%");
        timeView.setText("用时 " + ScoreCalculator.formatDuration(record.durationSeconds));
    }

    /** 逐题解析：数据层就绪后自动生效，本页无需再改。 */
    private void renderDetails(long recordId) {
        detailsView.removeAllViews();

        List<AnswerDetail> details;
        try {
            details = new PracticeRepository(this).findAnswers(recordId);
        } catch (UnsupportedOperationException e) {
            addNote("逐题解析尚未接入：PracticeRepository.findAnswers(recordId) 仍是待实现状态（成员 A 负责）。");
            return;
        } catch (RuntimeException e) {
            addNote("读取作答明细失败：" + e.getMessage());
            return;
        }

        if (details == null || details.isEmpty()) {
            addNote("这条记录没有作答明细。");
            return;
        }

        QuestionRepository questions = new QuestionRepository(this);
        int index = 0;
        for (AnswerDetail detail : details) {
            index++;
            Question question = questions.findById(detail.questionId);
            detailsView.addView(buildDetailView(index, detail, question));
        }
    }

    private LinearLayout buildDetailView(int index, AnswerDetail detail, Question question) {
        String userAnswer = detail.userAnswer;
        String correctAnswer = question == null ? "—" : question.answer;
        boolean unanswered = TextUtils.isEmpty(userAnswer);
        int stateColor = unanswered ? R.color.home_muted
                : detail.correct ? R.color.home_accent_dark : R.color.result_error;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_home_card);
        card.setPadding(dp(20), dp(18), dp(20), dp(20));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);

        TextView state = new TextView(this);
        state.setText("第 " + index + " 题  ·  "
                + (unanswered ? "未作答" : detail.correct ? "回答正确" : "回答错误"));
        state.setTextColor(getColor(stateColor));
        state.setTextSize(14);
        state.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(state);

        TextView stem = new TextView(this);
        stem.setText(question == null ? "题目已不存在（ID=" + detail.questionId + "）" : question.stem);
        stem.setTextColor(getColor(R.color.home_ink));
        stem.setTextSize(17);
        stem.setTypeface(null, android.graphics.Typeface.BOLD);
        stem.setLineSpacing(dp(4), 1f);
        stem.setPadding(0, dp(12), 0, dp(12));
        card.addView(stem);

        TextView answers = new TextView(this);
        answers.setText("你的答案  " + (unanswered ? "未作答" : userAnswer)
                + "     正确答案  " + correctAnswer);
        answers.setTextColor(getColor(stateColor));
        answers.setTextSize(14);
        card.addView(answers);

        if (question != null && !TextUtils.isEmpty(question.explanation)) {
            TextView explanation = new TextView(this);
            explanation.setText("解析\n" + question.explanation);
            explanation.setTextColor(getColor(R.color.home_muted));
            explanation.setTextSize(14);
            explanation.setLineSpacing(dp(3), 1f);
            explanation.setPadding(0, dp(14), 0, 0);
            card.addView(explanation);
        }
        return card;
    }

    private void addNote(String message) {
        TextView note = new TextView(this);
        note.setText(message);
        note.setTextSize(14f);
        note.setLineSpacing(0f, 1.2f);
        note.setTextColor(getColor(R.color.home_muted));
        detailsView.addView(note);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showMessage(String message) {
        summaryView.setVisibility(View.GONE);
        detailsTitleView.setVisibility(View.GONE);
        detailsView.setVisibility(View.GONE);
        statusView.setVisibility(View.VISIBLE);
        statusView.setText(message);
    }
}
