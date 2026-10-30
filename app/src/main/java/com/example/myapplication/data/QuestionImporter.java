package com.example.myapplication.data;

import com.example.myapplication.model.Question;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestionImporter {
    public static class ParseResult {
        public final List<Question> questions = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
    }

    /** 严格解码：错误编码会抛出 IOException，不产生可入库的部分结果。关闭输入流。 */
    public ParseResult parseUtf8(InputStream input, String source) throws IOException {
        return parse(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)), source);
    }

    public ParseResult parse(Reader input, String source) throws IOException {
        ParseResult result = new ParseResult();
        try (BufferedReader reader = new BufferedReader(input)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.trim().isEmpty()) continue;
                String[] fields = line.split("\t", -1);
                if (fields.length != 8) {
                    result.errors.add("第 " + lineNumber + " 行：需要 8 个制表符分隔字段");
                    continue;
                }
                String[] labels = {"类别", "题干", "选项A", "选项B", "选项C", "选项D", "答案"};
                List<String> reasons = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    if (fields[i].trim().isEmpty()) reasons.add(labels[i] + "不能为空");
                }
                String answer = fields[6].trim().toUpperCase(Locale.ROOT);
                if (!answer.isEmpty() && !answer.matches("[ABCD]")) reasons.add("答案必须是 A、B、C、D");
                if (!reasons.isEmpty()) {
                    result.errors.add("第 " + lineNumber + " 行：" + String.join("；", reasons));
                    continue;
                }
                Question question = new Question();
                question.category = fields[0].trim();
                question.stem = fields[1].trim();
                question.optionA = fields[2].trim();
                question.optionB = fields[3].trim();
                question.optionC = fields[4].trim();
                question.optionD = fields[5].trim();
                question.answer = answer;
                question.explanation = fields[7].trim();
                question.source = source;
                question.fingerprint = fingerprint(question);
                result.questions.add(question);
            }
        }
        return result;
    }

    public static String fingerprint(Question q) {
        String value = String.join("\u001f", q.category, q.stem, q.optionA, q.optionB, q.optionC, q.optionD);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
