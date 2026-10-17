package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.entity.BattleRecord;
import com.backend.com.minzu.mapper.BattleRecordMapper;
import com.backend.com.minzu.service.BattleRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BattleRecordServiceImpl extends ServiceImpl<BattleRecordMapper, BattleRecord> implements BattleRecordService {
}
