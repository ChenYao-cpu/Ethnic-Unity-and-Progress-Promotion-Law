package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.WrongQuestion;

import java.util.List;

public interface WrongService {
    void addWrong(Long userId, Long questionId);
    List<WrongQuestion> getUserWrongs(Long userId);
    void removeWrong(Long id);
    List<Long> getWrongQuestionIds(Long userId);
}
