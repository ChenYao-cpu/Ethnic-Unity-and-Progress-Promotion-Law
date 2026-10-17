package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Assignment;
import com.backend.com.minzu.entity.AssignmentQuestion;
import com.backend.com.minzu.entity.AssignmentSubmission;
import com.backend.com.minzu.entity.Question;
import com.backend.com.minzu.mapper.AssignmentMapper;
import com.backend.com.minzu.mapper.AssignmentQuestionMapper;
import com.backend.com.minzu.mapper.AssignmentSubmissionMapper;
import com.backend.com.minzu.mapper.QuestionMapper;
import com.backend.com.minzu.service.AssignmentService;
import com.backend.com.minzu.service.AIService;
import com.backend.com.minzu.service.QuestionService;
import com.backend.com.minzu.service.StudyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignment")
public class AssignmentController {
    
    @Autowired
    private AssignmentService assignmentService;
    
    @Autowired
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private StudyService studyService;
    
    @Autowired
    private QuestionService questionService;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private AssignmentQuestionMapper assignmentQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @PostMapping("/create")
    public Result<Assignment> create(@RequestBody Map<String, Object> params) {
        try {
            System.out.println("收到作业创建请求: " + params);
            
            Assignment assignment = new Assignment();
            assignment.setTitle(params.get("title").toString());
            assignment.setDescription(params.get("description") != null ? params.get("description").toString() : "");
            
            if (params.get("deadline") != null) {
                assignment.setDeadline(java.sql.Date.valueOf(params.get("deadline").toString()));
            }
            
            if (params.get("teacherId") != null) {
                assignment.setTeacherId(Long.parseLong(params.get("teacherId").toString()));
            }
            
            assignment.setStatus(1);
            
            List<Long> questionIds = new ArrayList<>();
            
            if (params.get("questions") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> questions = (List<Map<String, Object>>) params.get("questions");
                
                for (Map<String, Object> q : questions) {
                    Question question = new Question();
                    question.setTitle(q.get("questionText") != null ? q.get("questionText").toString() : "题目内容");
                    question.setOptions(q.get("options") != null ? q.get("options").toString() : "[]");
                    question.setAnswer(q.get("answer") != null ? q.get("answer").toString() : "");
                    question.setAnalysis(q.get("analysis") != null ? q.get("analysis").toString() : "");
                    
                    if (q.get("type") != null) {
                        question.setType(Integer.parseInt(q.get("type").toString()));
                    } else {
                        question.setType(1);
                    }
                    
                    if (q.get("difficulty") != null) {
                        question.setDifficulty(Integer.parseInt(q.get("difficulty").toString()));
                    } else {
                        question.setDifficulty(2);
                    }
                    
                    if (params.get("teacherId") != null) {
                        question.setTeacherId(Long.parseLong(params.get("teacherId").toString()));
                    }
                    
                    question.setCreateTime(new Date());
                    
                    boolean saved = questionService.save(question);
                    if (saved) {
                        questionIds.add(question.getId());
                        System.out.println("新题目保存成功，ID: " + question.getId());
                    } else {
                        System.err.println("题目保存失败: " + q.get("questionText"));
                    }
                }
            }
            
            if (!questionIds.isEmpty()) {
                assignment.setQuestionIds(questionIds);
                assignment.setQuestionCount(questionIds.size());
                assignment.setTotalScore(questionIds.size() * 5);
            } else {
                assignment.setQuestionCount(0);
                assignment.setTotalScore(0);
            }
            
            Assignment created = assignmentService.createAssignment(assignment);
            System.out.println("作业创建成功，ID: " + created.getId() + ", 题目数量: " + questionIds.size());
            return Result.success(created);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建作业失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/teacher/list")
    public Result<List<Assignment>> getTeacherList(@RequestParam(required = false) Long teacherId,
                                                   @RequestParam(required = false) Integer status) {
        try {
            List<Assignment> list = assignmentService.getTeacherAssignments(teacherId, status);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取作业列表失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/student/list")
    public Result<List<Assignment>> getStudentList(@RequestParam(required = false) Long studentId,
                                                   @RequestParam(required = false) Integer status) {
        try {
            if (studentId == null) {
                return Result.error("学生ID不能为空");
            }
            
            List<Assignment> list;
            if (status != null && status == 2) {
                list = assignmentService.getStudentCompletedAssignments(studentId);
            } else {
                list = assignmentService.getStudentPendingAssignments(studentId);
            }
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取作业列表失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        try {
            Assignment assignment = assignmentService.getAssignmentById(id);
            if (assignment == null) {
                return Result.error("作业不存在");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", assignment.getId());
            result.put("title", assignment.getTitle());
            result.put("description", assignment.getDescription());
            result.put("deadline", assignment.getDeadline());
            result.put("status", assignment.getStatus());
            result.put("questionCount", assignment.getQuestionCount());
            result.put("totalScore", assignment.getTotalScore());
            
            List<Map<String, Object>> questionsList = new ArrayList<>();
            if (assignment.getQuestions() != null && !assignment.getQuestions().isEmpty()) {
                for (AssignmentQuestion aq : assignment.getQuestions()) {
                    Map<String, Object> questionWrapper = new HashMap<>();
                    Question q = aq.getQuestion();
                    
                    if (q != null) {
                        Map<String, Object> questionMap = new HashMap<>();
                        questionMap.put("id", q.getId());
                        questionMap.put("title", q.getTitle());
                        questionMap.put("options", q.getOptions());
                        questionMap.put("answer", q.getAnswer());
                        questionMap.put("analysis", q.getAnalysis());
                        questionMap.put("type", q.getType());
                        
                        questionWrapper.put("question", questionMap);
                        questionsList.add(questionWrapper);
                    }
                }
            }
            
            result.put("questions", questionsList);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取作业详情失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/publish/{id}")
    public Result<String> publish(@PathVariable Long id) {
        try {
            assignmentService.publishAssignment(id);
            return Result.success("发布成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发布失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            assignmentService.deleteAssignment(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/update/{id}")
    public Result<Assignment> update(@PathVariable Long id, @RequestBody Assignment assignment) {
        try {
            assignment.setId(id);
            Assignment updated = assignmentService.updateAssignment(assignment);
            return Result.success(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/submissions")
    public Result<List<Map<String, Object>>> getSubmissions(@RequestParam Long assignmentId,
                                                            @RequestParam(required = false) Integer status,
                                                            @RequestParam(required = false) String sortBy) {
        try {
            return Result.success(new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取提交记录失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/export/{id}")
    public Result<String> export(@PathVariable Long id) {
        try {
            return Result.success("导出成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("导出失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/submit")
    public Result<String> submitAssignment(@RequestBody Map<String, Object> params) {
        try {
            Long assignmentId = Long.valueOf(params.get("assignmentId").toString());
            Long studentId = Long.valueOf(params.get("studentId").toString());
            String answers = params.get("answers").toString();
            Integer score = params.get("score") != null ? Integer.valueOf(params.get("score").toString()) : 0;
            
            AssignmentSubmission existingSubmission = assignmentSubmissionMapper.selectOne(
                    new LambdaQueryWrapper<AssignmentSubmission>()
                            .eq(AssignmentSubmission::getAssignmentId, assignmentId)
                            .eq(AssignmentSubmission::getStudentId, studentId)
                            .eq(AssignmentSubmission::getStatus, 1)
            );
            
            if (existingSubmission != null) {
                return Result.error("该作业已完成，不能重复提交");
            }
            
            AssignmentSubmission submission = new AssignmentSubmission();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
            submission.setAnswers(answers);
            submission.setScore(score);
            submission.setStatus(1);
            submission.setSubmitTime(new Date());
            submission.setCreateTime(new Date());
            
            assignmentSubmissionMapper.insert(submission);
            
            System.out.println("作业提交成功 - 作业ID: " + assignmentId + ", 学生ID: " + studentId + ", 分数: " + score);
            
            studyService.updateStudyProfile(studentId);
            
            int growthPoints = score * 2;
            studyService.addGrowthRecordPublic(studentId, growthPoints, "assignment_submit",
                    "完成作业获得" + growthPoints + "成长值");
            
            return Result.success("提交成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("提交失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/submission")
    public Result<AssignmentSubmission> getSubmission(@RequestParam Long assignmentId, @RequestParam Long studentId) {
        try {
            AssignmentSubmission submission = assignmentSubmissionMapper.selectOne(
                    new LambdaQueryWrapper<AssignmentSubmission>()
                            .eq(AssignmentSubmission::getAssignmentId, assignmentId)
                            .eq(AssignmentSubmission::getStudentId, studentId)
                            .eq(AssignmentSubmission::getStatus, 1)
            );
            return Result.success(submission);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取提交记录失败: " + e.getMessage());
        }
    }

    /**
     * 提交审核（管理员 → 法务人员）
     */
    @PostMapping("/{id}/submit-review")
    public Result<String> submitForReview(@PathVariable Long id) {
        try {
            Assignment assignment = assignmentMapper.selectById(id);
            if (assignment == null) {
                return Result.error("作业不存在");
            }
            assignment.setReviewStatus("pending_review");
            assignment.setUpdateTime(new Date());
            assignmentMapper.updateById(assignment);
            System.out.println("作业已提交审核 - ID: " + id);
            return Result.success("已提交审核");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("提交审核失败: " + e.getMessage());
        }
    }

    /**
     * 获取作业的所有题目
     */
    @GetMapping("/{id}/questions")
    public Result<List<Map<String, Object>>> getAssignmentQuestions(@PathVariable Long id) {
        try {
            List<AssignmentQuestion> relations = assignmentQuestionMapper.selectList(
                    new LambdaQueryWrapper<AssignmentQuestion>()
                            .eq(AssignmentQuestion::getAssignmentId, id)
                            .orderByAsc(AssignmentQuestion::getSortOrder)
            );

            List<Map<String, Object>> result = new ArrayList<>();
            for (AssignmentQuestion aq : relations) {
                Question question = questionMapper.selectById(aq.getQuestionId());
                if (question != null) {
                    Map<String, Object> qMap = new HashMap<>();
                    qMap.put("id", question.getId());
                    qMap.put("questionText", question.getTitle());
                    qMap.put("options", question.getOptions());
                    qMap.put("correctAnswer", question.getAnswer());
                    qMap.put("analysis", question.getAnalysis());
                    qMap.put("type", question.getType());
                    qMap.put("difficulty", question.getDifficulty());
                    result.add(qMap);
                }
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取题目失败: " + e.getMessage());
        }
    }

    /** 给作业添加题目（支持从题库随机抽取或手动添加） */
    @PostMapping("/{id}/questions")
    public Result<String> addQuestionToAssignment(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            String type = (String) params.getOrDefault("type", "manual");
            if ("bank_random".equals(type)) {
                int count = params.get("count") != null ? Integer.parseInt(params.get("count").toString()) : 5;
                // 排除已在该作业中的题目
                List<AssignmentQuestion> existing = assignmentQuestionMapper.selectList(
                        new LambdaQueryWrapper<AssignmentQuestion>().eq(AssignmentQuestion::getAssignmentId, id));
                Set<Long> existingIds = existing.stream().map(AssignmentQuestion::getQuestionId).collect(Collectors.toSet());
                List<Question> allQuestions = questionMapper.selectList(null);
                List<Question> available = allQuestions.stream().filter(q -> !existingIds.contains(q.getId())).collect(Collectors.toList());
                if (available.isEmpty()) return Result.error("题库中没有可添加的新题目");
                Collections.shuffle(available);
                int max = Math.min(count, available.size());
                int offset = existing.size();
                for (int i = 0; i < max; i++) {
                    AssignmentQuestion aq = new AssignmentQuestion();
                    aq.setAssignmentId(id);
                    aq.setQuestionId(available.get(i).getId());
                    aq.setSortOrder(offset + i + 1);
                    assignmentQuestionMapper.insert(aq);
                }
                // 更新作业题目数
                Assignment assignment = assignmentMapper.selectById(id);
                if (assignment != null) {
                    assignment.setQuestionCount(offset + max);
                    assignmentMapper.updateById(assignment);
                }
                return Result.success("已从题库随机添加" + max + "道题");
            }
            // 手动添加
            String questionText = (String) params.getOrDefault("questionText", "");
            if (questionText.isEmpty()) return Result.error("题目内容不能为空");
            Question q = new Question();
            q.setTitle(questionText);
            q.setType(params.get("type") != null ? Integer.parseInt(params.get("type").toString()) : 1);
            q.setOptions((String) params.getOrDefault("options", "[]"));
            q.setAnswer((String) params.getOrDefault("answer", ""));
            questionMapper.insert(q);
            int offset = assignmentQuestionMapper.selectCount(
                    new LambdaQueryWrapper<AssignmentQuestion>().eq(AssignmentQuestion::getAssignmentId, id)).intValue();
            AssignmentQuestion aq = new AssignmentQuestion();
            aq.setAssignmentId(id);
            aq.setQuestionId(q.getId());
            aq.setSortOrder(offset + 1);
            assignmentQuestionMapper.insert(aq);
            return Result.success("题目已添加");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    /** 删除作业中的一道题目 */
    @DeleteMapping("/{assignmentId}/questions/{questionId}")
    public Result<String> removeQuestionFromAssignment(@PathVariable Long assignmentId, @PathVariable Long questionId) {
        try {
            assignmentQuestionMapper.delete(
                    new LambdaQueryWrapper<AssignmentQuestion>()
                            .eq(AssignmentQuestion::getAssignmentId, assignmentId)
                            .eq(AssignmentQuestion::getQuestionId, questionId)
            );
            // 更新题目数
            Assignment assignment = assignmentMapper.selectById(assignmentId);
            if (assignment != null) {
                int total = assignmentQuestionMapper.selectCount(
                        new LambdaQueryWrapper<AssignmentQuestion>().eq(AssignmentQuestion::getAssignmentId, assignmentId)).intValue();
                assignment.setQuestionCount(total);
                assignmentMapper.updateById(assignment);
            }
            return Result.success("题目已删除");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}