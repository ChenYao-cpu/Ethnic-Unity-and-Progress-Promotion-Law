package com.backend.com.minzu.controller;   // 根据你的项目结构，包名应该是 com.law.controller

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/hello")
    public String hello() {
        return "民族团结促进法后台启动成功！我是 XSimple 开发的 AI 助手。";
    }
}