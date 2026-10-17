package com.backend.com.minzu.controller;
import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/learning")
public class LearningTutorController {
    @Autowired private LearningTokenService tokens;
    @Autowired private LearningTutorService tutor;
    @PostMapping("/tutor")
    public Object tutor(@RequestHeader(value="X-Learning-Token",required=false)String token,@RequestBody Map<String,Object> input){Long user=tokens.user(token);if(user==null)return ResponseEntity.status(401).body(Result.error("请重新登录小程序后使用AI辅导"));try{return Result.success(tutor.tutor(user,Long.parseLong(input.get("questionId").toString()),Objects.toString(input.get("answer"),""),Objects.toString(input.get("reason"),"")));}catch(Exception ex){return Result.error(ex.getMessage());}}
}
