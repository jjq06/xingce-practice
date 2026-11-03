package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import com.example.myapplication.ui.HistoryActivity;
import com.example.myapplication.ui.ImportActivity;
import com.example.myapplication.ui.PracticeActivity;
import com.example.myapplication.ui.SettingsActivity;
import com.example.myapplication.ui.WrongBookActivity;
import com.example.myapplication.data.QuestionRepository;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TextView questionCount;
    private LinearLayout categories;
    private Button startPractice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        questionCount = findViewById(R.id.question_count);
        categories = findViewById(R.id.category_list);
        startPractice = findViewById(R.id.start_practice);
        startPractice.setOnClickListener(v -> startPractice(null));
        findViewById(R.id.import_questions).setOnClickListener(v -> open(ImportActivity.class));
        findViewById(R.id.wrong_book).setOnClickListener(v -> open(WrongBookActivity.class));
        findViewById(R.id.history).setOnClickListener(v -> open(HistoryActivity.class));
        findViewById(R.id.settings).setOnClickListener(v -> open(SettingsActivity.class));
    }

    @Override protected void onResume() {
        super.onResume();
        new Thread(() -> {
            Map<String, Integer> counts = new QuestionRepository(this).categoryCounts();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                categories.removeAllViews();
                int total = 0;
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    total += entry.getValue();
                    Button button = new Button(this);
                    button.setText(getString(R.string.category_entry, entry.getKey(), entry.getValue()));
                    button.setOnClickListener(v -> startPractice(entry.getKey()));
                    categories.addView(button);
                }
                questionCount.setText(getString(R.string.question_count, total));
                startPractice.setEnabled(total > 0);
                findViewById(R.id.empty_categories).setVisibility(total == 0 ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void open(Class<?> destination) {
        startActivity(new Intent(this, destination));
    }

    private void startPractice(String category) {
        Intent intent = new Intent(this, PracticeActivity.class);
        intent.putExtra(AppContract.EXTRA_PRACTICE_MODE, AppContract.MODE_NORMAL);
        if (category != null) intent.putExtra(AppContract.EXTRA_CATEGORY, category);
        startActivity(intent);
    }
}
