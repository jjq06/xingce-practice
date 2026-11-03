package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import com.example.myapplication.ui.HistoryActivity;
import com.example.myapplication.ui.ImportActivity;
import com.example.myapplication.ui.PracticeActivity;
import com.example.myapplication.ui.SettingsActivity;
import com.example.myapplication.ui.WrongBookActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.start_practice).setOnClickListener(v -> open(PracticeActivity.class));
        findViewById(R.id.import_questions).setOnClickListener(v -> open(ImportActivity.class));
        findViewById(R.id.wrong_book).setOnClickListener(v -> open(WrongBookActivity.class));
        findViewById(R.id.history).setOnClickListener(v -> open(HistoryActivity.class));
        findViewById(R.id.settings).setOnClickListener(v -> open(SettingsActivity.class));
    }

    private void open(Class<?> destination) {
        startActivity(new Intent(this, destination));
    }
}
