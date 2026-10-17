package com.backend.com.minzu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("battle_record")
public class BattleRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String battleId;

    private Long playerId1;

    private Long playerId2;

    private Integer player1Score;

    private Integer player2Score;

    private Integer player1CorrectCount;

    private Integer player2CorrectCount;

    private Long winnerId;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
