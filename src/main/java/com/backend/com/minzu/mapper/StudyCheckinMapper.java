package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.StudyCheckin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudyCheckinMapper extends BaseMapper<StudyCheckin> {

    @Select("SELECT MAX(continuous_days) FROM study_checkin WHERE user_id = #{userId}")
    Integer getMaxContinuousDays(@Param("userId") Long userId);

    @Select("SELECT checkin_date, continuous_days, growth_value FROM study_checkin " +
            "WHERE user_id = #{userId} AND checkin_date >= #{startDate} " +
            "ORDER BY checkin_date ASC")
    List<Map<String, Object>> getCheckinCalendar(@Param("userId") Long userId, @Param("startDate") Date startDate);

    @Select("SELECT COUNT(DISTINCT checkin_date) FROM study_checkin " +
            "WHERE user_id = #{userId} AND checkin_date >= #{startDate}")
    Integer getActiveDays(@Param("userId") Long userId, @Param("startDate") Date startDate);
}
