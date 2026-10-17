package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.Reminder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {

    @Select("SELECT * FROM reminder WHERE to_user_id = #{userId} ORDER BY create_time DESC")
    List<Reminder> getByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM reminder WHERE to_user_id = #{userId} AND is_read = 0 ORDER BY create_time DESC")
    List<Reminder> getUnreadByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM reminder WHERE to_user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);
}
