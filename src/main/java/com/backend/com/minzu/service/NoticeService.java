package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.Notice;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface NoticeService {
    List<Notice> getAllNotices();

    List<Notice> getLatestNotices(int limit);

    List<Notice> getNoticesByType(Integer type);

    Notice getNoticeById(Long id);

    Page<Notice> getNoticePage(int pageNum, int pageSize);

    boolean addNotice(Notice notice);

    boolean updateNotice(Notice notice);

    boolean deleteNotice(Long id);

    Map<String, Object> getNoticeDetail(Long noticeId);
}
