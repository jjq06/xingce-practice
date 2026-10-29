package com.example.myapplication.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

public class ResultActivity extends AppCompatActivity {
    public static final String EXTRA_RECORD_ID = "record_id";
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_result);
    }
}
