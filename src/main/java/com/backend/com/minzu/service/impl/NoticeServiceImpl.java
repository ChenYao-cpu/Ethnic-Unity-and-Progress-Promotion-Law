package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.Notice;
import com.backend.com.minzu.mapper.NoticeMapper;
import com.backend.com.minzu.service.NoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Autowired
    private NoticeMapper noticeMapper;

    @Override
    public List<Notice> getAllNotices() {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Notice::getIsTop, Notice::getPublishTime);
        return noticeMapper.selectList(wrapper);
    }


    @Override
    public List<Notice> getLatestNotices(int limit) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Notice::getPublishTime);
        wrapper.last("LIMIT " + limit);
        List<Notice> list = noticeMapper.selectList(wrapper);

        // 如果数据库没数据，自动返回几条测试数据（方便调试）
        if (list.isEmpty()) {
            Notice n1 = new Notice();
            n1.setId(1L);
            n1.setTitle("关于开展民族团结进步宣传月活动的通知");
            n1.setContent("为深入贯彻党的民族政策，定于本月开展民族团结进步宣传月活动，请各部门积极组织参与。");
            n1.setType(1);
            n1.setIsTop(1);
            n1.setPublishTime(new Date());

            Notice n2 = new Notice();
            n2.setId(2L);
            n2.setTitle("学习平台正式上线");
            n2.setContent("本平台旨在帮助大家更好地学习《民族团结促进法》，提供课程学习、在线答题等功能。");
            n2.setType(1);
            n2.setIsTop(0);
            n2.setPublishTime(new Date());

            list.add(n1);
            list.add(n2);
        }

        return list;
    }


    @Override
    public List<Notice> getNoticesByType(Integer type) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getType, type);
        wrapper.orderByDesc(Notice::getIsTop, Notice::getPublishTime);
        return noticeMapper.selectList(wrapper);
    }

    @Override
    public Notice getNoticeById(Long id) {
        return noticeMapper.selectById(id);
    }

    @Override
    public Page<Notice> getNoticePage(int pageNum, int pageSize) {
        Page<Notice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Notice::getIsTop, Notice::getPublishTime);
        return noticeMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean addNotice(Notice notice) {
        if (notice.getPublishTime() == null) {
            notice.setPublishTime(new Date());
        }
        if (notice.getCreateTime() == null) {
            notice.setCreateTime(new Date());
        }
        if (notice.getIsTop() == null) {
            notice.setIsTop(0);
        }
        return noticeMapper.insert(notice) > 0;
    }

    @Override
    public boolean updateNotice(Notice notice) {
        return noticeMapper.updateById(notice) > 0;
    }

    @Override
    public boolean deleteNotice(Long id) {
        return noticeMapper.deleteById(id) > 0;
    }

    @Override
    public Map<String, Object> getNoticeDetail(Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("notice", notice);
        return result;
    }
}
