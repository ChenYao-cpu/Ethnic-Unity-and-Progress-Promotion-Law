package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.User;
import com.backend.com.minzu.mapper.UserMapper;
import com.backend.com.minzu.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("password", password);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public boolean register(User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            return false;
        }
        user.setCreateTime(new Date());
        // 如果前端没传 role，默认给 party_member（党员）
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("party_member");
        }
        return userMapper.insert(user) > 0;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean updateUser(User user) {
        return userMapper.updateById(user) > 0;
    }
}
