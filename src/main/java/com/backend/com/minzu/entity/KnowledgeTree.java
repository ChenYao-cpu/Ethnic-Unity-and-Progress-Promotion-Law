package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_tree")
public class KnowledgeTree {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer level;
    private Integer growthValue;
    private Date lastFeedTime;
    private Date createTime;
    private Date updateTime;
}
