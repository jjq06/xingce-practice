package com.example.myapplication.ui;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.AppContract;
import com.example.myapplication.R;
import com.example.myapplication.data.PracticeRepository;
import com.example.myapplication.data.QuestionRepository;
import com.example.myapplication.model.Question;
import java.util.ArrayList;
import java.util.List;

public class WrongBookActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(getColor(R.color.home_mint));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_wrong_book);
        findViewById(R.id.wrong_back).setOnClickListener(v -> finish());
        findViewById(R.id.retry_wrong).setOnClickListener(v -> {
            Intent intent = new Intent(this, PracticeActivity.class);
            intent.putExtra(AppContract.EXTRA_PRACTICE_MODE, AppContract.MODE_WRONG_BOOK);
            startActivity(intent);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        new Thread(() -> {
            List<Long> ids = new PracticeRepository(this).wrongQuestionIds();
            QuestionRepository repository = new QuestionRepository(this);
            List<Question> questions = new ArrayList<>();
            for (Long id : ids) {
                Question question = repository.findById(id);
                if (question != null) questions.add(question);
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                LinearLayout list = findViewById(R.id.wrong_list);
                list.removeAllViews();
                TextView status = findViewById(R.id.page_status);
                status.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
                status.setText(R.string.wrong_empty);
                TextView count = findViewById(R.id.wrong_count);
                count.setText(getString(R.string.wrong_count, questions.size()));
                findViewById(R.id.retry_wrong).setEnabled(!questions.isEmpty());
                findViewById(R.id.retry_wrong).setAlpha(questions.isEmpty() ? 0.45f : 1f);
                for (Question question : questions) {
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setBackgroundResource(R.drawable.bg_home_card);
                    card.setPadding(dp(20), dp(18), dp(20), dp(18));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.bottomMargin = dp(12);
                    list.addView(card, params);

                    TextView category = new TextView(this);
                    category.setText(question.category);
                    category.setTextColor(getColor(R.color.home_accent_dark));
                    category.setTextSize(13);
                    card.addView(category);

                    TextView stem = new TextView(this);
                    stem.setText(question.stem);
                    stem.setTextColor(getColor(R.color.home_ink));
                    stem.setTextSize(17);
                    stem.setPadding(0, dp(8), 0, 0);
                    card.addView(stem);
                }
            });
        }).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
