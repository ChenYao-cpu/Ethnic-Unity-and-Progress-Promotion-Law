package com.backend.com.minzu.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    Object askQuestion(Long userId, String question);
    Object getChatHistory(Long userId);
    Object interpretLaw(Long userId, String keyword);
    Object askRAG(Long userId, String question);
    Object askFineTuned(Long userId, String question);
    List<Map<String, Object>> generateQuestions(Long userId, Integer questionCount, Integer questionType, Integer difficulty);
}