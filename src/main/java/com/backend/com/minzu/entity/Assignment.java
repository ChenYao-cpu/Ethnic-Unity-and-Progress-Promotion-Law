package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
@TableName("assignment")
public class Assignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private Integer questionCount;
    private Integer singleCount;
    private Integer multiCount;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;
    
    private Integer status;

    private String reviewStatus;  // draft / pending_review / approved / rejected / published
    private String reviewComment; // 审核意见
    private String questionReview; // 逐题审核结果JSON: [{"questionId":1,"status":"approved","comment":""}]
    private Integer totalScore;
    private Integer generateType;
    private String category;
    private Integer difficulty;
    private Date createTime;
    private Date updateTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String teacherName;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<AssignmentQuestion> questions;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<Long> questionIds;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer submitCount;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer totalCount;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer avgScore;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.Date submitTime;


}
