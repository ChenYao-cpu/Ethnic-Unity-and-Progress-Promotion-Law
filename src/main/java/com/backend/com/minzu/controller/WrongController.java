package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.WrongQuestion;
import com.backend.com.minzu.service.WrongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wrong")
public class WrongController {
    
    @Autowired
    private WrongService wrongService;

    @GetMapping("/list")
    public Result<List<WrongQuestion>> list(@RequestParam Long userId) {
        List<WrongQuestion> list = wrongService.getUserWrongs(userId);
        return Result.success(list);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        wrongService.removeWrong(id);
        return Result.success();
    }
}
