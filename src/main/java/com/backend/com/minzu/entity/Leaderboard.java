package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("leaderboard")
public class Leaderboard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer totalScore;
    private Integer levelCount;
    private Integer achievementCount;
    private Integer rank;
    private Date updateTime;
}
