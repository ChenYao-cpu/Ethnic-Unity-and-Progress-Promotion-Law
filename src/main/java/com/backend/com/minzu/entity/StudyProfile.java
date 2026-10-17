package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("study_profile")
public class StudyProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer regulationScore;
    private Integer caseScore;
    private Integer answerScore;
    private Integer noteScore;
    private Integer videoScore;
    private Date updateTime;
}
