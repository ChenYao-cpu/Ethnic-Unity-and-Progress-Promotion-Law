package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("study_report")
public class StudyReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reportType;
    private Date startDate;
    private Date endDate;
    private Integer activeDays;
    private Integer totalDuration;
    private Integer courseCompleted;
    private Integer questionsAnswered;
    private Double correctRate;
    private String weakPoints;
    private String recommendations;
    private Integer growthValue;
    private Date createTime;
}
