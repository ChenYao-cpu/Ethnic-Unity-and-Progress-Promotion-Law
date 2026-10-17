package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("study_record")
public class StudyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("course_id")
    private Long courseId;

    @TableField("study_duration")
    private Integer studyDuration;

    private Integer progress;

    @TableField(exist = false)
    private Integer currentPosition;

    @TableField(exist = false)
    private Integer status;

    @TableField("study_time")
    private Date studyTime;

    @TableField(exist = false)
    private Date updateTime;
}
