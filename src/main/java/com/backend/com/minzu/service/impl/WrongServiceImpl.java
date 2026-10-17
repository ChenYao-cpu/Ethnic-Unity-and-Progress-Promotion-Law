package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.WrongQuestion;
import com.backend.com.minzu.mapper.WrongMapper;
import com.backend.com.minzu.service.WrongService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WrongServiceImpl implements WrongService {

    @Autowired
    private WrongMapper wrongMapper;

    @Override
    public void addWrong(Long userId, Long questionId) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId);
        
        WrongQuestion existWrong = wrongMapper.selectOne(wrapper);
        
        if (existWrong != null) {
            existWrong.setWrongCount(existWrong.getWrongCount() + 1);
            existWrong.setLastWrongTime(new Date());
            wrongMapper.updateById(existWrong);
        } else {
            WrongQuestion wq = new WrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setWrongCount(1);
            wq.setLastWrongTime(new Date());
            wrongMapper.insert(wq);
        }
    }

    @Override
    public List<WrongQuestion> getUserWrongs(Long userId) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId);
        wrapper.orderByDesc(WrongQuestion::getLastWrongTime);
        return wrongMapper.selectList(wrapper);
    }

    @Override
    public void removeWrong(Long id) {
        wrongMapper.deleteById(id);
    }

    @Override
    public List<Long> getWrongQuestionIds(Long userId) {
        List<WrongQuestion> wrongs = getUserWrongs(userId);
        return wrongs.stream()
                .map(WrongQuestion::getQuestionId)
                .collect(Collectors.toList());
    }
}
