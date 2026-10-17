package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("assignment_submission")
public class AssignmentSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long assignmentId;
    private Long studentId;
    private Integer score;
    private Date submitTime;
    private String answers;
    private Integer status;
    private Date createTime;
}
