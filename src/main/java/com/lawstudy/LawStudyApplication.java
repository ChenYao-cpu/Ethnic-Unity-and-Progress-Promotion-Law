package com.lawstudy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.lawstudy", "com.backend.com.minzu"})
@MapperScan(basePackages = {"com.lawstudy.mapper", "com.backend.com.minzu.mapper"})
public class LawStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LawStudyApplication.class, args);
    }
}
