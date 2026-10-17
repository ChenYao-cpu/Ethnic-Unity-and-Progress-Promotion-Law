package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.AiChat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiChatMapper extends BaseMapper<AiChat> {

    @Select("SELECT * FROM ai_chat WHERE user_id = #{userId} ORDER BY create_time ASC LIMIT #{limit}")
    List<AiChat> getRecentHistory(Long userId, int limit);
}
