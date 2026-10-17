package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.StudyReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudyReportMapper extends BaseMapper<StudyReport> {

    @Select("SELECT * FROM study_report WHERE user_id = #{userId} AND report_type = #{type} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<StudyReport> getRecentReports(@Param("userId") Long userId, @Param("type") String type, @Param("limit") int limit);
}
