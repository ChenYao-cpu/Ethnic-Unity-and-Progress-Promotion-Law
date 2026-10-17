package com.backend.com.minzu.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.lawstudy.mapper")
public class MybatisConfig {
}
