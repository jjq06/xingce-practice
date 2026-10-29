package com.example.myapplication.data;

import com.example.myapplication.model.Question;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
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

    public ParseResult parse(Reader input, String source) throws IOException {
        ParseResult result = new ParseResult();
        try (BufferedReader reader = new BufferedReader(input)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                String[] fields = line.split("\t", -1);
                if (fields.length != 8) {
                    result.errors.add("第 " + lineNumber + " 行：需要 8 个制表符分隔字段");
                    continue;
                }
                boolean empty = false;
                for (int i = 0; i < 7; i++) empty |= fields[i].trim().isEmpty();
                String answer = fields[6].trim().toUpperCase(Locale.ROOT);
                if (empty || !answer.matches("[ABCD]")) {
                    result.errors.add("第 " + lineNumber + " 行：必填字段为空或答案不是 A-D");
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

    private String fingerprint(Question q) {
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
