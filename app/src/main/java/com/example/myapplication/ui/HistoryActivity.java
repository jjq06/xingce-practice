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
import com.example.myapplication.model.PracticeRecord;
import com.example.myapplication.practice.ScoreCalculator;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(getColor(R.color.home_mint));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_history);
        findViewById(R.id.history_back).setOnClickListener(v -> finish());
    }

    @Override protected void onResume() {
        super.onResume();
        new Thread(() -> {
            List<PracticeRecord> records = new PracticeRepository(this).history();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                LinearLayout list = findViewById(R.id.history_list);
                list.removeAllViews();
                TextView status = findViewById(R.id.page_status);
                status.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
                status.setText(R.string.history_empty);
                TextView count = findViewById(R.id.history_count);
                count.setText(getString(R.string.history_count, records.size()));
                DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
                for (PracticeRecord record : records) {
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setBackgroundResource(R.drawable.bg_home_card);
                    card.setPadding(dp(20), dp(18), dp(20), dp(18));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.bottomMargin = dp(12);
                    list.addView(card, params);

                    TextView category = new TextView(this);
                    category.setText(record.category + "  ›");
                    category.setTextColor(getColor(R.color.home_ink));
                    category.setTextSize(18);
                    category.setTypeface(null, android.graphics.Typeface.BOLD);
                    card.addView(category);

                    TextView date = new TextView(this);
                    date.setText(formatter.format(new Date(record.submittedAt)));
                    date.setTextColor(getColor(R.color.home_muted));
                    date.setTextSize(13);
                    date.setPadding(0, dp(4), 0, dp(10));
                    card.addView(date);

                    int percent = ScoreCalculator.accuracyPercent(record.correctCount, record.questionCount);
                    TextView summary = new TextView(this);
                    summary.setText(getString(R.string.history_summary, record.score, record.correctCount,
                            record.questionCount, percent, record.durationSeconds));
                    summary.setTextColor(getColor(R.color.home_accent_dark));
                    summary.setTextSize(15);
                    card.addView(summary);

                    card.setClickable(true);
                    card.setFocusable(true);
                    card.setOnClickListener(v -> {
                        Intent intent = new Intent(this, ResultActivity.class);
                        intent.putExtra(AppContract.EXTRA_RECORD_ID, record.id);
                        startActivity(intent);
                    });
                }
            });
        }).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
