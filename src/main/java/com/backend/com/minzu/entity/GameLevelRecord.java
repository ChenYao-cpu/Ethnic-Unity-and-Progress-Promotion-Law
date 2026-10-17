package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("game_level_record")
public class GameLevelRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer levelId;
    private String levelType;
    private Integer score;
    private Integer timeUsed;
    private Double accuracy;
    private Integer status;
    private Integer bestScore;
    private Date completeTime;
    private Date createTime;
    private Date updateTime;
}
