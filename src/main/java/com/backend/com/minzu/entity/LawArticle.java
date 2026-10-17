package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("law_article")
public class LawArticle {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String chapter;
    private String articleNumber;
    private String content;
    private String keywords;
    private Integer sortOrder;
    private Date createTime;
}
