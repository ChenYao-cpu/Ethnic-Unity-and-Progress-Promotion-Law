package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.*;
import com.backend.com.minzu.mapper.*;
import com.backend.com.minzu.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;

/** Dependency-free boundary checks. No database writes or real accounts. */
public class ConsoleBoundaryCheck {
    @SuppressWarnings("unchecked")
    static <T> T stub(Class<T> type, BiFunction<String,Object[],Object> behavior) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->behavior.apply(m.getName(),a));
    }
    static void inject(Object target,String name,Object value) throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    static void check(boolean value,String name) {if(!value)throw new AssertionError(name);System.out.println("PASS "+name);}
    static boolean denied(Object result,int status) {return result instanceof ResponseEntity && ((ResponseEntity<?>)result).getStatusCodeValue()==status;}
    static boolean error(Object result) {return result instanceof Result && ((Result<?>)result).getCode()!=200;}
    static HttpServletRequest request(Long id,String method,String origin) {
        HttpSession session=stub(HttpSession.class,(name,args)->name.equals("getAttribute")?id:null);
        return stub(HttpServletRequest.class,(name,args)->{
            switch(name) {
                case "getSession":return id==null?null:session;
                case "getMethod":return method;
                case "getScheme":return "http";
                case "getHeader":return "Origin".equals(args[0])?origin:"localhost:8080";
                case "getLocalPort":return 8080;
                default:return null;
            }
        });
    }
    public static void main(String[] args) throws Exception {
        ConsoleController c=new ConsoleController();User u=new User();u.setId(7L);u.setRole("admin");
        Assignment a=new Assignment();a.setId(11L);a.setTeacherId(7L);a.setReviewStatus("draft");
        inject(c,"users",stub(UserService.class,(name,params)->u));
        inject(c,"assignments",stub(AssignmentMapper.class,(name,params)->name.equals("selectById")?a:1));
        inject(c,"json",new ObjectMapper());
        check(denied(c.data("/assignment/teacher/list",null,request(null,"GET",null)),401),"unauthenticated access");
        u.setRole("legal_officer");
        check(denied(c.data("/assignment/create",new HashMap<>(),request(7L,"POST",null)),403),"legal cannot create assignments");
        u.setRole("admin");
        check(denied(c.data("/review/pending",null,request(7L,"GET",null)),403),"admin cannot access legal review");
        check(denied(c.data("/assignment/create",new HashMap<>(),request(7L,"POST","https://foreign.example")),403),"cross-site mutation");
        check(denied(c.data("/../user/info",null,request(7L,"GET",null)),403),"path escape blocked");
        a.setTeacherId(8L);
        check(denied(c.data("/assignment/11/questions",null,request(7L,"GET",null)),403),"other owner blocked");
        a.setTeacherId(7L);
        check(error(c.data("/assignment/publish/11",new HashMap<>(),request(7L,"POST",null))),"unreviewed publication blocked");
        a.setReviewStatus("pending_review");
        check(error(c.data("/assignment/update/11",new HashMap<>(),request(7L,"PUT",null))),"pending content edits blocked");
        u.setRole("legal_officer");
        AssignmentQuestion q=new AssignmentQuestion();q.setQuestionId(21L);
        inject(c,"links",stub(AssignmentQuestionMapper.class,(name,params)->Arrays.asList(q)));
        check(error(c.data("/review/11/submit-review",new HashMap<>(),request(7L,"POST",null))),"incomplete review blocked");
        check(error(c.data("/review/11/approve-question/99",new HashMap<>(),request(7L,"POST",null))),"unrelated question blocked");
        Map<String,String> credentials=new HashMap<>();credentials.put("portal","admin");
        check(error(c.login(credentials,request(null,"POST",null))),"legal account cannot use admin portal");
        u.setRole("admin");credentials.put("portal","legal_officer");
        check(error(c.login(credentials,request(null,"POST",null))),"admin account cannot use legal portal");
        System.out.println("12 boundary checks passed");
    }
}
