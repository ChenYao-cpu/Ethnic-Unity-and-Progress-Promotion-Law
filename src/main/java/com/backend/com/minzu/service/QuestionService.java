package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.Question;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.List;

public interface QuestionService {
    List<Question> getQuestionsByCategory(Integer category);
    Question getQuestionById(Long id);
    boolean checkAnswer(Long questionId, String userAnswer);

    // 添加通用的查询方法
    List<Question> list(LambdaQueryWrapper<Question> wrapper);
    
    List<Question> listByIds(List<Long> questionIds);
    
    boolean save(Question question);
}
