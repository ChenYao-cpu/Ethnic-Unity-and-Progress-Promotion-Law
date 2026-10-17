package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.dto.AIRequestDTO;
import com.backend.com.minzu.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * 智能问答接口
     */
    @PostMapping("/ask")
    public Object ask(@RequestBody AIRequestDTO request) {
        return aiService.askQuestion(request.getUserId(), request.getQuestion());
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history")
    public Object getHistory(@RequestParam Long userId) {
        return aiService.getChatHistory(userId);
    }

    /**
     * 法条解读接口
     */
    @PostMapping("/interpret")
    public Object interpretLaw(@RequestBody AIRequestDTO request) {
        return aiService.interpretLaw(request.getUserId(), request.getKeyword());
    }
    
    /**
     * RAG智能问答 —— 检索增强生成
     * 先检索相关法条，再作为上下文注入提示词，调用DeepSeek生成回答
     */
    @PostMapping("/rag")
    public Object askRAG(@RequestBody AIRequestDTO request) {
        return aiService.askRAG(request.getUserId(), request.getQuestion());
    }

    /**
     * 微调模型问答 —— 基于完整法律知识库的专业问答
     * 将全部法条+立法解读作为领域知识，模拟微调后的法律专家模型
     */
    @PostMapping("/finetuned")
    public Object askFineTuned(@RequestBody AIRequestDTO request) {
        return aiService.askFineTuned(request.getUserId(), request.getQuestion());
    }

    /**
     * AI生成题目接口
     */
    @PostMapping("/generateQuestions")
    public Result<List<Map<String, Object>>> generateQuestions(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Integer questionCount = params.get("questionCount") != null ? 
                Integer.parseInt(params.get("questionCount").toString()) : 5;
            Integer questionType = params.get("questionType") != null ? 
                Integer.parseInt(params.get("questionType").toString()) : 1;
            Integer difficulty = params.get("difficulty") != null ? 
                Integer.parseInt(params.get("difficulty").toString()) : 2;
            
            List<Map<String, Object>> questions = aiService.generateQuestions(userId, questionCount, questionType, difficulty);
            return Result.success(questions);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成题目失败: " + e.getMessage());
        }
    }
}