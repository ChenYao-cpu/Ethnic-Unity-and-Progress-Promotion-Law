package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("study_checkin")
public class StudyCheckin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Date checkinDate;
    private Integer continuousDays;
    private Integer growthValue;
    private Date createTime;
}
