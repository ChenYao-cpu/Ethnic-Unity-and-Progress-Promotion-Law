package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String role;
    private String school;
    private String identity;  // JSON: {"party_member":true,"admin":false,"legal_officer":false}
    private Integer totalScore;
    private Date createTime;
    private Date updateTime;

    /** 验证码（仅用于注册传参，不存库） */
    @TableField(exist = false)
    private String verifyCode;
}
