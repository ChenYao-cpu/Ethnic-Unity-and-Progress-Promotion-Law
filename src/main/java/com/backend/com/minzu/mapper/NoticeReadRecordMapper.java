package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.NoticeReadRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeReadRecordMapper extends BaseMapper<NoticeReadRecord> {

    @Select("SELECT nrr.*, u.nickname, u.avatar FROM notice_read_record nrr " +
            "LEFT JOIN user u ON nrr.user_id = u.id " +
            "WHERE nrr.notice_id = #{noticeId} ORDER BY nrr.read_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getNoticeReaders(@Param("noticeId") Long noticeId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM notice_read_record WHERE notice_id = #{noticeId}")
    Integer getViewCount(@Param("noticeId") Long noticeId);

    @Select("SELECT COUNT(*) FROM notice_read_record WHERE notice_id = #{noticeId} AND is_liked = 1")
    Integer getLikeCount(@Param("noticeId") Long noticeId);
}
