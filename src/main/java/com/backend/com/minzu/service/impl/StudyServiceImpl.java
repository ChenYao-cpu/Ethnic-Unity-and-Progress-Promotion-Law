package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.*;
import com.backend.com.minzu.mapper.*;
import com.backend.com.minzu.service.StudyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class StudyServiceImpl implements StudyService {

    @Autowired
    private StudyRecordMapper studyRecordMapper;

    @Autowired
    private StudyProfileMapper studyProfileMapper;

    @Autowired
    private StudyCheckinMapper studyCheckinMapper;

    @Autowired
    private StudyReportMapper studyReportMapper;

    @Autowired
    private GrowthRecordMapper growthRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserAnswerMapper userAnswerMapper;

    @Autowired
    private KnowledgeTreeMapper knowledgeTreeMapper;

    @Override
    @Transactional
    public void saveStudyRecord(Long userId, Long courseId, Integer duration, Integer progress) {
        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        record.setCourseId(courseId);
        record.setStudyDuration(duration);
        record.setProgress(progress != null ? progress : 0);
        record.setStudyTime(new Date());
        studyRecordMapper.insert(record);
        addGrowthRecord(userId, duration / 5, "study_complete", "完成课程学习 " + duration + " 分钟");
    }

    @Override
    public int getTotalStudyTime(Long userId) {
        Integer total = studyRecordMapper.getTotalStudyTime(userId);
        return total != null ? total : 0;
    }

    @Override
    public int getStudiedCourseCount(Long userId) {
        Integer count = studyRecordMapper.getStudiedCourseCount(userId);
        return count != null ? count : 0;
    }

    @Override
    public List<Map<String, Object>> getStudyStats(Long userId, int days) {
        return studyRecordMapper.getStudyStats(userId, days);
    }

    @Override
    public List<Map<String, Object>> getRecentRecords(Long userId, int limit) {
        return studyRecordMapper.getRecentRecords(userId, limit);
    }


    @Override
    public StudyRecord getUserCourseRecord(Long userId, Long courseId) {
        LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyRecord::getUserId, userId)
                .eq(StudyRecord::getCourseId, courseId)
                .orderByDesc(StudyRecord::getStudyTime)
                .last("LIMIT 1");
        return studyRecordMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public void updateStudyProgress(Long userId, Long courseId, Integer duration, Integer progress) {
        StudyRecord existRecord = getUserCourseRecord(userId, courseId);
        if (existRecord != null) {
            int newDuration = existRecord.getStudyDuration() + duration;
            int newProgress = progress != null ? progress : existRecord.getProgress();

            // 只有当新进度大于旧进度，或者时长有增加时才更新
            if (newProgress > existRecord.getProgress() || duration > 0) {
                existRecord.setStudyDuration(newDuration);
                existRecord.setProgress(newProgress);
                existRecord.setStudyTime(new Date());
                studyRecordMapper.updateById(existRecord);
            }
        } else {
            saveStudyRecord(userId, courseId, duration, progress);
        }
        if (progress != null && progress >= 100) {
            addGrowthRecord(userId, 20, "course_complete", "完成一门课程学习");
        }
    }

    @Override
    public Map<String, Object> getStudyProfile(Long userId) {
        StudyProfile profile = studyProfileMapper.selectOne(
                new LambdaQueryWrapper<StudyProfile>().eq(StudyProfile::getUserId, userId)
        );
        if (profile == null) {
            profile = new StudyProfile();
            profile.setUserId(userId);
            profile.setRegulationScore(0);
            profile.setCaseScore(0);
            profile.setAnswerScore(0);
            profile.setNoteScore(0);
            profile.setVideoScore(0);
            studyProfileMapper.insert(profile);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("regulationScore", profile.getRegulationScore());
        result.put("caseScore", profile.getCaseScore());
        result.put("answerScore", profile.getAnswerScore());
        result.put("noteScore", profile.getNoteScore());
        result.put("videoScore", profile.getVideoScore());
        List<Map<String, Object>> radarData = new ArrayList<>();
        radarData.add(createRadarItem("法规掌握", profile.getRegulationScore()));
        radarData.add(createRadarItem("案例分析", profile.getCaseScore()));
        radarData.add(createRadarItem("答题能力", profile.getAnswerScore()));
        radarData.add(createRadarItem("笔记整理", profile.getNoteScore()));
        radarData.add(createRadarItem("视频学习", profile.getVideoScore()));
        result.put("radarData", radarData);
        return result;
    }

    private Map<String, Object> createRadarItem(String name, Integer value) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value != null ? value : 0);
        return item;
    }

    @Override
    @Transactional
    public void updateStudyProfile(Long userId) {
        StudyProfile profile = studyProfileMapper.selectOne(
                new LambdaQueryWrapper<StudyProfile>().eq(StudyProfile::getUserId, userId)
        );
        if (profile == null) {
            profile = new StudyProfile();
            profile.setUserId(userId);
        }
        int totalStudyTime = getTotalStudyTime(userId);
        int videoScore = Math.min(100, totalStudyTime / 10);
        int courseCount = getStudiedCourseCount(userId);
        int regulationScore = Math.min(100, courseCount * 20);
        int answerScore = calculateAnswerScore(userId);
        int noteScore = calculateNoteScore(userId);
        int caseScore = calculateCaseScore(userId);
        profile.setRegulationScore(regulationScore);
        profile.setCaseScore(caseScore);
        profile.setAnswerScore(answerScore);
        profile.setNoteScore(noteScore);
        profile.setVideoScore(videoScore);
        profile.setUpdateTime(new Date());
        if (profile.getId() == null) {
            studyProfileMapper.insert(profile);
        } else {
            studyProfileMapper.updateById(profile);
        }
    }

    private int calculateAnswerScore(Long userId) {
        LambdaQueryWrapper<UserAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAnswer::getUserId, userId).ge(UserAnswer::getAnswerTime, getDateDaysAgo(30));
        List<UserAnswer> answers = userAnswerMapper.selectList(wrapper);
        if (answers.isEmpty()) return 0;
        long correctCount = answers.stream().filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1).count();
        return Math.min(100, (int) (correctCount * 100 / answers.size()));
    }

    private int calculateNoteScore(Long userId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId);
        Long count = noteMapper.selectCount(wrapper);
        return Math.min(100, count.intValue() * 10);
    }

    private int calculateCaseScore(Long userId) {
        int answerScore = calculateAnswerScore(userId);
        int studyTime = getTotalStudyTime(userId);
        return Math.min(100, (answerScore + studyTime / 20) / 2);
    }

    private Date getDateDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }

    @Override
    @Transactional
    public Map<String, Object> checkin(Long userId) {
        Map<String, Object> result = new HashMap<>();
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = sdf.format(today);
        LambdaQueryWrapper<StudyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyCheckin::getUserId, userId)
                .apply("DATE_FORMAT(checkin_date, '%Y-%m-%d') = {0}", todayStr);
        if (studyCheckinMapper.selectCount(wrapper) > 0) {
            result.put("success", false);
            result.put("message", "今日已打卡");
            return result;
        }
        StudyCheckin checkin = new StudyCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(today);
        checkin.setContinuousDays(1);
        studyCheckinMapper.insert(checkin);
        LambdaQueryWrapper<StudyCheckin> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.eq(StudyCheckin::getUserId, userId)
                .orderByDesc(StudyCheckin::getCheckinDate)
                .last("LIMIT 30");
        List<StudyCheckin> recentCheckins = studyCheckinMapper.selectList(recentWrapper);
        int continuousDays = calculateContinuousDays(recentCheckins);
        checkin.setContinuousDays(continuousDays);
        studyCheckinMapper.updateById(checkin);
        int scoreReward = 5 + continuousDays;
        updateUserScore(userId, scoreReward);
        addGrowthRecord(userId, scoreReward, "checkin", "连续打卡" + continuousDays + "天，奖励" + scoreReward + "积分");
        result.put("success", true);
        result.put("continuousDays", continuousDays);
        result.put("scoreReward", scoreReward);
        result.put("message", "打卡成功");
        return result;
    }

    private int calculateContinuousDays(List<StudyCheckin> checkins) {
        if (checkins.isEmpty()) return 1;
        int continuousDays = 1;
        for (int i = 0; i < checkins.size() - 1; i++) {
            try {
                Date currentDate = checkins.get(i).getCheckinDate();
                Date previousDate = checkins.get(i + 1).getCheckinDate();
                long diff = currentDate.getTime() - previousDate.getTime();
                long daysDiff = diff / (1000 * 60 * 60 * 24);
                if (daysDiff == 1) {
                    continuousDays++;
                } else {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }
        return continuousDays;
    }

    @Override
    public Map<String, Object> getCheckinCalendar(Long userId, int month) {
        Map<String, Object> result = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        if (month == 0) month = currentMonth;
        LambdaQueryWrapper<StudyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyCheckin::getUserId, userId)
                .apply("YEAR(checkin_date) = {0} AND MONTH(checkin_date) = {1}", year, month)
                .orderByAsc(StudyCheckin::getCheckinDate);
        List<StudyCheckin> checkins = studyCheckinMapper.selectList(wrapper);
        List<String> checkinDates = new ArrayList<>();
        for (StudyCheckin checkin : checkins) {
            checkinDates.add(new SimpleDateFormat("yyyy-MM-dd").format(checkin.getCheckinDate()));
        }
        result.put("year", year);
        result.put("month", month);
        result.put("checkinDates", checkinDates);
        result.put("totalDays", checkins.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> generateWeeklyReport(Long userId) {
        Map<String, Object> report = new HashMap<>();
        try {
            Date endDate = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.add(Calendar.DAY_OF_MONTH, -7);
            Date startDate = cal.getTime();

            LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StudyRecord::getUserId, userId)
                    .ge(StudyRecord::getStudyTime, startDate)
                    .le(StudyRecord::getStudyTime, endDate);
            List<StudyRecord> records = studyRecordMapper.selectList(wrapper);

            int totalStudyTime = 0;
            Set<Long> courseIds = new HashSet<>();

            if (records != null) {
                for (StudyRecord record : records) {
                    if (record.getStudyDuration() != null) {
                        totalStudyTime += record.getStudyDuration();
                    }
                    if (record.getCourseId() != null) {
                        courseIds.add(record.getCourseId());
                    }
                }
            }

            int growthValue = calculateGrowthValue(userId, startDate, endDate);

            report.put("startDate", startDate);
            report.put("endDate", endDate);
            report.put("totalStudyTime", totalStudyTime);
            report.put("courseCount", courseIds.size());
            report.put("averageDailyTime", totalStudyTime / 7);
            report.put("growthValue", growthValue);
        } catch (Exception e) {
            e.printStackTrace();
            report.put("startDate", new Date());
            report.put("endDate", new Date());
            report.put("totalStudyTime", 0);
            report.put("courseCount", 0);
            report.put("averageDailyTime", 0);
            report.put("growthValue", 0);
        }
        return report;
    }

    private int calculateGrowthValue(Long userId, Date startDate, Date endDate) {
        LambdaQueryWrapper<GrowthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrowthRecord::getUserId, userId)
                .ge(GrowthRecord::getCreateTime, startDate)
                .le(GrowthRecord::getCreateTime, endDate);
        List<GrowthRecord> records = growthRecordMapper.selectList(wrapper);

        int totalGrowth = 0;
        if (records != null) {
            for (GrowthRecord record : records) {
                if (record.getChangeValue() != null) {
                    totalGrowth += record.getChangeValue();
                }
            }
        }

        return totalGrowth;
    }


    @Override
    @Transactional
    public Map<String, Object> generateMonthlyReport(Long userId) {
        Map<String, Object> report = new HashMap<>();
        try {
            Date endDate = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.add(Calendar.MONTH, -1);
            Date startDate = cal.getTime();

            LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StudyRecord::getUserId, userId).between(StudyRecord::getStudyTime, startDate, endDate);
            List<StudyRecord> records = studyRecordMapper.selectList(wrapper);

            int totalStudyTime = records.stream()
                    .mapToInt(r -> r.getStudyDuration() != null ? r.getStudyDuration() : 0)
                    .sum();
            int courseCount = (int) records.stream()
                    .map(StudyRecord::getCourseId)
                    .distinct()
                    .count();

            report.put("startDate", startDate);
            report.put("endDate", endDate);
            report.put("totalStudyTime", totalStudyTime);
            report.put("courseCount", courseCount);
            report.put("averageDailyTime", totalStudyTime / 30);
            report.put("growthValue", 0);
        } catch (Exception e) {
            e.printStackTrace();
            report.put("totalStudyTime", 0);
            report.put("courseCount", 0);
            report.put("averageDailyTime", 0);
            report.put("growthValue", 0);
        }
        return report;
    }

    @Override
    public List<Map<String, Object>> getGrowthRecords(Long userId, int limit) {
        LambdaQueryWrapper<GrowthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GrowthRecord::getUserId, userId).orderByDesc(GrowthRecord::getCreateTime).last("LIMIT " + limit);
        List<GrowthRecord> records = growthRecordMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (GrowthRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("changeValue", record.getChangeValue());
            item.put("currentValue", record.getCurrentValue());
            item.put("type", record.getType());
            item.put("description", record.getDescription());
            item.put("createTime", record.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getWrongQuestionHotspots(Long userId) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId).orderByDesc(WrongQuestion::getWrongCount).last("LIMIT 10");
        List<WrongQuestion> wrongQuestions = wrongQuestionMapper.selectList(wrapper);
        List<Map<String, Object>> hotspots = new ArrayList<>();
        for (WrongQuestion wq : wrongQuestions) {
            Question question = questionMapper.selectById(wq.getQuestionId());
            if (question != null) {
                Map<String, Object> hotspot = new HashMap<>();
                hotspot.put("questionId", wq.getQuestionId());
                hotspot.put("title", question.getTitle());
                hotspot.put("wrongCount", wq.getWrongCount());
                hotspot.put("knowledgePoint", question.getAnalysis());
                hotspots.add(hotspot);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("hotspots", hotspots);
        result.put("totalWrongCount", wrongQuestions.size());
        return result;
    }

    @Override
    public String exportStudyArchive(Long userId, String format) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        int totalStudyTime = getTotalStudyTime(userId);
        int courseCount = getStudiedCourseCount(userId);
        StringBuilder archive = new StringBuilder();
        archive.append("学习档案\n========\n\n");
        archive.append("用户：").append(user.getNickname()).append("\n");
        archive.append("总学习时长：").append(totalStudyTime).append(" 分钟\n");
        archive.append("完成课程数：").append(courseCount).append("\n\n");
        LambdaQueryWrapper<StudyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudyRecord::getUserId, userId).orderByDesc(StudyRecord::getStudyTime);
        List<StudyRecord> records = studyRecordMapper.selectList(wrapper);
        archive.append("学习记录：\n");
        for (StudyRecord record : records) {
            archive.append("- 课程ID: ").append(record.getCourseId())
                    .append(", 时长: ").append(record.getStudyDuration())
                    .append("分钟, 进度: ").append(record.getProgress()).append("%\n");
        }
        return archive.toString();
    }

    @Override
    public Map<String, Object> getStudyTimeline(Long userId, int days) {
        List<Map<String, Object>> stats = getStudyStats(userId, days);
        List<Map<String, Object>> timeline = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        for (int i = days - 1; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String dateStr = sdf.format(cal.getTime());
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dateStr);
            dayData.put("duration", 0);
            for (Map<String, Object> stat : stats) {
                if (dateStr.equals(stat.get("date").toString())) {
                    dayData.put("duration", stat.get("duration"));
                    break;
                }
            }
            timeline.add(dayData);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("timeline", timeline);
        result.put("totalDays", days);
        return result;
    }

    @Override
    public KnowledgeTree getOrCreateTree(Long userId) {
        LambdaQueryWrapper<KnowledgeTree> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeTree::getUserId, userId);
        KnowledgeTree tree = knowledgeTreeMapper.selectOne(wrapper);
        if (tree == null) {
            tree = new KnowledgeTree();
            tree.setUserId(userId);
            tree.setLevel(1);
            tree.setGrowthValue(0);
            knowledgeTreeMapper.insert(tree);
        }
        return tree;
    }

    @Override
    @Transactional
    public KnowledgeTree feedTree(Long userId, Integer points) {
        KnowledgeTree tree = getOrCreateTree(userId);
        User user = userMapper.selectById(userId);
        if (user.getTotalScore() < points) {
            throw new RuntimeException("积分不足");
        }
        tree.setGrowthValue(tree.getGrowthValue() + points);
        tree.setLastFeedTime(new Date());
        int newLevel = tree.getGrowthValue() / 100 + 1;
        if (newLevel > tree.getLevel()) {
            tree.setLevel(newLevel);
            addGrowthRecord(userId, points, "tree_level_up", "知识之树升级到" + newLevel + "级");
        } else {
            addGrowthRecord(userId, points, "tree_feed", "为知识之树施肥");
        }
        user.setTotalScore(user.getTotalScore() - points);
        userMapper.updateById(user);
        knowledgeTreeMapper.updateById(tree);
        return tree;
    }

    private void updateUserScore(Long userId, int score) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setTotalScore(user.getTotalScore() + score);
            userMapper.updateById(user);
        }
    }

    private void addGrowthRecord(Long userId, int changeValue, String type, String description) {
        GrowthRecord record = new GrowthRecord();
        record.setUserId(userId);
        record.setChangeValue(changeValue);
        record.setCurrentValue(changeValue);
        record.setType(type);
        record.setDescription(description);
        record.setCreateTime(new Date());
        growthRecordMapper.insert(record);
    }

    public void addGrowthRecordPublic(Long userId, int changeValue, String type, String description) {
        addGrowthRecord(userId, changeValue, type, description);
    }
}
