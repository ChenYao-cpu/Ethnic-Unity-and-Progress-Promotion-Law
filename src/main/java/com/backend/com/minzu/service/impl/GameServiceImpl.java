package com.backend.com.minzu.service.impl;
import com.alibaba.fastjson.JSON; import com.backend.com.minzu.entity.GameLevelRecord; import com.backend.com.minzu.entity.Question; import com.backend.com.minzu.entity.User; import com.backend.com.minzu.entity.UserAchievement; import com.backend.com.minzu.mapper.GameLevelRecordMapper; import com.backend.com.minzu.mapper.QuestionMapper; import com.backend.com.minzu.mapper.UserAchievementMapper; import com.backend.com.minzu.mapper.UserMapper; import com.backend.com.minzu.service.GameService; import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.*; import java.util.stream.Collectors;
@Service public class GameServiceImpl implements GameService {
    @Autowired
    private GameLevelRecordMapper levelRecordMapper;
    
    @Autowired
    private UserAchievementMapper achievementMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    private static final Map<String, AchievementConfig> ACHIEVEMENTS = new HashMap<>();
    
    static {
        ACHIEVEMENTS.put("perfect_match", new AchievementConfig("perfect_match", "法条大神", "法条匹配首次完全正确", "icon-fatiao1"));
        ACHIEVEMENTS.put("speed_master", new AchievementConfig("speed_master", "闪电侠", "快速答题每题3秒内答对", "icon-shandianxia"));
        ACHIEVEMENTS.put("first_blood", new AchievementConfig("first_blood", "初出茅庐", "完成人生第一关", "icon-chuchumaolu"));
        ACHIEVEMENTS.put("rank_first", new AchievementConfig("rank_first", "冠军", "排行榜获得第一名", "icon-jifen1"));
        ACHIEVEMENTS.put("rank_second", new AchievementConfig("rank_second", "亚军", "排行榜获得第二名", "icon-face_happy"));
        ACHIEVEMENTS.put("rank_third", new AchievementConfig("rank_third", "季军", "排行榜获得第三名", "icon-face_smile"));
        ACHIEVEMENTS.put("ten_levels", new AchievementConfig("ten_levels", "闯关达人", "成功通关10个关卡", "icon-darenrenzheng"));
        ACHIEVEMENTS.put("score_1000", new AchievementConfig("score_1000", "积分收割机", "累计获得1000积分", "icon-star-ai"));
    }
    
    @Override
    public Map<String, Object> startMatchLevel(Long userId, Integer levelId) {
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getCategory, levelId)
                        .last("LIMIT 5")
        );
        
        if (questions.isEmpty()) {
            throw new RuntimeException("该关卡暂无题目");
        }
        
        List<Map<String, Object>> matchItems = new ArrayList<>();
        List<Map<String, Object>> matchOptions = new ArrayList<>();
        
        for (Question q : questions) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", q.getId());
            item.put("content", q.getTitle());
            matchItems.add(item);
            
            Map<String, Object> option = new HashMap<>();
            option.put("id", q.getId());
            String content = q.getAnalysis();
            if (content == null || content.trim().isEmpty()) {
                content = q.getTitle();
            }
            if (content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            option.put("content", content);
            matchOptions.add(option);
        }
        
        Collections.shuffle(matchOptions);
        
        Map<String, Object> result = new HashMap<>();
        result.put("levelId", levelId);
        result.put("items", matchItems);
        result.put("options", matchOptions);
        result.put("startTime", System.currentTimeMillis());
        
        return result;
    }
    
    @Override
    @Transactional
    public Map<String, Object> submitMatchLevel(Long userId, Integer levelId, String answers, Integer timeUsed) {
        Map<String, Object> answerMap = JSON.parseObject(answers, new com.alibaba.fastjson.TypeReference<Map<String, Object>>(){});
        
        int correct = 0;
        int total = answerMap.size();
        
        for (Map.Entry<String, Object> entry : answerMap.entrySet()) {
            Long itemId = Long.parseLong(entry.getKey());
            Long matchedId = Long.valueOf(entry.getValue().toString());
            
            if (itemId.equals(matchedId)) {
                correct++;
            }
        }
        
        int score = correct * 1;  // 每对1题得1分
        double accuracy = (double) correct / total * 100;
        int status = correct == total ? 2 : (correct > 0 ? 1 : 0);

        GameLevelRecord record = levelRecordMapper.selectOne(
                new LambdaQueryWrapper<GameLevelRecord>()
                        .eq(GameLevelRecord::getUserId, userId)
                        .eq(GameLevelRecord::getLevelId, levelId)
        );

        // 保存历史最佳分数，用于计算增量积分（必须在 setBestScore 之前获取）
        int previousBestScore = (record != null && record.getBestScore() != null) ? record.getBestScore() : 0;

        if (record == null) {
            record = new GameLevelRecord();
            record.setUserId(userId);
            record.setLevelId(levelId);
            record.setLevelType("match");
        }

        record.setScore(Math.max(score, previousBestScore));
        record.setBestScore(Math.max(score, previousBestScore));
        record.setTimeUsed(timeUsed);
        record.setAccuracy(accuracy);
        record.setStatus(status);
        record.setCompleteTime(new Date());

        if (record.getId() == null) {
            levelRecordMapper.insert(record);
        } else {
            levelRecordMapper.updateById(record);
        }

        checkAndUnlockAchievements(userId, correct, total, timeUsed, score);
        // 增量积分：只加超过历史最佳的部分
        // 每次提交都累加全部分数（重复玩重复加）
        addUserScore(userId, score);
        updateLeaderboard(userId);

        // 获取用户最新的总积分返回给前端
        User user = userMapper.selectById(userId);
        int newTotalScore = user != null && user.getTotalScore() != null ? user.getTotalScore() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("correct", correct);
        result.put("total", total);
        result.put("accuracy", accuracy);
        result.put("status", status);
        result.put("totalScore", newTotalScore);  // 用户最新总积分

        return result;
    }

    @Override
    public Map<String, Object> startQuickLevel(Long userId, Integer levelId) {
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .last("LIMIT 8")
        );
        
        List<Map<String, Object>> questionList = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("id", q.getId());
            qMap.put("title", q.getTitle());
            qMap.put("type", q.getType());
            qMap.put("options", q.getOptions());
            qMap.put("timeLimit", 7);
            questionList.add(qMap);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("levelId", levelId);
        result.put("questions", questionList);
        result.put("totalTime", 60);
        result.put("startTime", System.currentTimeMillis());
        
        return result;
    }
    
    @Override
    @Transactional
    public Map<String, Object> submitQuickLevel(Long userId, Integer levelId, String answers, Integer timeUsed) {
        Map<String, String> answerMap = JSON.parseObject(answers, new com.alibaba.fastjson.TypeReference<Map<String, String>>(){});
        
        int correct = 0;
        int total = answerMap.size();
        int fastCorrect = 0;
        
        for (Map.Entry<String, String> entry : answerMap.entrySet()) {
            Long questionId = Long.parseLong(entry.getKey());
            String userAnswer = entry.getValue();
            
            Question question = questionMapper.selectById(questionId);
            if (question != null && question.getAnswer().equals(userAnswer)) {
                correct++;
                if (timeUsed <= 7) {
                    fastCorrect++;
                }
            }
        }
        
        int score = correct * 1;  // 每对1题得1分
        double accuracy = (double) correct / total * 100;
        int status = correct == total ? 2 : (correct > 0 ? 1 : 0);

        GameLevelRecord record = levelRecordMapper.selectOne(
                new LambdaQueryWrapper<GameLevelRecord>()
                        .eq(GameLevelRecord::getUserId, userId)
                        .eq(GameLevelRecord::getLevelId, levelId)
        );

        // 保存历史最佳分数，用于计算增量积分（必须在 setBestScore 之前获取）
        int previousBestScore = (record != null && record.getBestScore() != null) ? record.getBestScore() : 0;

        if (record == null) {
            record = new GameLevelRecord();
            record.setUserId(userId);
            record.setLevelId(levelId);
            record.setLevelType("quick");
        }

        record.setScore(Math.max(score, previousBestScore));
        record.setBestScore(Math.max(score, previousBestScore));
        record.setTimeUsed(timeUsed);
        record.setAccuracy(accuracy);
        record.setStatus(status);
        record.setCompleteTime(new Date());

        if (record.getId() == null) {
            levelRecordMapper.insert(record);
        } else {
            levelRecordMapper.updateById(record);
        }

        checkAndUnlockAchievements(userId, correct, total, timeUsed, score);
        // 每次提交都累加全部分数（重复玩重复加）
        addUserScore(userId, score);
        updateLeaderboard(userId);

        User user = userMapper.selectById(userId);
        int newTotalScore = user != null && user.getTotalScore() != null ? user.getTotalScore() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("correct", correct);
        result.put("total", total);
        result.put("fastCorrect", fastCorrect);
        result.put("accuracy", accuracy);
        result.put("status", status);
        result.put("totalScore", newTotalScore);  // 用户最新总积分

        return result;
    }

    @Override
    public Map<String, Object> startScenarioLevel(Long userId, Integer levelId) {
        List<Question> allQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getType, 4)
                        .orderByAsc(Question::getId)
        );

        if (allQuestions.isEmpty()) {
            allQuestions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .ge(Question::getType, 1)
                            .le(Question::getType, 3)
                            .orderByAsc(Question::getId)
            );
        }

        List<Map<String, Object>> scenarioList = new ArrayList<>();
        int batchSize = 5;
        int totalBatches = (int) Math.ceil((double) allQuestions.size() / batchSize);

        Random random = new Random();
        int batchIndex = random.nextInt(totalBatches);

        int start = batchIndex * batchSize;
        int end = Math.min(start + batchSize, allQuestions.size());

        List<Question> questions = allQuestions.subList(start, end);

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Map<String, Object> scenario = new HashMap<>();
            scenario.put("id", q.getId());
            scenario.put("scenario", q.getTitle());

            String optionsJson = q.getOptions();
            if (optionsJson != null && !optionsJson.isEmpty() && !"null".equals(optionsJson)) {
                try {
                    List<String> optionsList = JSON.parseArray(optionsJson, String.class);
                    scenario.put("options", optionsList);
                } catch (Exception e) {
                    scenario.put("options", optionsJson);
                }
            } else {
                scenario.put("options", new String[]{"选项数据缺失", "请联系管理员"});
            }

            scenario.put("knowledgePoint", q.getKnowledgePoint());
            scenario.put("answer", q.getAnswer());
            scenario.put("analysis", q.getAnalysis());

            int globalIndex = start + i;
            int imageIndex = (globalIndex % 32) + 1;
            scenario.put("image", "/images/scenarios/scenario-" + imageIndex + ".png");

            scenarioList.add(scenario);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("levelId", levelId);
        result.put("scenarios", scenarioList);
        result.put("batchIndex", batchIndex);
        result.put("totalBatches", totalBatches);

        return result;
    }


    @Override
    @Transactional
    public Map<String, Object> submitScenarioLevel(Long userId, Integer levelId, String answers) {
        Map<String, String> answerMap = JSON.parseObject(answers, new com.alibaba.fastjson.TypeReference<Map<String, String>>(){});
        
        int correct = 0;
        int total = answerMap.size();
        
        List<Map<String, Object>> details = new ArrayList<>();

        for (Map.Entry<String, String> entry : answerMap.entrySet()) {
            Long questionId = Long.parseLong(entry.getKey());
            String userAnswer = entry.getValue();

            Question question = questionMapper.selectById(questionId);
            String correctAnswer = question != null ? question.getAnswer() : "";
            boolean isRight = correctAnswer.equals(userAnswer);
            if (isRight) correct++;

            Map<String, Object> detail = new HashMap<>();
            detail.put("questionId", questionId);
            detail.put("scenario", question != null ? question.getTitle() : "");
            detail.put("userAnswer", userAnswer);
            detail.put("correctAnswer", correctAnswer);
            detail.put("isCorrect", isRight);
            detail.put("analysis", question != null ? question.getAnalysis() : "");
            details.add(detail);
        }

        int score = correct * 1;  // 每对1题得1分
        double accuracy = (double) correct / total * 100;
        int status = correct == total ? 2 : (correct > 0 ? 1 : 0);

        GameLevelRecord record = levelRecordMapper.selectOne(
                new LambdaQueryWrapper<GameLevelRecord>()
                        .eq(GameLevelRecord::getUserId, userId)
                        .eq(GameLevelRecord::getLevelId, levelId)
        );

        // 保存历史最佳分数，用于计算增量积分（必须在 setBestScore 之前获取）
        int previousBestScore = (record != null && record.getBestScore() != null) ? record.getBestScore() : 0;

        if (record == null) {
            record = new GameLevelRecord();
            record.setUserId(userId);
            record.setLevelId(levelId);
            record.setLevelType("scenario");
        }

        record.setScore(Math.max(score, previousBestScore));
        record.setBestScore(Math.max(score, previousBestScore));
        record.setAccuracy(accuracy);
        record.setStatus(status);
        record.setCompleteTime(new Date());

        if (record.getId() == null) {
            levelRecordMapper.insert(record);
        } else {
            levelRecordMapper.updateById(record);
        }

        checkAndUnlockAchievements(userId, correct, total, 0, score);
        // 每次提交都累加全部分数（重复玩重复加）
        addUserScore(userId, score);
        updateLeaderboard(userId);

        User user = userMapper.selectById(userId);
        int newTotalScore = user != null && user.getTotalScore() != null ? user.getTotalScore() : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("correct", correct);
        result.put("total", total);
        result.put("accuracy", accuracy);
        result.put("status", status);
        result.put("details", details);
        result.put("totalScore", newTotalScore);  // 用户最新总积分

        return result;
    }

    @Override
    public List<Map<String, Object>> getUserAchievements(Long userId) {
        List<UserAchievement> achievements = achievementMapper.selectList(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
        );
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserAchievement ua : achievements) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", ua.getAchievementCode());
            item.put("name", ua.getAchievementName());
            item.put("desc", ua.getAchievementDesc());
            item.put("icon", ua.getIcon());
            item.put("unlocked", ua.getUnlocked());
            item.put("unlockTime", ua.getUnlockTime());
            result.add(item);
        }
        
        for (Map.Entry<String, AchievementConfig> entry : ACHIEVEMENTS.entrySet()) {
            boolean exists = result.stream()
                                     .anyMatch(r -> r.get("code").equals(entry.getKey()));
            
            if (!exists) {
                Map<String, Object> item = new HashMap<>();
                item.put("code", entry.getKey());
                item.put("name", entry.getValue().getName());
                item.put("desc", entry.getValue().getDesc());
                item.put("icon", entry.getValue().getIcon());
                item.put("unlocked", 0);
                result.add(item);
            }
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getLeaderboard(int limit) {
        // 直接从 user 表按 total_score 排名
        return userMapper.getLeaderboard(limit);
    }
    
    @Override
    public Map<String, Object> getUserGameStats(Long userId) {
        List<GameLevelRecord> records = levelRecordMapper.selectList(
                new LambdaQueryWrapper<GameLevelRecord>()
                        .eq(GameLevelRecord::getUserId, userId)
        );
        
        int totalScore = records.stream().mapToInt(r -> r.getScore() != null ? r.getScore() : 0).sum();
        int completedLevels = (int) records.stream().filter(r -> r.getStatus() > 0).count();
        int perfectLevels = (int) records.stream().filter(r -> r.getStatus() == 2).count();
        
        long unlockedAchievements = achievementMapper.selectCount(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
                        .eq(UserAchievement::getUnlocked, 1)
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", totalScore);
        result.put("completedLevels", completedLevels);
        result.put("perfectLevels", perfectLevels);
        result.put("unlockedAchievements", unlockedAchievements);
        result.put("records", records);
        
        return result;
    }
    
    private void checkAndUnlockAchievements(Long userId, int correct, int total, int timeUsed, int score) {
        // 1. 法条匹配首次完全正确
        if (correct == total && correct > 0) {
            unlockAchievement(userId, "perfect_match");
        }
        
        // 2. 快速答题每题3秒内答对
        if (correct == total && total > 0 && timeUsed > 0) {
            double avgTime = (double) timeUsed / total;
            if (avgTime <= 3) {
                unlockAchievement(userId, "speed_master");
            }
        }
        
        // 3. 完成人生第一关
        long completedCount = levelRecordMapper.selectCount(
                new LambdaQueryWrapper<GameLevelRecord>()
                        .eq(GameLevelRecord::getUserId, userId)
                        .gt(GameLevelRecord::getStatus, 0)
        );
        
        if (completedCount >= 1) {
            unlockAchievement(userId, "first_blood");
        }
        
        // 5. 通关10个关卡
        if (completedCount >= 10) {
            unlockAchievement(userId, "ten_levels");
        }
        
        // 6. 累计获得1000积分
        int totalScore = levelRecordMapper.selectList(
                new LambdaQueryWrapper<GameLevelRecord>()
                        .eq(GameLevelRecord::getUserId, userId)
        ).stream().mapToInt(r -> r.getScore() != null ? r.getScore() : 0).sum();
        
        if (totalScore >= 1000) {
            unlockAchievement(userId, "score_1000");
        }
        
        // 7. 检查排行榜排名成就
        checkRankAchievements(userId);
    }
    
    // 检查排行榜排名成就
    private void checkRankAchievements(Long userId) {
        try {
            List<Map<String, Object>> leaderboard = userMapper.getLeaderboard(3);
            
            if (leaderboard != null && !leaderboard.isEmpty()) {
                for (int i = 0; i < leaderboard.size(); i++) {
                    Map<String, Object> item = leaderboard.get(i);
                    Object userIdObj = item.get("userId");
                    if (userIdObj != null) {
                        Long itemUserId = ((Number) userIdObj).longValue();
                        
                        if (itemUserId.equals(userId)) {
                            if (i == 0) {
                                unlockAchievement(userId, "rank_first");
                            } else if (i == 1) {
                                unlockAchievement(userId, "rank_second");
                            } else if (i == 2) {
                                unlockAchievement(userId, "rank_third");
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void unlockAchievement(Long userId, String achievementCode) {
        UserAchievement existing = achievementMapper.selectOne(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
                        .eq(UserAchievement::getAchievementCode, achievementCode)
        );
        
        if (existing == null || existing.getUnlocked() == 0) {
            AchievementConfig config = ACHIEVEMENTS.get(achievementCode);
            if (config != null) {
                UserAchievement achievement = new UserAchievement();
                achievement.setUserId(userId);
                achievement.setAchievementCode(achievementCode);
                achievement.setAchievementName(config.getName());
                achievement.setAchievementDesc(config.getDesc());
                achievement.setIcon(config.getIcon());
                achievement.setUnlocked(1);
                achievement.setUnlockTime(new Date());
                
                if (existing == null) {
                    achievementMapper.insert(achievement);
                } else {
                    achievementMapper.updateById(achievement);
                }
            }
        }
    }
    
    private void updateLeaderboard(Long userId) {
        // 排行榜数据已在 user.total_score 中，不再从 game_level_record 覆盖
        // 各游戏提交方法会直接累加积分到 user.total_score
    }

    private void addUserScore(Long userId, int addScore) {
        if (addScore <= 0) return;
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setTotalScore((user.getTotalScore() != null ? user.getTotalScore() : 0) + addScore);
            userMapper.updateById(user);
        }
    }
    
    static class AchievementConfig {
        private String code;
        private String name;
        private String desc;
        private String icon;
        
        public AchievementConfig(String code, String name, String desc, String icon) {
            this.code = code;
            this.name = name;
            this.desc = desc;
            this.icon = icon;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDesc() { return desc; }
        public String getIcon() { return icon; }
    }
}