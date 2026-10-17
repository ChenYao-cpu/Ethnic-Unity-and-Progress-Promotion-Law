package com.backend.com.minzu.service;

import java.util.List;
import java.util.Map;

public interface GameService {

    Map<String, Object> startMatchLevel(Long userId, Integer levelId);

    Map<String, Object> submitMatchLevel(Long userId, Integer levelId, String answers, Integer timeUsed);

    Map<String, Object> startQuickLevel(Long userId, Integer levelId);

    Map<String, Object> submitQuickLevel(Long userId, Integer levelId, String answers, Integer timeUsed);

    Map<String, Object> startScenarioLevel(Long userId, Integer levelId);

    Map<String, Object> submitScenarioLevel(Long userId, Integer levelId, String answers);

    List<Map<String, Object>> getUserAchievements(Long userId);

    List<Map<String, Object>> getLeaderboard(int limit);

    Map<String, Object> getUserGameStats(Long userId);
}
