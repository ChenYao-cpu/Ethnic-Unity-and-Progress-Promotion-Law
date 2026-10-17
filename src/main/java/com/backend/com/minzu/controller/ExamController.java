package com.backend.com.minzu.controller;

import com.backend.com.minzu.entity.Question;
import com.backend.com.minzu.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam")
public class ExamController {
    @Autowired
    private ExamService examService;

    @GetMapping("/start")
    public Map<String, Object> start(@RequestParam(defaultValue = "10") int size) {
        List<Question> paper = examService.generateExamPaper(null, size);
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("paper", paper);
        return res;
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody SubmitRequest req) {
        Long userId = req.getUserId();
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("code", 400);
            res.put("msg", "缺少用户信息，请重新登录");
            return res;
        }
        if (req.getQuestionIds() == null || req.getAnswers() == null
                || req.getQuestionIds().size() != req.getAnswers().size()) {
            res.put("code", 400);
            res.put("msg", "答题数据不完整");
            return res;
        }
        int score = examService.submitExam(userId, req.getQuestionIds(), req.getAnswers());
        res.put("code", 200);
        res.put("score", score);
        return res;
    }
}

class SubmitRequest {
    private Long userId;
    private List<Integer> questionIds;
    private List<String> answers;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<Integer> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Integer> questionIds) { this.questionIds = questionIds; }
    public List<String> getAnswers() { return answers; }
    public void setAnswers(List<String> answers) { this.answers = answers; }
}
