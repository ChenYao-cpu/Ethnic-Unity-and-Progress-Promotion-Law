package com.backend.com.minzu.controller;
import com.backend.com.minzu.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
public class AgentWorkflowCheck {
    static void check(boolean b,String message){ConsoleBoundaryCheck.check(b,message);}
    static Map<String,Object> question(){Map<String,Object> q=new LinkedHashMap<>();q.put("title","测试题干，仅用于内存验证");q.put("options",new ArrayList<>(Arrays.asList("选项甲","选项乙")));q.put("answer","A");q.put("analysis","测试解析");q.put("articleId",1);q.put("quote","用于验证引用的测试原文内容");return q;}
    static boolean rejected(Runnable action){try{action.run();return false;}catch(IllegalArgumentException e){return true;}}
    public static void main(String[] args)throws Exception{
        List<Map<String,Object>> sources=Collections.singletonList(Map.of("articleId",1,"content","这是用于验证引用的测试原文内容，并非真实法条。"));
        Map<String,Object> q=question();
        check(AgentTaskService.validate(List.of(q),sources,1).isEmpty(),"valid supported question");
        q=question();q.put("articleId",9);check(!AgentTaskService.validate(List.of(q),sources,1).isEmpty(),"invented article blocked");
        q=question();q.put("quote","不在来源中的虚构引用内容");check(!AgentTaskService.validate(List.of(q),sources,1).isEmpty(),"invented quote blocked");
        q=question();q.put("answer","F");check(!AgentTaskService.validate(List.of(q),sources,1).isEmpty(),"answer outside options blocked");
        q=question();q.put("options",List.of("重复","重复"));check(!AgentTaskService.validate(List.of(q),sources,1).isEmpty(),"duplicate options blocked");
        check(!AgentTaskService.validate(List.of(question(),question()),sources,1).isEmpty(),"duplicate questions blocked");
        check(!AgentTaskService.validate(List.of(question()),sources,2).isEmpty(),"single-answer multiselect blocked");
        q=question();q.put("options",List.of("A","B","C"));check(!AgentTaskService.validate(List.of(q),sources,3).isEmpty(),"malformed judgement blocked");
        AgentTaskService service=new AgentTaskService();ObjectMapper json=new ObjectMapper();ConsoleBoundaryCheck.inject(service,"json",json);
        final var duplicate=json.readTree("{\"reviews\":[{\"index\":0,\"approved\":true,\"reason\":\"依据充分\"},{\"index\":0,\"approved\":true,\"reason\":\"重复题号\"}]}");
        check(rejected(()->service.decodeReviews(duplicate,2)),"duplicate reviewer indices blocked");
        final var missing=json.readTree("{\"reviews\":[]}");check(rejected(()->service.decodeReviews(missing,1)),"missing review blocked");
        final var strings=json.readTree("{\"reviews\":[{\"index\":0,\"approved\":\"true\",\"reason\":\"格式不符\"}]}");check(rejected(()->service.decodeReviews(strings,1)),"nonboolean approval blocked");
        final var empty=json.readTree("{\"questions\":[]}");check(rejected(()->service.decodeQuestions(empty,1)),"missing generated questions not fabricated");
        final var reordered=json.readTree("{\"reviews\":[{\"index\":1,\"approved\":false,\"reason\":\"需修改\"},{\"index\":0,\"approved\":true,\"reason\":\"依据充分\"}]}");
        check(Boolean.TRUE.equals(service.decodeReviews(reordered,2).get(0).get("approved")),"review ordering aligned by question index");
        AgentModelClient model=new AgentModelClient();ConsoleBoundaryCheck.inject(model,"key","");boolean failed=false;try{model.call("test","test");}catch(IllegalStateException e){failed=true;}check(failed,"missing model credentials fail without fallback");
        System.out.println("14 agent workflow checks passed");
    }
}
