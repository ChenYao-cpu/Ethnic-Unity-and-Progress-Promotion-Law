package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Question;
import com.backend.com.minzu.entity.UserAnswer;
import com.backend.com.minzu.mapper.UserAnswerMapper;
import com.backend.com.minzu.service.LearningTokenService;
import com.backend.com.minzu.service.QuestionService;
import com.backend.com.minzu.service.WrongService;
import com.backend.com.minzu.service.StudyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/question")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private WrongService wrongService;
    
    @Autowired
    private StudyService studyService;

    @Autowired
    private UserAnswerMapper userAnswerMapper;

    @Autowired
    private LearningTokenService learningTokens;
    
    @GetMapping("/list")
    public Result<List<Question>> list(@RequestParam(required = false) Integer category,
                                       @RequestParam(required = false) Integer type,
                                       @RequestParam(required = false, defaultValue = "50") Integer limit) {
        try {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            
            if (category != null) {
                wrapper.eq(Question::getCategory, category);
            }
            if (type != null) {
                wrapper.eq(Question::getType, type);
            }
            
            wrapper.last("LIMIT " + limit);
            
            List<Question> list = questionService.list(wrapper);
            
            System.out.println("题库查询 - type: " + type + ", 返回数量: " + list.size());
            
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取题目列表失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/create")
    public Result<Question> createQuestion(@RequestBody Map<String, Object> params) {
        try {
            System.out.println("收到保存题目请求: " + params);
            
            Question question = new Question();
            
            // 设置标题
            if (params.get("title") != null) {
                question.setTitle(params.get("title").toString());
            }
            
            // 设置选项
            if (params.get("options") != null) {
                question.setOptions(params.get("options").toString());
            }
            
            // 设置答案
            if (params.get("answer") != null) {
                question.setAnswer(params.get("answer").toString());
            }
            
            // 设置解析
            if (params.get("analysis") != null) {
                question.setAnalysis(params.get("analysis").toString());
            }
            
            // 设置题目类型：兼容 type 和 questionType 两种字段名
            Integer typeValue = null;
            if (params.get("type") != null) {
                typeValue = Integer.parseInt(params.get("type").toString());
            } else if (params.get("questionType") != null) {
                typeValue = Integer.parseInt(params.get("questionType").toString());
            }
            if (typeValue != null) {
                question.setType(typeValue);
            } else {
                question.setType(1);
            }
            
            // 设置难度
            if (params.get("difficulty") != null) {
                question.setDifficulty(Integer.parseInt(params.get("difficulty").toString()));
            } else {
                question.setDifficulty(2);
            }
            
            // 设置教师ID
            if (params.get("teacherId") != null) {
                question.setTeacherId(Long.parseLong(params.get("teacherId").toString()));
            }
            
            question.setCreateTime(new Date());
            
            boolean saved = questionService.save(question);
            
            if (saved) {
                System.out.println("题目保存成功，ID: " + question.getId());
                return Result.success(question);
            } else {
                return Result.error("保存题目失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存题目失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/answer")
    @Transactional
    public Result<Map<String, Object>> answer(@RequestParam Long userId,
                                              @RequestParam Long questionId,
                                              @RequestParam String userAnswer,
                                              @RequestHeader(value = "X-Learning-Token", required = false) String token) {
        // 登录令牌与请求账号不一致时拒绝，避免把 A 账号的作答写进 B 账号
        Long tokenUser = learningTokens.user(token);
        if (tokenUser != null && !tokenUser.equals(userId)) {
            return Result.error("登录状态与账号不一致，请重新登录");
        }

        boolean correct = questionService.checkAnswer(questionId, userAnswer);

        // 每个账号的作答按 (userId, questionId) 独立保存：已有则更新，没有才新增，避免重复堆积
        LambdaQueryWrapper<UserAnswer> uaWrapper = new LambdaQueryWrapper<>();
        uaWrapper.eq(UserAnswer::getUserId, userId).eq(UserAnswer::getQuestionId, questionId);
        UserAnswer record = userAnswerMapper.selectOne(uaWrapper);
        if (record == null) {
            record = new UserAnswer();
            record.setUserId(userId);
            record.setQuestionId(questionId);
            record.setUserAnswer(userAnswer);
            record.setIsCorrect(correct ? 1 : 0);
            record.setAnswerTime(new Date());
            userAnswerMapper.insert(record);
        } else {
            record.setUserAnswer(userAnswer);
            record.setIsCorrect(correct ? 1 : 0);
            record.setAnswerTime(new Date());
            userAnswerMapper.updateById(record);
        }

        if (!correct) {
            wrongService.addWrong(userId, questionId);
        } else {
            studyService.updateStudyProfile(userId);
            studyService.addGrowthRecordPublic(userId, 5, "answer_correct", "答对题目获得成长值");
        }

        Question q = questionService.getQuestionById(questionId);
        Map<String, Object> data = new HashMap<>();
        data.put("correct", correct);
        data.put("analysis", q != null ? q.getAnalysis() : "");

        return Result.success(data);
    }
}