package com.backend.com.minzu.service.impl;


import com.alibaba.fastjson.JSON;
import com.backend.com.minzu.entity.Exam;
import com.backend.com.minzu.entity.Question;
import com.backend.com.minzu.entity.User;
import com.backend.com.minzu.mapper.ExamMapper;
import com.backend.com.minzu.mapper.QuestionMapper;
import com.backend.com.minzu.mapper.UserMapper;
import com.backend.com.minzu.service.ExamService;
import com.backend.com.minzu.service.WrongService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private ExamMapper examMapper;
    
    @Autowired
    private WrongService wrongService;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Question> generateExamPaper(Integer category, int size) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("ORDER BY RAND() LIMIT " + size);
        return questionMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public int submitExam(Long userId, List<Integer> questionIds, List<String> userAnswers) {
        int score = 0;
        for (int i = 0; i < questionIds.size(); i++) {
            Integer qid = questionIds.get(i);
            String userAns = userAnswers.get(i);
            Question q = questionMapper.selectById(qid);
            if (q != null && q.getAnswer().equalsIgnoreCase(userAns)) {
                score += 10;
            } else {
                wrongService.addWrong(userId, qid.longValue());
            }
        }
        Exam exam = new Exam();
        exam.setUserId(userId);
        exam.setScore(score);
        exam.setExamTime(new Date());
        examMapper.insert(exam);

        return score;
    }

    @Override
    @Transactional
    public Map<String, Object> startExam(Long userId, String type, int size) {
        List<Question> questions;
        
        if ("错题重考".equals(type)) {
            questions = getWrongQuestions(userId, size);
        } else {
            questions = generateExamPaper(null, size);
        }
        
        List<Long> questionIds = new ArrayList<>();
        for (Question q : questions) {
            questionIds.add(q.getId());
            q.setOptions(q.getOptions());
            q.setAnswer(null);
            q.setAnalysis(null);
        }
        
        Exam exam = new Exam();
        exam.setUserId(userId);
        exam.setTitle(type + "测试");
        exam.setType(type);
        exam.setQuestions(JSON.toJSONString(questionIds));
        exam.setStatus(0);
        exam.setStartTime(new Date());
        exam.setTotalScore(size * 5);
        examMapper.insert(exam);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", exam.getId());
        result.put("questions", questions);
        result.put("totalScore", exam.getTotalScore());
        
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(Long examId, String userAnswers, Integer timeUsed) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        
        List<Long> questionIds = JSON.parseArray(exam.getQuestions(), Long.class);
        Map<String, String> answersMap = JSON.parseObject(userAnswers, Map.class);
        
        int correctCount = 0;
        List<Map<String, Object>> questionResults = new ArrayList<>();
        
        for (Long qid : questionIds) {
            Question question = questionMapper.selectById(qid);
            String userAnswer = answersMap.getOrDefault(qid.toString(), "");
            boolean isCorrect = question.getAnswer().equalsIgnoreCase(userAnswer);
            
            if (isCorrect) {
                correctCount++;
            } else {
                wrongService.addWrong(exam.getUserId(), qid);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("questionId", qid);
            result.put("userAnswer", userAnswer);
            result.put("correctAnswer", question.getAnswer());
            result.put("isCorrect", isCorrect);
            result.put("analysis", question.getAnalysis());
            questionResults.add(result);
        }
        
        int score = correctCount * 5;
        exam.setUserAnswers(userAnswers);
        exam.setScore(score);
        exam.setCorrectCount(correctCount);
        exam.setTimeUsed(timeUsed);
        exam.setStatus(1);
        exam.setFinishTime(new Date());
        examMapper.updateById(exam);
        
        User user = userMapper.selectById(exam.getUserId());
        user.setTotalScore(user.getTotalScore() + score / 2);
        userMapper.updateById(user);
        
        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("totalScore", exam.getTotalScore());
        result.put("correctCount", correctCount);
        result.put("totalCount", questionIds.size());
        result.put("timeUsed", timeUsed);
        result.put("accuracy", (correctCount * 100.0 / questionIds.size()));
        result.put("questions", questionResults);
        
        return result;
    }

    @Override
    public List<Exam> getExamHistory(Long userId) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Exam::getUserId, userId);
        wrapper.eq(Exam::getStatus, 1);
        wrapper.orderByDesc(Exam::getFinishTime);
        wrapper.last("LIMIT 20");
        return examMapper.selectList(wrapper);
    }

    private List<Question> getWrongQuestions(Long userId, int size) {
        List<Long> wrongQuestionIds = wrongService.getWrongQuestionIds(userId);
        if (wrongQuestionIds.isEmpty()) {
            return generateExamPaper(null, size);
        }
        
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Question::getId, wrongQuestionIds);
        wrapper.last("ORDER BY RAND() LIMIT " + size);
        return questionMapper.selectList(wrapper);
    }
}
