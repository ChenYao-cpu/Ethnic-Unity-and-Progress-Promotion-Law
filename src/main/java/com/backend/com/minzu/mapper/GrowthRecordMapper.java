package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.GrowthRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GrowthRecordMapper extends BaseMapper<GrowthRecord> {

    @Select("SELECT * FROM growth_record WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<GrowthRecord> getRecentGrowthRecords(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT SUM(change_value) FROM growth_record WHERE user_id = #{userId}")
    Integer getTotalGrowthValue(@Param("userId") Long userId);
}
