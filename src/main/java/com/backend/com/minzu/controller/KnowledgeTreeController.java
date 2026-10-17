package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.KnowledgeTree;
import com.backend.com.minzu.service.StudyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tree")
public class KnowledgeTreeController {
    
    @Autowired
    private StudyService studyService;

    @GetMapping("/my")
    public Result<KnowledgeTree> getMyTree(@RequestParam Long userId) {
        KnowledgeTree tree = studyService.getOrCreateTree(userId);
        return Result.success(tree);
    }

    @PostMapping("/feed")
    public Result<KnowledgeTree> feedTree(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Integer points = Integer.valueOf(params.get("points").toString());
            
            KnowledgeTree tree = studyService.feedTree(userId, points);
            return Result.success(tree);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
