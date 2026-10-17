package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String options;
    
    private String answer;
    
    private String analysis;
    
    private Integer type;
    
    private Integer difficulty;
    
    private Date createTime;
    
    // teacher_id 字段
    private Long teacherId;
    
    /**
     * 题目配图URL（可选）
     */
    private String image;

    @TableField(exist = false)
    private Integer category;

    @TableField(exist = false)
    private String knowledgePoint;
    
    @TableField(exist = false)
    private Date updateTime;
    
    public Integer getChapter() {
        return 0;
    }
}