package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.StudyRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudyRecordMapper extends BaseMapper<StudyRecord> {

    @Select("SELECT SUM(study_duration) as total FROM study_record WHERE user_id = #{userId}")
    Integer getTotalStudyTime(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT course_id) as count FROM study_record WHERE user_id = #{userId}")
    Integer getStudiedCourseCount(@Param("userId") Long userId);

    @Select("SELECT DATE(study_time) as date, SUM(study_duration) as duration " +
            "FROM study_record WHERE user_id = #{userId} " +
            "GROUP BY DATE(study_time) ORDER BY date DESC LIMIT #{days}")
    List<Map<String, Object>> getStudyStats(@Param("userId") Long userId, @Param("days") int days);

    @Select("SELECT sr.*, c.title as course_title FROM study_record sr " +
            "LEFT JOIN course c ON sr.course_id = c.id " +
            "WHERE sr.user_id = #{userId} ORDER BY sr.study_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getRecentRecords(@Param("userId") Long userId, @Param("limit") int limit);
}
