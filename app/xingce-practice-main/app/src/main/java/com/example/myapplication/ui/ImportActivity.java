package com.example.myapplication.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.data.QuestionImporter;
import com.example.myapplication.data.QuestionRepository;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ImportActivity extends AppCompatActivity {
    private TextView status;
    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
                try (InputStreamReader reader = new InputStreamReader(
                        getContentResolver().openInputStream(result.getData().getData()), StandardCharsets.UTF_8)) {
                    QuestionImporter.ParseResult parsed = new QuestionImporter().parse(reader, "用户导入");
                    int inserted = new QuestionRepository(this).insertAll(parsed.questions);
                    status.setText("新增 " + inserted + " 题；格式错误 " + parsed.errors.size() + " 行\n" + String.join("\n", parsed.errors));
                } catch (Exception e) {
                    status.setText("导入失败：" + e.getMessage());
                }
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_import);
        status = findViewById(R.id.import_status);
        findViewById(R.id.pick_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            picker.launch(intent);
        });
    }
}
