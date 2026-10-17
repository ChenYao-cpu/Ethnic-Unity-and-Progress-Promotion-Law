package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.StudyRecord;
import com.backend.com.minzu.mapper.StudyRecordMapper;
import com.backend.com.minzu.service.StudyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study")
public class StudyController {

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyRecordMapper studyRecordMapper;

    @PostMapping("/record")
    public Result<Boolean> record(@RequestParam Long userId,
                                  @RequestParam Long courseId,
                                  @RequestParam Integer duration,
                                  @RequestParam(required = false) Integer progress) {
        studyService.saveStudyRecord(userId, courseId, duration, progress);
        return Result.success(true);
    }

    @PostMapping("/progress")
    public Result<Boolean> updateProgress(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long courseId = Long.valueOf(params.get("courseId").toString());
            Integer duration = Integer.valueOf(params.get("duration").toString());
            Integer progress = params.get("progress") != null ?
                    Integer.valueOf(params.get("progress").toString()) : 0;

            studyService.updateStudyProgress(userId, courseId, duration, progress);
            return Result.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新进度失败: " + e.getMessage());
        }
    }

    @GetMapping("/total")
    public Result<Map<String, Object>> total(@RequestParam Long userId) {
        int totalMinutes = studyService.getTotalStudyTime(userId);
        int courseCount = studyService.getStudiedCourseCount(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("totalMinutes", totalMinutes);
        data.put("totalHours", String.format("%.2f", totalMinutes / 60.0));
        data.put("courseCount", courseCount);

        return Result.success(data);
    }

    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> stats = studyService.getStudyStats(userId, days);
        return Result.success(stats);
    }

    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> records = studyService.getRecentRecords(userId, limit);
        return Result.success(records);
    }

    @GetMapping("/course/{courseId}")
    public Result<StudyRecord> getCourseRecord(
            @RequestParam Long userId,
            @PathVariable Long courseId) {
        StudyRecord record = studyService.getUserCourseRecord(userId, courseId);
        return Result.success(record);
    }


    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(@RequestParam Long userId) {
        Map<String, Object> overview = new HashMap<>();

        int totalMinutes = studyService.getTotalStudyTime(userId);
        int courseCount = studyService.getStudiedCourseCount(userId);

        overview.put("totalMinutes", totalMinutes);
        overview.put("totalHours", String.format("%.2f", totalMinutes / 60.0));
        overview.put("courseCount", courseCount);

        return Result.success(overview);
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(@RequestParam Long userId) {
        Map<String, Object> profile = studyService.getStudyProfile(userId);
        return Result.success(profile);
    }

    @PostMapping("/profile/update")
    public Result<Boolean> updateProfile(@RequestParam Long userId) {
        studyService.updateStudyProfile(userId);
        return Result.success(true);
    }

    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkin(@RequestParam Long userId) {
        Map<String, Object> result = studyService.checkin(userId);
        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error(result != null ? (String) result.get("message") : "打卡失败");
    }

    @GetMapping("/checkin/calendar")
    public Result<Map<String, Object>> getCheckinCalendar(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer month) {
        Map<String, Object> calendar = studyService.getCheckinCalendar(userId, month != null ? month : 0);
        return Result.success(calendar);
    }

    @GetMapping("/report/weekly")
    public Result<Map<String, Object>> getWeeklyReport(@RequestParam Long userId) {
        Map<String, Object> report = studyService.generateWeeklyReport(userId);
        return Result.success(report);
    }

    @GetMapping("/report/monthly")
    public Result<Map<String, Object>> getMonthlyReport(@RequestParam Long userId) {
        Map<String, Object> report = studyService.generateMonthlyReport(userId);
        return Result.success(report);
    }

    @GetMapping("/growth-records")
    public Result<List<Map<String, Object>>> getGrowthRecords(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> records = studyService.getGrowthRecords(userId, limit);
        return Result.success(records);
    }

    @GetMapping("/wrong-hotspots")
    public Result<Map<String, Object>> getWrongHotspots(@RequestParam Long userId) {
        Map<String, Object> hotspots = studyService.getWrongQuestionHotspots(userId);
        return Result.success(hotspots);
    }

    @GetMapping("/export")
    public Result<String> exportArchive(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "pdf") String format) {
        String filePath = studyService.exportStudyArchive(userId, format);
        if (filePath != null) {
            return Result.success(filePath);
        }
        return Result.error("导出失败");
    }

    @GetMapping("/timeline")
    public Result<Map<String, Object>> getTimeline(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> timeline = studyService.getStudyTimeline(userId, days);
        return Result.success(timeline);
    }

    @GetMapping("/records")
    public Result<List<Map<String, Object>>> getUserRecords(@RequestParam Long userId) {
        List<Map<String, Object>> records = studyService.getRecentRecords(userId, 100);
        return Result.success(records);
    }

    @GetMapping("/stats/summary")
    public Result<Map<String, Object>> getStatsSummary(@RequestParam Long userId) {
        int totalStudyTime = studyService.getTotalStudyTime(userId);
        int completeCourses = studyService.getStudiedCourseCount(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("totalStudyTime", totalStudyTime);
        data.put("completeCourses", completeCourses);

        return Result.success(data);
    }
    @GetMapping("/debug/records")
    public Result<List<Map<String, Object>>> debugRecords(@RequestParam Long userId) {
        List<Map<String, Object>> records = studyRecordMapper.getRecentRecords(userId, 100);
        return Result.success(records);
    }
}