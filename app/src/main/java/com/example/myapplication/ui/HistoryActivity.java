package com.example.myapplication.ui;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.AppContract;
import com.example.myapplication.R;
import com.example.myapplication.data.PracticeRepository;
import com.example.myapplication.model.PracticeRecord;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_history);
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
                DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
                for (PracticeRecord record : records) {
                    Button row = new Button(this);
                    int percent = record.questionCount == 0 ? 0 : record.correctCount * 100 / record.questionCount;
                    row.setText(getString(R.string.history_entry, formatter.format(new Date(record.submittedAt)),
                            record.category, record.score, record.correctCount, record.questionCount, percent,
                            record.durationSeconds));
                    row.setOnClickListener(v -> {
                        Intent intent = new Intent(this, ResultActivity.class);
                        intent.putExtra(AppContract.EXTRA_RECORD_ID, record.id);
                        startActivity(intent);
                    });
                    list.addView(row);
                }
            });
        }).start();
    }
}
