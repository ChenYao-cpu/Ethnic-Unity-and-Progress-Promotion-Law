package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.Question;
import com.backend.com.minzu.mapper.QuestionMapper;
import com.backend.com.minzu.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Override
    public List<Question> getQuestionsByCategory(Integer category) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (category != null) {
            wrapper.eq(Question::getCategory, category);
        }
        return questionMapper.selectList(wrapper);
    }
    
    @Override
    public Question getQuestionById(Long id) {
        return questionMapper.selectById(id);
    }
    
    @Override
    public boolean checkAnswer(Long questionId, String userAnswer) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            return false;
        }
        return question.getAnswer().equals(userAnswer);
    }
    
    @Override
    public List<Question> list(LambdaQueryWrapper<Question> wrapper) {
        return questionMapper.selectList(wrapper);
    }
    
    @Override
    public List<Question> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return questionMapper.selectBatchIds(ids);
    }
    
    @Override
    public boolean save(Question question) {
        // 修改这里：返回 true 表示插入成功
        return questionMapper.insert(question) > 0;
    }
}