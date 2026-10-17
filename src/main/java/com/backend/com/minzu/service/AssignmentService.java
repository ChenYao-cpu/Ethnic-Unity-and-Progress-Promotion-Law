package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.Assignment;
import java.util.List;

public interface AssignmentService {
    // 自动随机抽题创建作业
    Assignment createAutoAssignment(Assignment assignment);
    
    // 获取详情（含题目内容）
    Assignment getAssignmentById(Long id);
    
    // 获取教师列表
    List<Assignment> getTeacherAssignments(Long teacherId, Integer status);
    
    // 更新与删除
    Assignment updateAssignment(Assignment assignment);
    void deleteAssignment(Long id);
    void publishAssignment(Long id);
    
    Assignment createAssignment(Assignment assignment);
    
    // 获取学生未完成的作业列表
    List<Assignment> getStudentPendingAssignments(Long studentId);
    
    // 获取学生已完成的作业列表
    List<Assignment> getStudentCompletedAssignments(Long studentId);
}
