package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id AS userId, username AS nickname, total_score AS totalScore, avatar, school " +
            "FROM user " +
            "ORDER BY total_score DESC LIMIT #{limit}")
    List<Map<String, Object>> getLeaderboard(int limit);
}