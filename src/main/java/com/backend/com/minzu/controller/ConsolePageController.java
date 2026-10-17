package com.backend.com.minzu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConsolePageController {
    @GetMapping("/console")
    public String redirect() { return "redirect:/console/"; }

    @GetMapping("/console/")
    public String index() { return "forward:/console/index.html"; }
}
