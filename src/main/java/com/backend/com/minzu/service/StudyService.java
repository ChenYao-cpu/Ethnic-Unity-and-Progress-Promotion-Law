package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.KnowledgeTree;
import com.backend.com.minzu.entity.StudyRecord;

import java.util.List;
import java.util.Map;

public interface StudyService {
    void saveStudyRecord(Long userId, Long courseId, Integer duration, Integer progress);

    int getTotalStudyTime(Long userId);

    int getStudiedCourseCount(Long userId);

    List<Map<String, Object>> getStudyStats(Long userId, int days);

    List<Map<String, Object>> getRecentRecords(Long userId, int limit);

    StudyRecord getUserCourseRecord(Long userId, Long courseId);

    void updateStudyProgress(Long userId, Long courseId, Integer duration, Integer progress);

    Map<String, Object> getStudyProfile(Long userId);

    void updateStudyProfile(Long userId);

    Map<String, Object> checkin(Long userId);

    Map<String, Object> getCheckinCalendar(Long userId, int month);

    Map<String, Object> generateWeeklyReport(Long userId);

    Map<String, Object> generateMonthlyReport(Long userId);

    List<Map<String, Object>> getGrowthRecords(Long userId, int limit);

    Map<String, Object> getWrongQuestionHotspots(Long userId);

    String exportStudyArchive(Long userId, String format);

    Map<String, Object> getStudyTimeline(Long userId, int days);

    KnowledgeTree getOrCreateTree(Long userId);

    KnowledgeTree feedTree(Long userId, Integer points);

    void addGrowthRecordPublic(Long userId, int changeValue, String type, String description);
}
