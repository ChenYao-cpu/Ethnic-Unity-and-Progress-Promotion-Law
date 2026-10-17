package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.*;
import com.backend.com.minzu.mapper.*;
import com.backend.com.minzu.service.AssignmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentServiceImpl implements AssignmentService {
    
    @Autowired
    private AssignmentMapper assignmentMapper;
    
    @Autowired
    private AssignmentQuestionMapper assignmentQuestionMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    
    @Override
    public Assignment createAssignment(Assignment assignment) {
        assignment.setCreateTime(new Date());
        assignment.setUpdateTime(new Date());
        assignment.setStatus(1);
        // 明确设置为草稿状态
        if (assignment.getReviewStatus() == null || assignment.getReviewStatus().isEmpty()) {
            assignment.setReviewStatus("draft");
        }

        if (assignment.getQuestionCount() == null) {
            assignment.setQuestionCount(assignment.getSingleCount() + assignment.getMultiCount());
        }
        if (assignment.getTotalScore() == null) {
            assignment.setTotalScore(assignment.getQuestionCount() * 5);
        }

        assignmentMapper.insert(assignment);

        if (assignment.getQuestionIds() != null && !assignment.getQuestionIds().isEmpty()) {
            addQuestionsToAssignment(assignment.getId(), assignment.getQuestionIds());
        } else {
            autoGenerateQuestions(assignment.getId(), assignment.getSingleCount(), assignment.getMultiCount());
        }

        return assignment;
    }
    
    @Override
    public Assignment createAutoAssignment(Assignment assignment) {
        return createAssignment(assignment);
    }
    
    private void autoGenerateQuestions(Long assignmentId, Integer singleCount, Integer multiCount) {
        List<Question> questions = new ArrayList<>();
        
        if (singleCount > 0) {
            List<Question> singleQuestions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getType, 1)
                            .last("ORDER BY RAND() LIMIT " + singleCount)
            );
            questions.addAll(singleQuestions);
        }
        
        if (multiCount > 0) {
            List<Question> multiQuestions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getType, 2)
                            .last("ORDER BY RAND() LIMIT " + multiCount)
            );
            questions.addAll(multiQuestions);
        }
        
        addQuestionsToAssignment(assignmentId, questions.stream().map(Question::getId).collect(Collectors.toList()));
    }
    
    private void addQuestionsToAssignment(Long assignmentId, List<Long> questionIds) {
        int offset = assignmentQuestionMapper.selectCount(
                new LambdaQueryWrapper<AssignmentQuestion>()
                        .eq(AssignmentQuestion::getAssignmentId, assignmentId)
        ).intValue();
        
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        for (int i = 0; i < questions.size(); i++) {
            AssignmentQuestion aq = new AssignmentQuestion();
            aq.setAssignmentId(assignmentId);
            aq.setQuestionId(questions.get(i).getId());
            aq.setSortOrder(offset + i + 1);
            assignmentQuestionMapper.insert(aq);
        }
    }
    
    @Override
    public Assignment getAssignmentById(Long id) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment != null) {
            List<AssignmentQuestion> relations = assignmentQuestionMapper.selectList(
                    new LambdaQueryWrapper<AssignmentQuestion>()
                            .eq(AssignmentQuestion::getAssignmentId, id)
                            .orderByAsc(AssignmentQuestion::getSortOrder)
            );
            
            List<AssignmentQuestion> questionsWithDetail = new ArrayList<>();
            for (AssignmentQuestion aq : relations) {
                Question question = questionMapper.selectById(aq.getQuestionId());
                if (question != null) {
                    aq.setQuestion(question);
                    questionsWithDetail.add(aq);
                }
            }
            assignment.setQuestions(questionsWithDetail);
        }
        return assignment;
    }
    
    @Override
    public List<Assignment> getTeacherAssignments(Long teacherId, Integer status) {
        return assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                                                   .eq(teacherId != null, Assignment::getTeacherId, teacherId)
                                                   .eq(status != null, Assignment::getStatus, status)
                                                   .orderByDesc(Assignment::getCreateTime));
    }
    
    @Override
    public void publishAssignment(Long id) {
        Assignment a = new Assignment();
        a.setId(id);
        a.setStatus(1);
        a.setReviewStatus("published");
        assignmentMapper.updateById(a);
    }
    
    @Override
    public void deleteAssignment(Long id) {
        assignmentMapper.deleteById(id);
    }
    
    @Override
    public Assignment updateAssignment(Assignment a) {
        assignmentMapper.updateById(a);
        return a;
    }
    
    @Override
    public List<Assignment> getStudentPendingAssignments(Long studentId) {
        List<Assignment> allAssignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>()
                        .eq(Assignment::getStatus, 1)
                        .orderByDesc(Assignment::getCreateTime)
        );
        
        List<Long> completedAssignmentIds = assignmentSubmissionMapper.selectList(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getStudentId, studentId)
                        .eq(AssignmentSubmission::getStatus, 1)
        ).stream().map(AssignmentSubmission::getAssignmentId).collect(Collectors.toList());
        
        return allAssignments.stream()
                       .filter(assignment -> !completedAssignmentIds.contains(assignment.getId()))
                       .collect(Collectors.toList());
    }
    
    @Override
    public List<Assignment> getStudentCompletedAssignments(Long studentId) {
        List<AssignmentSubmission> submissions = assignmentSubmissionMapper.selectList(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getStudentId, studentId)
                        .eq(AssignmentSubmission::getStatus, 1)
                        .orderByDesc(AssignmentSubmission::getSubmitTime)
        );
        
        List<Long> completedAssignmentIds = submissions.stream()
                                                    .map(AssignmentSubmission::getAssignmentId)
                                                    .collect(Collectors.toList());
        
        if (completedAssignmentIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Assignment> completedAssignments = assignmentMapper.selectList(
                new LambdaQueryWrapper<Assignment>()
                        .in(Assignment::getId, completedAssignmentIds)
                        .orderByDesc(Assignment::getCreateTime)
        );
        
        // 为每个作业加载题目详情和分数信息
        for (Assignment assignment : completedAssignments) {
            // 加载题目
            List<AssignmentQuestion> relations = assignmentQuestionMapper.selectList(
                    new LambdaQueryWrapper<AssignmentQuestion>()
                            .eq(AssignmentQuestion::getAssignmentId, assignment.getId())
                            .orderByAsc(AssignmentQuestion::getSortOrder)
            );
            
            List<AssignmentQuestion> questionsWithDetail = new ArrayList<>();
            for (AssignmentQuestion aq : relations) {
                Question question = questionMapper.selectById(aq.getQuestionId());
                if (question != null) {
                    aq.setQuestion(question);
                    questionsWithDetail.add(aq);
                }
            }
            assignment.setQuestions(questionsWithDetail);
            
            // 添加分数和提交时间
            submissions.stream()
                    .filter(s -> s.getAssignmentId().equals(assignment.getId()))
                    .findFirst()
                    .ifPresent(submission -> {
                        assignment.setAvgScore(submission.getScore());
                        assignment.setSubmitTime(submission.getSubmitTime());
                    });
        }
        
        return completedAssignments;
    }
}