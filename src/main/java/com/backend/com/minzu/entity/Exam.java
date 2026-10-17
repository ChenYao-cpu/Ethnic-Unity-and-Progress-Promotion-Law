package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String type;
    private String questions;
    private String userAnswers;
    private Integer score;
    private Integer totalScore;
    private Integer correctCount;
    private Integer timeUsed;
    private Integer status;
    private Date startTime;
    private Date finishTime;
    private Date examTime;
}
