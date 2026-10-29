package com.example.myapplication.practice;

import com.example.myapplication.model.Question;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PracticeSession {
    public final List<Question> questions;
    public final long startedAt = System.currentTimeMillis();
    public final Map<Long, String> answers = new HashMap<>();
    public int currentIndex;
    public boolean submitted;

    public PracticeSession(List<Question> questions) {
        this.questions = questions;
    }

    public int unansweredCount() {
        return questions.size() - answers.size();
    }
}
