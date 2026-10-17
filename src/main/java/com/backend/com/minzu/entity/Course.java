package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String videoUrl;
    private String cover;
    private String description;
    private String content;
    private Integer duration;
    private String category;
    private Integer points;
    private Integer sortOrder;
    private Integer status;
    /** 审核状态：draft/pending_review/approved/rejected/published */
    private String reviewStatus;
    /** 审核意见 */
    private String reviewComment;
    /** 上传者（教师/管理员ID） */
    private Long teacherId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updateTime;
}
