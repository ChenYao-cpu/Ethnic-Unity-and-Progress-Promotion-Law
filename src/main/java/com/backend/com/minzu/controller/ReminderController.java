package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Reminder;
import com.backend.com.minzu.mapper.ReminderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reminder")
public class ReminderController {

    @Autowired
    private ReminderMapper reminderMapper;

    /**
     * 管理员催促党员完成作业
     */
    @PostMapping("/send")
    public Result<String> sendReminder(@RequestBody Map<String, Object> params) {
        try {
            Long assignmentId = Long.parseLong(params.get("assignmentId").toString());
            Long fromUserId = Long.parseLong(params.get("fromUserId").toString());
            Long toUserId = Long.parseLong(params.get("toUserId").toString());
            String message = params.get("message") != null ? params.get("message").toString() : "请尽快完成作业！";

            Reminder reminder = new Reminder();
            reminder.setAssignmentId(assignmentId);
            reminder.setFromUserId(fromUserId);
            reminder.setToUserId(toUserId);
            reminder.setMessage(message);
            reminder.setIsRead(0);
            reminder.setCreateTime(new Date());

            reminderMapper.insert(reminder);
            System.out.println("催促提醒已发送 - 作业ID: " + assignmentId + ", 党员ID: " + toUserId + ", 消息: " + message);
            return Result.success("提醒已发送");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 批量催促（对某作业所有未完成的党员）
     */
    @PostMapping("/send-batch")
    public Result<Map<String, Object>> sendBatchReminder(@RequestBody Map<String, Object> params) {
        try {
            Long assignmentId = Long.parseLong(params.get("assignmentId").toString());
            Long fromUserId = Long.parseLong(params.get("fromUserId").toString());
            String message = params.get("message") != null ? params.get("message").toString() : "请尽快完成作业！";

            @SuppressWarnings("unchecked")
            List<Integer> toUserIdsRaw = (List<Integer>) params.get("toUserIds");
            List<Long> toUserIds = new ArrayList<>();
            for (Integer id : toUserIdsRaw) {
                toUserIds.add(id.longValue());
            }

            int count = 0;
            for (Long toUserId : toUserIds) {
                Reminder reminder = new Reminder();
                reminder.setAssignmentId(assignmentId);
                reminder.setFromUserId(fromUserId);
                reminder.setToUserId(toUserId);
                reminder.setMessage(message);
                reminder.setIsRead(0);
                reminder.setCreateTime(new Date());
                reminderMapper.insert(reminder);
                count++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量发送失败: " + e.getMessage());
        }
    }

    /**
     * 党员获取自己的提醒列表
     */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyReminders(@RequestParam Long userId) {
        try {
            List<Reminder> reminders = reminderMapper.getByUserId(userId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Reminder r : reminders) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", r.getId());
                item.put("assignmentId", r.getAssignmentId());
                item.put("fromUserId", r.getFromUserId());
                item.put("message", r.getMessage());
                item.put("isRead", r.getIsRead());
                item.put("createTime", r.getCreateTime());
                result.add(item);
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取提醒失败: " + e.getMessage());
        }
    }

    /**
     * 获取未读提醒数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount(@RequestParam Long userId) {
        try {
            int count = reminderMapper.countUnread(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取未读数失败: " + e.getMessage());
        }
    }

    /**
     * 标记提醒为已读
     */
    @PostMapping("/{id}/read")
    public Result<String> markAsRead(@PathVariable Long id) {
        try {
            Reminder reminder = reminderMapper.selectById(id);
            if (reminder != null) {
                reminder.setIsRead(1);
                reminderMapper.updateById(reminder);
            }
            return Result.success("已标记为已读");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 全部标记为已读
     */
    @PostMapping("/read-all")
    public Result<String> markAllAsRead(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            List<Reminder> unreadList = reminderMapper.getUnreadByUserId(userId);
            for (Reminder r : unreadList) {
                r.setIsRead(1);
                reminderMapper.updateById(r);
            }
            return Result.success("全部已读");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败: " + e.getMessage());
        }
    }
}
