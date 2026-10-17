package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/match/start")
    public Result startMatchLevel(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Integer levelId = Integer.parseInt(params.get("levelId").toString());
            Map<String, Object> data = gameService.startMatchLevel(userId, levelId);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/match/submit")
    public Result submitMatchLevel(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Integer levelId = Integer.parseInt(params.get("levelId").toString());
            String answers = params.get("answers").toString();
            Integer timeUsed = Integer.parseInt(params.get("timeUsed").toString());
            Map<String, Object> data = gameService.submitMatchLevel(userId, levelId, answers, timeUsed);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/quick/start")
    public Result startQuickLevel(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Integer levelId = Integer.parseInt(params.get("levelId").toString());
            Map<String, Object> data = gameService.startQuickLevel(userId, levelId);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/quick/submit")
    public Result submitQuickLevel(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Integer levelId = Integer.parseInt(params.get("levelId").toString());
            String answers = params.get("answers").toString();
            Integer timeUsed = Integer.parseInt(params.get("timeUsed").toString());
            Map<String, Object> data = gameService.submitQuickLevel(userId, levelId, answers, timeUsed);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/scenario/start")
    public Result startScenarioLevel(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Integer levelId = Integer.parseInt(params.get("levelId").toString());
            Map<String, Object> data = gameService.startScenarioLevel(userId, levelId);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/scenario/submit")
    public Result submitScenarioLevel(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Integer levelId = Integer.parseInt(params.get("levelId").toString());
            String answers = params.get("answers").toString();
            Map<String, Object> data = gameService.submitScenarioLevel(userId, levelId, answers);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/achievements")
    public Result getUserAchievements(@RequestParam Long userId) {
        try {
            List<Map<String, Object>> data = gameService.getUserAchievements(userId);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/leaderboard")
    public Result getLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> data = gameService.getLeaderboard(limit);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result getUserGameStats(@RequestParam Long userId) {
        try {
            Map<String, Object> data = gameService.getUserGameStats(userId);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
