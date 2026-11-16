package com.example.myapplication.ui;

import android.app.Application;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.myapplication.data.QuestionImporter;
import com.example.myapplication.data.QuestionRepository;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 旋转屏幕后继续同一次导入；Activity 只观察结果，不持有后台任务。 */
public class ImportViewModel extends AndroidViewModel {
    public static class State {
        final boolean busy;
        final String message;
        State(boolean busy, String message) { this.busy = busy; this.message = message; }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false,
            "请选择 UTF-8 编码的 .txt 文件。合法题会导入，错误行会列出原因，重复题会跳过。"));
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public LiveData<State> state() { return state; }

    public ImportViewModel(@NonNull Application application) { super(application); }

    public void importFile(Uri uri) {
        if (state.getValue() != null && state.getValue().busy) return;
        state.setValue(new State(true, "正在读取并导入，请稍候…"));
        executor.execute(() -> {
            String message;
            try {
                String name = fileName(uri);
                if (!name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                    throw new IllegalArgumentException("仅支持 .txt 文件，请按模板保存为 UTF-8 文本");
                }
                try (InputStream stream = getApplication().getContentResolver().openInputStream(uri)) {
                    if (stream == null) throw new IllegalArgumentException("无法打开所选文件");
                    // 禁止用替换字符静默吞掉 GBK 等非 UTF-8 字节。解码失败时不写库。
                    QuestionImporter.ParseResult parsed = new QuestionImporter().parseUtf8(stream, "用户导入：" + name);
                    int inserted = new QuestionRepository(getApplication()).insertAll(parsed.questions);
                    message = "文件：" + name + "\n新增 " + inserted + " 题；重复跳过 "
                            + (parsed.questions.size() - inserted) + " 题；格式错误 " + parsed.errors.size() + " 行";
                    if (parsed.questions.isEmpty() && parsed.errors.isEmpty()) message += "\n文件为空，没有可导入的题目。";
                    if (!parsed.errors.isEmpty()) message += "\n\n" + String.join("\n", parsed.errors);
                }
            } catch (java.nio.charset.CharacterCodingException e) {
                message = "导入失败：文件不是有效的 UTF-8 编码，请转换编码后重试。本次未写入题目。";
            } catch (Exception e) {
                message = "导入失败：" + (e.getMessage() == null ? "无法读取文件，请重新选择" : e.getMessage());
            }
            state.postValue(new State(false, message));
        });
    }

    private String fileName(Uri uri) {
        try (Cursor cursor = getApplication().getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null) return name;
            }
        }
        String path = uri.getLastPathSegment();
        return path == null ? "未知文件" : path;
    }

    @Override protected void onCleared() { executor.shutdown(); }
}
