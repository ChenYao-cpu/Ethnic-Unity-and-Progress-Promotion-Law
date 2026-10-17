package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.GameLevelRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface GameLevelRecordMapper extends BaseMapper<GameLevelRecord> {

    @Select("SELECT l.*, u.nickname, u.avatar FROM leaderboard l " +
            "LEFT JOIN user u ON l.user_id = u.id " +
            "ORDER BY l.total_score DESC LIMIT #{limit}")
    List<Map<String, Object>> getLeaderboard(int limit);
}
