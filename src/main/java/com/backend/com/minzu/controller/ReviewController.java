package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Assignment;
import com.backend.com.minzu.entity.AssignmentQuestion;
import com.backend.com.minzu.entity.Question;
import com.backend.com.minzu.mapper.AssignmentMapper;
import com.backend.com.minzu.mapper.AssignmentQuestionMapper;
import com.backend.com.minzu.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private AssignmentQuestionMapper assignmentQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取待审核的作业列表
     */
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> getPendingReviews() {
        List<Assignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>()
                        .eq(Assignment::getReviewStatus, "pending_review")
                        .orderByDesc(Assignment::getCreateTime)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Assignment a : assignments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("description", a.getDescription());
            item.put("questionCount", a.getQuestionCount());
            item.put("submitTime", a.getUpdateTime());
            item.put("submitter", "管理员");
            item.put("deadline", a.getDeadline());
            item.put("totalScore", a.getTotalScore());
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 获取审核历史
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getReviewHistory() {
        List<Assignment> assignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>()
                        .in(Assignment::getReviewStatus, Arrays.asList("approved", "rejected"))
                        .orderByDesc(Assignment::getUpdateTime)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Assignment a : assignments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("questionCount", a.getQuestionCount());
            item.put("reviewTime", a.getUpdateTime());
            item.put("result", a.getReviewStatus());
            item.put("comment", a.getReviewComment());
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 获取审核详情（含所有题目）
     */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getReviewDetail(@PathVariable Long id) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            return Result.error("作业不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", assignment.getId());
        result.put("title", assignment.getTitle());
        result.put("description", assignment.getDescription());
        result.put("submitter", "管理员");
        result.put("submitTime", assignment.getUpdateTime());
        result.put("deadline", assignment.getDeadline());
        result.put("totalScore", assignment.getTotalScore());
        result.put("reviewStatus", assignment.getReviewStatus());
        result.put("reviewComment", assignment.getReviewComment());

        // 获取所有题目
        List<AssignmentQuestion> relations = assignmentQuestionMapper.selectList(
                new LambdaQueryWrapper<AssignmentQuestion>()
                        .eq(AssignmentQuestion::getAssignmentId, id)
                        .orderByAsc(AssignmentQuestion::getSortOrder)
        );

        List<Map<String, Object>> questions = new ArrayList<>();
        for (AssignmentQuestion aq : relations) {
            Question q = questionMapper.selectById(aq.getQuestionId());
            if (q != null) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("assignmentQuestionId", aq.getId());
                qMap.put("id", q.getId()); // questionId
                qMap.put("title", q.getTitle());
                qMap.put("options", q.getOptions());
                qMap.put("answer", q.getAnswer());
                qMap.put("analysis", q.getAnalysis());
                qMap.put("type", q.getType());
                qMap.put("difficulty", q.getDifficulty());
                questions.add(qMap);
            }
        }
        result.put("questions", questions);

        // 解析已有的逐题审核结果
        if (assignment.getQuestionReview() != null && !assignment.getQuestionReview().isEmpty()) {
            try {
                List<Map<String, Object>> questionReview = objectMapper.readValue(
                        assignment.getQuestionReview(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                result.put("questionReview", questionReview);
            } catch (Exception e) {
                result.put("questionReview", new ArrayList<>());
            }
        } else {
            result.put("questionReview", new ArrayList<>());
        }

        return Result.success(result);
    }

    /**
     * 逐题审核通过
     */
    @PostMapping("/{id}/approve-question/{questionId}")
    public Result<String> approveQuestion(@PathVariable Long id, @PathVariable Long questionId,
                                          @RequestBody Map<String, String> params) {
        String comment = params.getOrDefault("comment", "");
        updateQuestionReview(id, questionId, "approved", comment);
        return Result.success("该题已通过");
    }

    /**
     * 逐题审核驳回
     */
    @PostMapping("/{id}/reject-question/{questionId}")
    public Result<String> rejectQuestion(@PathVariable Long id, @PathVariable Long questionId,
                                         @RequestBody Map<String, String> params) {
        String comment = params.getOrDefault("comment", "");
        if (comment.isEmpty()) {
            return Result.error("请填写驳回理由");
        }
        updateQuestionReview(id, questionId, "rejected", comment);
        return Result.success("该题已驳回");
    }

    /**
     * 提交整体审核结果
     * 如果所有题都通过 → approved
     * 如果有任何题被驳回 → rejected，打回给管理员
     */
    @PostMapping("/{id}/submit-review")
    public Result<Map<String, Object>> submitReview(@PathVariable Long id,
                                                     @RequestBody Map<String, String> params) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            return Result.error("作业不存在");
        }

        String overallComment = params.getOrDefault("comment", "");

        // 解析逐题审核结果
        List<Map<String, Object>> questionReviews = new ArrayList<>();
        if (assignment.getQuestionReview() != null && !assignment.getQuestionReview().isEmpty()) {
            try {
                questionReviews = objectMapper.readValue(
                        assignment.getQuestionReview(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 检查是否有题目被驳回
        boolean hasRejected = false;
        int approvedCount = 0;
        int rejectedCount = 0;
        List<Map<String, Object>> rejectedQuestions = new ArrayList<>();

        for (Map<String, Object> qr : questionReviews) {
            String status = (String) qr.get("status");
            if ("rejected".equals(status)) {
                hasRejected = true;
                rejectedCount++;
                rejectedQuestions.add(qr);
            } else if ("approved".equals(status)) {
                approvedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("approvedCount", approvedCount);
        result.put("rejectedCount", rejectedCount);
        result.put("rejectedQuestions", rejectedQuestions);

        if (hasRejected) {
            // 驳回给管理员
            assignmentMapper.update(null,
                    new LambdaUpdateWrapper<Assignment>()
                            .eq(Assignment::getId, id)
                            .set(Assignment::getReviewStatus, "rejected")
                            .set(Assignment::getReviewComment, overallComment)
            );
            result.put("status", "rejected");
            result.put("message", "审核不通过，共" + rejectedCount + "道题目需要修改，已打回给管理员");
            System.out.println("审核驳回 - 作业ID: " + id + ", 通过: " + approvedCount + ", 驳回: " + rejectedCount);
        } else {
            // 全部通过
            assignmentMapper.update(null,
                    new LambdaUpdateWrapper<Assignment>()
                            .eq(Assignment::getId, id)
                            .set(Assignment::getReviewStatus, "approved")
                            .set(Assignment::getReviewComment, overallComment)
            );
            result.put("status", "approved");
            result.put("message", "审核全部通过，共" + approvedCount + "道题目");
            System.out.println("审核通过 - 作业ID: " + id + ", 共" + approvedCount + "题");
        }

        return Result.success(result);
    }

    /**
     * 审核通过（整体通过，兼容旧接口）
     */
    @PostMapping("/{id}/approve")
    public Result<String> approve(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String comment = params.getOrDefault("comment", "");

        assignmentMapper.update(null,
                new LambdaUpdateWrapper<Assignment>()
                        .eq(Assignment::getId, id)
                        .set(Assignment::getReviewStatus, "approved")
                        .set(Assignment::getReviewComment, comment)
        );

        System.out.println("审核通过 - 作业ID: " + id + ", 备注: " + comment);
        return Result.success("审核已通过");
    }

    /**
     * 审核驳回（整体驳回，兼容旧接口）
     */
    @PostMapping("/{id}/reject")
    public Result<String> reject(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String comment = params.getOrDefault("comment", "");
        if (comment.isEmpty()) {
            return Result.error("请填写驳回理由");
        }

        assignmentMapper.update(null,
                new LambdaUpdateWrapper<Assignment>()
                        .eq(Assignment::getId, id)
                        .set(Assignment::getReviewStatus, "rejected")
                        .set(Assignment::getReviewComment, comment)
        );

        System.out.println("审核驳回 - 作业ID: " + id + ", 理由: " + comment);
        return Result.success("已驳回");
    }

    /**
     * 更新逐题审核结果
     */
    private void updateQuestionReview(Long assignmentId, Long questionId, String status, String comment) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) return;

        List<Map<String, Object>> questionReviews = new ArrayList<>();
        if (assignment.getQuestionReview() != null && !assignment.getQuestionReview().isEmpty()) {
            try {
                questionReviews = objectMapper.readValue(
                        assignment.getQuestionReview(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } catch (Exception e) {
                questionReviews = new ArrayList<>();
            }
        }

        // 查找是否已有该题的审核记录
        boolean found = false;
        for (Map<String, Object> qr : questionReviews) {
            Object qidObj = qr.get("questionId");
            Long qid = qidObj instanceof Integer ? ((Integer) qidObj).longValue() : (Long) qidObj;
            if (qid != null && qid.equals(questionId)) {
                qr.put("status", status);
                qr.put("comment", comment);
                found = true;
                break;
            }
        }

        if (!found) {
            Map<String, Object> newReview = new HashMap<>();
            newReview.put("questionId", questionId);
            newReview.put("status", status);
            newReview.put("comment", comment);
            questionReviews.add(newReview);
        }

        try {
            String json = objectMapper.writeValueAsString(questionReviews);
            assignmentMapper.update(null,
                    new LambdaUpdateWrapper<Assignment>()
                            .eq(Assignment::getId, assignmentId)
                            .set(Assignment::getQuestionReview, json)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 课程审核 ====================

    @Autowired
    private com.backend.com.minzu.mapper.CourseMapper courseMapper;

    /** 获取待审核的课程列表 */
    @GetMapping("/course/pending")
    public Result<List<Map<String, Object>>> getPendingCourseReviews() {
        List<com.backend.com.minzu.entity.Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<com.backend.com.minzu.entity.Course>()
                        .eq(com.backend.com.minzu.entity.Course::getReviewStatus, "pending_review")
                        .orderByDesc(com.backend.com.minzu.entity.Course::getCreateTime)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (com.backend.com.minzu.entity.Course c : courses) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("videoUrl", c.getVideoUrl());
            item.put("cover", c.getCover());
            item.put("category", c.getCategory());
            item.put("duration", c.getDuration());
            item.put("points", c.getPoints());
            item.put("description", c.getDescription());
            item.put("teacherId", c.getTeacherId());
            item.put("createTime", c.getCreateTime());
            result.add(item);
        }
        return Result.success(result);
    }

    /** 审核通过课程 → 自动发布 */
    @PostMapping("/course/{id}/approve")
    public Result<String> approveCourse(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String comment = params.getOrDefault("comment", "");
        courseMapper.update(null,
                new LambdaUpdateWrapper<com.backend.com.minzu.entity.Course>()
                        .eq(com.backend.com.minzu.entity.Course::getId, id)
                        .set(com.backend.com.minzu.entity.Course::getReviewStatus, "approved")
                        .set(com.backend.com.minzu.entity.Course::getReviewComment, comment)
        );
        // 自动发布
        com.backend.com.minzu.entity.Course course = courseMapper.selectById(id);
        if (course != null) {
            course.setReviewStatus("published");
            course.setStatus(1);
            course.setUpdateTime(new Date());
            courseMapper.updateById(course);
        }
        return Result.success("审核通过，课程已自动发布");
    }

    /** 驳回课程 */
    @PostMapping("/course/{id}/reject")
    public Result<String> rejectCourse(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String comment = params.getOrDefault("comment", "");
        if (comment.isEmpty()) {
            return Result.error("请填写驳回理由");
        }
        courseMapper.update(null,
                new LambdaUpdateWrapper<com.backend.com.minzu.entity.Course>()
                        .eq(com.backend.com.minzu.entity.Course::getId, id)
                        .set(com.backend.com.minzu.entity.Course::getReviewStatus, "rejected")
                        .set(com.backend.com.minzu.entity.Course::getReviewComment, comment)
                        .set(com.backend.com.minzu.entity.Course::getUpdateTime, new Date())
        );
        return Result.success("已驳回");
    }
}
