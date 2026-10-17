package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("assignment_question")
public class AssignmentQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assignmentId;
    private Long questionId;
    private Integer sortOrder;
    
    // 关键：非数据库字段，用于存储具体的题目详情
    @TableField(exist = false)
    private Question question;
}