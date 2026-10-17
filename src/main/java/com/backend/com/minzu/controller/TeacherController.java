package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.*;
import com.backend.com.minzu.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private AssignmentSubmissionMapper assignmentSubmissionMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/assignment/{assignmentId}/submissions")
    public Result<List<Map<String, Object>>> getAssignmentSubmissions(@PathVariable Long assignmentId) {
        try {
            List<AssignmentSubmission> submissions = assignmentSubmissionMapper.selectList(
                    new LambdaQueryWrapper<AssignmentSubmission>()
                            .eq(AssignmentSubmission::getAssignmentId, assignmentId)
                            .orderByDesc(AssignmentSubmission::getSubmitTime)
            );

            // 获取作业总分用于百分制转换
            Assignment assignment = assignmentMapper.selectById(assignmentId);
            int totalScore = (assignment != null && assignment.getTotalScore() != null) ? assignment.getTotalScore() : 100;

            List<Map<String, Object>> result = new ArrayList<>();
            for (AssignmentSubmission submission : submissions) {
                int rawScore = submission.getScore() != null ? submission.getScore() : 0;
                int normalizedScore = totalScore > 0 ? (int) Math.round((double) rawScore / totalScore * 100) : rawScore;
                Map<String, Object> item = new HashMap<>();
                item.put("id", submission.getId());
                item.put("assignmentId", submission.getAssignmentId());
                item.put("studentId", submission.getStudentId());
                item.put("score", normalizedScore);
                item.put("submitTime", submission.getSubmitTime());
                item.put("status", submission.getStatus());

                try {
                    User student = userMapper.selectById(submission.getStudentId());
                    if (student != null) {
                        String studentName = student.getNickname() != null && !student.getNickname().isEmpty()
                                ? student.getNickname()
                                : (student.getUsername() != null ? student.getUsername() : "学生" + student.getId());
                        item.put("studentName", studentName);
                        item.put("school", student.getSchool());
                    } else {
                        item.put("studentName", "未知学生");
                    }
                } catch (Exception e) {
                    item.put("studentName", "学生" + submission.getStudentId());
                }

                result.add(item);
            }

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取提交记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/assignment/{assignmentId}/statistics")
    public Result<Map<String, Object>> getAssignmentStatistics(@PathVariable Long assignmentId) {
        try {
            List<AssignmentSubmission> submissions = assignmentSubmissionMapper.selectList(
                    new LambdaQueryWrapper<AssignmentSubmission>()
                            .eq(AssignmentSubmission::getAssignmentId, assignmentId)
                            .orderByAsc(AssignmentSubmission::getScore)
            );

            // 获取所有党员（含管理员身份的党员）
            List<User> allUsers = userMapper.selectList(null);
            List<User> allPartyMembers = new ArrayList<>();
            for (User u : allUsers) {
                String role = u.getRole();
                String identity = u.getIdentity();
                boolean isPartyMember = "party_member".equals(role)
                    || (identity != null && identity.contains("\"party_member\":true"));
                if (isPartyMember) {
                    allPartyMembers.add(u);
                }
            }

            // 统计未提交人数
            Set<Long> submittedUserIds = submissions.stream()
                    .map(AssignmentSubmission::getStudentId)
                    .collect(Collectors.toSet());

            int unsubmittedCount = 0;
            for (User u : allPartyMembers) {
                if (!submittedUserIds.contains(u.getId())) {
                    unsubmittedCount++;
                }
            }

            // 获取作业总分，用于标准化为百分制
            Assignment assignment = assignmentMapper.selectById(assignmentId);
            int assignmentTotalScore = (assignment != null && assignment.getTotalScore() != null) ? assignment.getTotalScore() : 100;

            // 只用已提交的分数做统计，统一转为百分制
            List<Integer> allScores = new ArrayList<>();
            for (AssignmentSubmission s : submissions) {
                int rawScore = s.getScore() != null ? s.getScore() : 0;
                int normalizedScore = assignmentTotalScore > 0 ? (int) Math.round((double) rawScore / assignmentTotalScore * 100) : rawScore;
                allScores.add(normalizedScore);
            }

            int totalCount = allPartyMembers.size();
            int submittedCount = submissions.size();
            Map<String, Object> statistics = new HashMap<>();

            if (totalCount == 0) {
                statistics.put("totalCount", 0);
                statistics.put("submittedCount", 0);
                statistics.put("unsubmittedCount", 0);
                statistics.put("avgScore", 0);
                statistics.put("maxScore", 0);
                statistics.put("minScore", 0);
                statistics.put("passRate", 0);
                statistics.put("excellentRate", 0);
                statistics.put("needImprovementCount", 0);
                statistics.put("submitRate", 0);
                statistics.put("onTimeSubmitRate", 0);
                statistics.put("median", 0);
                statistics.put("mode", 0);
                statistics.put("stdDev", 0);
                statistics.put("skewness", 0);
                statistics.put("scoreDistribution", new ArrayList<>());
                return Result.success(statistics);
            }

            // 使用allScores（只含已提交的分数）
            List<Integer> scores = allScores;

            double avgScore = scores.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);
            int maxScore = scores.stream().mapToInt(Integer::intValue).max().orElse(0);
            int minScore = scores.stream().mapToInt(Integer::intValue).min().orElse(0);

            long passCount = scores.stream().filter(s -> s >= 60).count();
            double passRate = (double) passCount / totalCount * 100;

            long excellentCount = scores.stream().filter(s -> s >= 90).count();
            double excellentRate = (double) excellentCount / totalCount * 100;

            long needImprovementCount = scores.stream().filter(s -> s < 60).count();

            Collections.sort(scores);
            int scoreCount = scores.size();
            double median;
            if (scoreCount % 2 == 0 && scoreCount > 0) {
                median = (scores.get(scoreCount / 2 - 1) + scores.get(scoreCount / 2)) / 2.0;
            } else if (scoreCount > 0) {
                median = scores.get(scoreCount / 2);
            } else {
                median = 0;
            }

            Map<Integer, Long> frequencyMap = scores.stream()
                    .collect(Collectors.groupingBy(Integer::intValue, Collectors.counting()));
            int mode = frequencyMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(0);

            double variance = scores.stream()
                    .mapToDouble(s -> Math.pow(s - avgScore, 2))
                    .average()
                    .orElse(0);
            double stdDev = Math.sqrt(variance);

            double skewness = 0;
            if (stdDev > 0) {
                skewness = scores.stream()
                        .mapToDouble(s -> Math.pow((s - avgScore) / stdDev, 3))
                        .average()
                        .orElse(0) / totalCount;
            }

            Map<String, Integer> distribution = new LinkedHashMap<>();
            distribution.put("0-59", 0);
            distribution.put("60-69", 0);
            distribution.put("70-79", 0);
            distribution.put("80-89", 0);
            distribution.put("90-100", 0);

            for (int score : scores) {
                if (score < 60) {
                    distribution.put("0-59", distribution.get("0-59") + 1);
                } else if (score < 70) {
                    distribution.put("60-69", distribution.get("60-69") + 1);
                } else if (score < 80) {
                    distribution.put("70-79", distribution.get("70-79") + 1);
                } else if (score < 90) {
                    distribution.put("80-89", distribution.get("80-89") + 1);
                } else {
                    distribution.put("90-100", distribution.get("90-100") + 1);
                }
            }

            List<Map<String, Object>> scoreDistribution = new ArrayList<>();
            String[] ranges = {"0-59", "60-69", "70-79", "80-89", "90-100"};
            String[] levels = {"不及格", "及格", "中等", "良好", "优秀"};
            String[] levelColors = {"#ff4d4f", "#faad14", "#fa8c16", "#52c41a", "#1890ff"};
            String[] suggestions = {
                    "需要重点关注，加强基础知识学习",
                    "基础掌握一般，需要加强巩固练习",
                    "掌握较好，可适当增加难度练习",
                    "掌握良好，可以挑战更复杂的内容",
                    "掌握优秀，可以承担进阶任务"
            };

            for (int i = 0; i < ranges.length; i++) {
                Map<String, Object> item = new HashMap<>();
                String range = ranges[i];
                int count = distribution.get(range);
                double percentage = (double) count / totalCount * 100;

                item.put("range", range);
                item.put("level", levels[i]);
                item.put("levelColor", levelColors[i]);
                item.put("count", count);
                item.put("percentage", Math.round(percentage * 100.0) / 100.0);
                item.put("suggestion", suggestions[i]);
                scoreDistribution.add(item);
            }

            statistics.put("totalCount", totalCount);
            statistics.put("submittedCount", submittedCount);
            statistics.put("unsubmittedCount", unsubmittedCount);
            statistics.put("avgScore", Math.round(avgScore * 100.0) / 100.0);
            statistics.put("maxScore", maxScore);
            statistics.put("minScore", minScore);
            statistics.put("passRate", Math.round(passRate * 100.0) / 100.0);
            statistics.put("excellentRate", Math.round(excellentRate * 100.0) / 100.0);
            statistics.put("needImprovementCount", needImprovementCount);
            statistics.put("submitRate", Math.round((double) submittedCount / totalCount * 10000.0) / 100.0);
            statistics.put("onTimeSubmitRate", Math.round((double) submittedCount / totalCount * 10000.0) / 100.0);
            statistics.put("median", Math.round(median * 100.0) / 100.0);
            statistics.put("mode", mode);
            statistics.put("stdDev", Math.round(stdDev * 100.0) / 100.0);
            statistics.put("skewness", Math.round(skewness * 100.0) / 100.0);
            statistics.put("scoreDistribution", scoreDistribution);

            return Result.success(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取某个作业的学习进度概览
     * 返回：完成人数、未完成人数、完成率、每个党员的状态
     */
    @GetMapping("/assignment/{assignmentId}/progress")
    public Result<Map<String, Object>> getAssignmentProgress(@PathVariable Long assignmentId) {
        try {
            Assignment assignment = assignmentMapper.selectById(assignmentId);
            if (assignment == null) {
                return Result.error("作业不存在");
            }

            // 获取所有已提交的记录
            List<AssignmentSubmission> submissions = assignmentSubmissionMapper.selectList(
                    new LambdaQueryWrapper<AssignmentSubmission>()
                            .eq(AssignmentSubmission::getAssignmentId, assignmentId)
            );

            // 获取所有党员（含管理员身份的党员）
            List<User> allUsers = userMapper.selectList(null);
            List<User> allPartyMembers = new ArrayList<>();
            for (User u : allUsers) {
                String role = u.getRole();
                String identity = u.getIdentity();
                boolean isPartyMember = "party_member".equals(role)
                    || (identity != null && identity.contains("\"party_member\":true"));
                if (isPartyMember) {
                    allPartyMembers.add(u);
                }
            }

            Set<Long> completedUserIds = submissions.stream()
                    .map(AssignmentSubmission::getStudentId)
                    .collect(Collectors.toSet());

            int totalStudents = allPartyMembers.size();
            int completedCount = 0;
            int uncompletedCount = 0;

            List<Map<String, Object>> studentDetails = new ArrayList<>();
            for (User student : allPartyMembers) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("studentId", student.getId());
                detail.put("studentName", student.getNickname() != null && !student.getNickname().isEmpty()
                        ? student.getNickname() : student.getUsername());
                detail.put("school", student.getSchool());

                if (completedUserIds.contains(student.getId())) {
                    detail.put("status", "completed");
                    // 找到该学生的提交记录
                    AssignmentSubmission sub = submissions.stream()
                            .filter(s -> s.getStudentId().equals(student.getId()))
                            .findFirst().orElse(null);
                    detail.put("score", sub != null ? sub.getScore() : 0);
                    detail.put("submitTime", sub != null ? sub.getSubmitTime() : null);
                    completedCount++;
                } else {
                    detail.put("status", "uncompleted");
                    detail.put("score", 0);
                    detail.put("submitTime", null);
                    uncompletedCount++;
                }
                studentDetails.add(detail);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("assignmentId", assignmentId);
            result.put("assignmentTitle", assignment.getTitle());
            result.put("totalStudents", totalStudents);
            result.put("completedCount", completedCount);
            result.put("uncompletedCount", uncompletedCount);
            result.put("completionRate", totalStudents > 0
                    ? Math.round((double) completedCount / totalStudents * 10000.0) / 100.0 : 0);
            result.put("studentDetails", studentDetails);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取学习进度失败: " + e.getMessage());
        }
    }

    @GetMapping("/student/{studentId}/progress")
    public Result<Map<String, Object>> getStudentProgress(@PathVariable Long studentId) {
        try {
            List<AssignmentSubmission> submissions = assignmentSubmissionMapper.selectList(
                    new LambdaQueryWrapper<AssignmentSubmission>()
                            .eq(AssignmentSubmission::getStudentId, studentId)
                            .orderByDesc(AssignmentSubmission::getSubmitTime)
            );

            Map<String, Object> progress = new HashMap<>();

            int totalAssignments = submissions.size();
            progress.put("totalAssignments", totalAssignments);

            int completedAssignments = (int) submissions.stream()
                    .filter(s -> s.getStatus() == 1)
                    .count();
            progress.put("completedAssignments", completedAssignments);

            if (!submissions.isEmpty()) {
                double avgScore = submissions.stream()
                        .mapToInt(AssignmentSubmission::getScore)
                        .average()
                        .orElse(0);
                progress.put("avgScore", Math.round(avgScore * 100.0) / 100.0);

                int maxScore = submissions.stream()
                        .mapToInt(AssignmentSubmission::getScore)
                        .max()
                        .orElse(0);
                int minScore = submissions.stream()
                        .mapToInt(AssignmentSubmission::getScore)
                        .min()
                        .orElse(0);
                progress.put("maxScore", maxScore);
                progress.put("minScore", minScore);
            } else {
                progress.put("avgScore", 0);
                progress.put("maxScore", 0);
                progress.put("minScore", 0);
            }

            List<Map<String, Object>> recentSubmissions = new ArrayList<>();
            int limit = Math.min(10, submissions.size());
            for (int i = 0; i < limit; i++) {
                AssignmentSubmission submission = submissions.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("assignmentId", submission.getAssignmentId());
                item.put("score", submission.getScore());
                item.put("submitTime", submission.getSubmitTime());

                // 查找作业标题
                Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
                item.put("assignmentTitle", assignment != null ? assignment.getTitle() : "未知作业");

                recentSubmissions.add(item);
            }
            progress.put("recentSubmissions", recentSubmissions);

            return Result.success(progress);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取学生进度失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有已发布的作业列表（供下拉选择）
     */
    @GetMapping("/assignments")
    public Result<List<Map<String, Object>>> getAssignments(@RequestParam(required = false) Long teacherId) {
        try {
            List<Assignment> assignments = assignmentMapper.selectList(
                    new LambdaQueryWrapper<Assignment>()
                            .eq(teacherId != null, Assignment::getTeacherId, teacherId)
                            .eq(Assignment::getStatus, 1)
                            .orderByDesc(Assignment::getCreateTime)
            );

            List<Map<String, Object>> result = new ArrayList<>();
            for (Assignment a : assignments) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", a.getId());
                item.put("title", a.getTitle());
                item.put("deadline", a.getDeadline());
                item.put("questionCount", a.getQuestionCount());
                item.put("totalScore", a.getTotalScore());
                item.put("reviewStatus", a.getReviewStatus());
                result.add(item);
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取作业列表失败: " + e.getMessage());
        }
    }
}
