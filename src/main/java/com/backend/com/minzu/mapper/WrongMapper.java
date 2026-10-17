// WrongMapper.java
package com.backend.com.minzu.mapper;

import com.backend.com.minzu.entity.WrongQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
// 不要使用 import org.springframework.stereotype.Repository;

@Mapper
public interface WrongMapper extends BaseMapper<WrongQuestion> {
}