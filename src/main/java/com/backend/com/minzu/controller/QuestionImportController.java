package com.backend.com.minzu.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Question;
import com.backend.com.minzu.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@RestController
@RequestMapping("/api/question")
public class QuestionImportController {

    @Autowired
    private QuestionMapper questionMapper;

    @PostMapping("/import")
    public Result<String> importQuestions() {
        try {
            System.out.println("开始导入题库数据...");
            
            // 读取 JSON 文件
            ClassPathResource resource = new ClassPathResource("question-bank.json");
            InputStream inputStream = resource.getInputStream();
            String jsonContent = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            
            JSONObject jsonObject = JSON.parseObject(jsonContent);
            
            // 导入单选题
            JSONArray singleQuestions = jsonObject.getJSONArray("single");
            int singleCount = 0;
            for (int i = 0; i < singleQuestions.size(); i++) {
                JSONObject q = singleQuestions.getJSONObject(i);
                Question question = new Question();
                question.setType(1); // 单选题
                question.setTitle(q.getString("question"));
                question.setOptions(q.getJSONArray("options").toJSONString());
                question.setAnswer(q.getString("answer"));
                question.setAnalysis(q.getString("analysis"));
                question.setDifficulty(2);
                question.setCreateTime(new Date());
                questionMapper.insert(question);
                singleCount++;
            }
            System.out.println("单选题导入完成：" + singleCount + " 题");
            
            // 导入多选题
            JSONArray multiQuestions = jsonObject.getJSONArray("multi");
            int multiCount = 0;
            for (int i = 0; i < multiQuestions.size(); i++) {
                JSONObject q = multiQuestions.getJSONObject(i);
                Question question = new Question();
                question.setType(2); // 多选题
                question.setTitle(q.getString("question"));
                question.setOptions(q.getJSONArray("options").toJSONString());
                question.setAnswer(q.getString("answer"));
                question.setAnalysis(q.getString("analysis"));
                question.setDifficulty(2);
                question.setCreateTime(new Date());
                questionMapper.insert(question);
                multiCount++;
            }
            System.out.println("多选题导入完成：" + multiCount + " 题");
            
            return Result.success("题库导入成功！单选题：" + singleCount + " 道，多选题：" + multiCount + " 道");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("题库导入失败：" + e.getMessage());
        }
    }
}
