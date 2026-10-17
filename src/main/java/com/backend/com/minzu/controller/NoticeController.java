package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Notice;
import com.backend.com.minzu.service.NoticeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping("/list")
    public Result<List<Notice>> list() {
        List<Notice> list = noticeService.getAllNotices();
        return Result.success(list);
    }

    @GetMapping("/latest")
    public Result<List<Notice>> latest(@RequestParam(defaultValue = "5") int limit) {
        List<Notice> list = noticeService.getLatestNotices(limit);
        return Result.success(list);
    }

    @GetMapping("/type/{type}")
    public Result<List<Notice>> getByType(@PathVariable Integer type) {
        List<Notice> list = noticeService.getNoticesByType(type);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> detail = noticeService.getNoticeDetail(id);
        if (detail != null) {
            return Result.success(detail);
        } else {
            return Result.error("公告不存在");
        }
    }

    @GetMapping("/page")
    public Result<Page<Notice>> getPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Notice> page = noticeService.getNoticePage(pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Notice notice) {
        boolean success = noticeService.addNotice(notice);
        if (success) {
            return Result.success("添加成功");
        } else {
            return Result.error("添加失败");
        }
    }

    @PutMapping("/update")
    public Result<?> update(@RequestBody Notice notice) {
        boolean success = noticeService.updateNotice(notice);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        boolean success = noticeService.deleteNotice(id);
        return Result.success(success ? "删除成功" : "删除失败");
    }
}
