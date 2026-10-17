package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.Assignment;
import com.backend.com.minzu.entity.Course;
import com.backend.com.minzu.entity.User;
import com.backend.com.minzu.mapper.AssignmentMapper;
import com.backend.com.minzu.mapper.CourseMapper;
import com.backend.com.minzu.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

/** Same-origin console adapter. Role and ownership are checked before calling existing services. */
@RestController
@RequestMapping("/api/console")
public class ConsoleController {
    @Autowired private UserService users;
    @Autowired private AssignmentMapper assignments;
    @Autowired private CourseMapper courses;
    @Autowired private ObjectMapper json;
    @Autowired private FileUploadController uploads;
    @Autowired private com.backend.com.minzu.mapper.QuestionMapper questions;
    @Autowired private com.backend.com.minzu.mapper.ReminderMapper reminders;
    @Autowired private com.backend.com.minzu.mapper.UserMapper userMapper;
    @Autowired private com.backend.com.minzu.service.AgentTaskService agents;
    @Autowired private com.backend.com.minzu.service.LearningTutorService tutor;
    @GetMapping("/agents/feedback")
    public Object learningFeedback(HttpServletRequest request){User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));if("legal_officer".equals(u.getRole()))return ResponseEntity.status(403).body(Result.error("请使用管理员账号"));return Result.success(tutor.recent());}

    @GetMapping("/agents/tasks")
    public Object agentTasks(HttpServletRequest request){User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));if("legal_officer".equals(u.getRole()))return ResponseEntity.status(403).body(Result.error("请在审核作业中查看协同记录"));return Result.success(agents.list(u.getId()));}
    @PostMapping("/agents/tasks")
    public Object createAgentTask(@RequestBody Map<String,Object> input,HttpServletRequest request){
        User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));
        if("legal_officer".equals(u.getRole()) || !sameOrigin(request))return ResponseEntity.status(403).body(Result.error("只有管理员可以发起协同出题"));
        try{return Result.success(Map.of("id",agents.create(u.getId(),Objects.toString(input.get("topic"),""),Integer.parseInt(input.get("count").toString()),Integer.parseInt(input.get("type").toString()),Integer.parseInt(input.get("difficulty").toString()))));}
        catch(Exception ex){return Result.error(ex.getMessage());}
    }
    @GetMapping("/agents/tasks/{id}")
    public Object agentTask(@PathVariable long id,HttpServletRequest request){User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));try{return Result.success(agents.get(id,u.getId(),"legal_officer".equals(u.getRole())));}catch(Exception ex){return Result.error(ex.getMessage());}}
    @PostMapping("/agents/tasks/{id}/save")
    public Object saveAgentTask(@PathVariable long id,@RequestBody Map<String,Object> input,HttpServletRequest request){User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));if("legal_officer".equals(u.getRole()) || !sameOrigin(request))return ResponseEntity.status(403).body(Result.error("无权保存作业"));try{return Result.success(Map.of("assignmentId",agents.saveAssignment(id,u.getId(),input)));}catch(Exception ex){return Result.error(ex.getMessage());}}
    @GetMapping("/agents/assignment/{id}")
    public Object assignmentAgents(@PathVariable long id,HttpServletRequest request){User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));Assignment a=assignments.selectById(id);if(a==null || !"legal_officer".equals(u.getRole()) && !u.getId().equals(a.getTeacherId()))return ResponseEntity.status(403).body(Result.error("无权查看"));return Result.success(agents.forAssignment(id));}
    private boolean sameOrigin(HttpServletRequest request){String origin=request.getHeader("Origin");return origin==null || origin.equals(request.getScheme()+"://"+request.getHeader("Host"));}

    @PostMapping("/assignment/{id}/remind-batch")
    @org.springframework.transaction.annotation.Transactional
    public Object remindBatch(@PathVariable long id,@RequestBody Map<String,Object> input,HttpServletRequest request){
        User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));Assignment a=assignments.selectById(id);
        if("legal_officer".equals(u.getRole()) || !sameOrigin(request) || a==null || !u.getId().equals(a.getTeacherId()))return ResponseEntity.status(403).body(Result.error("无权发送提醒"));
        String message=Objects.toString(input.get("message"),"").trim();if(message.isEmpty()||message.length()>500)return Result.error("提醒应为1—500字");
        if(!(input.get("toUserIds") instanceof List) || ((List<?>)input.get("toUserIds")).isEmpty() || ((List<?>)input.get("toUserIds")).size()>200)return Result.error("请选择1—200名接收人");
        Set<Long> ids=new LinkedHashSet<>();for(Object raw:(List<?>)input.get("toUserIds")){long target;try{target=Long.parseLong(raw.toString());}catch(Exception ex){return Result.error("接收人格式错误");}if(userMapper.selectById(target)==null)return Result.error("接收人不存在");ids.add(target);}
        for(long target:ids){com.backend.com.minzu.entity.Reminder r=new com.backend.com.minzu.entity.Reminder();r.setAssignmentId(id);r.setFromUserId(u.getId());r.setToUserId(target);r.setMessage(message);r.setIsRead(0);r.setCreateTime(new Date());reminders.insert(r);}
        return Result.success(Map.of("count",ids.size()));
    }
    @PostMapping("/assignment/{id}/questions")
    @org.springframework.transaction.annotation.Transactional
    public Object addConsoleQuestion(@PathVariable long id,@RequestBody Map<String,Object> input,HttpServletRequest request)throws Exception{
        User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));Assignment a=assignments.selectById(id);
        if("legal_officer".equals(u.getRole()) || !sameOrigin(request) || a==null || !u.getId().equals(a.getTeacherId()))return ResponseEntity.status(403).body(Result.error("无权编辑"));
        if(!Arrays.asList("draft","rejected").contains(a.getReviewStatus()))return Result.error("只能编辑草稿或退回作业");
        com.backend.com.minzu.entity.Question q;
        if(input.get("bankQuestionId")!=null){q=questions.selectById(Long.parseLong(input.get("bankQuestionId").toString()));if(q==null)return Result.error("题目不存在");if(assignmentQuestions(id).stream().anyMatch(link->link.getQuestionId().equals(q.getId())))return Result.error("作业已包含该题");}
        else{String title=Objects.toString(input.get("title"),"").trim(),answer=Objects.toString(input.get("answer"),"").trim();if(title.isEmpty()||answer.isEmpty()||!(input.get("options") instanceof List)||((List<?>)input.get("options")).size()<2)return Result.error("请填写题干、至少两个选项和答案");q=new com.backend.com.minzu.entity.Question();q.setTitle(title);q.setAnswer(answer);q.setOptions(json.writeValueAsString(input.get("options")));q.setAnalysis(Objects.toString(input.get("analysis"),""));q.setType(Integer.parseInt(Objects.toString(input.get("type"),"1")));q.setDifficulty(Integer.parseInt(Objects.toString(input.get("difficulty"),"2")));q.setTeacherId(u.getId());q.setCreateTime(new Date());questions.insert(q);}
        com.backend.com.minzu.entity.AssignmentQuestion link=new com.backend.com.minzu.entity.AssignmentQuestion();link.setAssignmentId(id);link.setQuestionId(q.getId());link.setSortOrder(assignmentQuestions(id).size()+1);links.insert(link);resetQuestionReview(a);return Result.success(true);
    }
    @DeleteMapping("/assignment/{id}/questions/{qid}")
    @org.springframework.transaction.annotation.Transactional
    public Object removeConsoleQuestion(@PathVariable long id,@PathVariable long qid,HttpServletRequest request){User u=current(request);if(u==null)return ResponseEntity.status(401).body(Result.error("请重新登录"));Assignment a=assignments.selectById(id);if("legal_officer".equals(u.getRole())||!sameOrigin(request)||a==null||!u.getId().equals(a.getTeacherId()))return ResponseEntity.status(403).body(Result.error("无权编辑"));if(!Arrays.asList("draft","rejected").contains(a.getReviewStatus()))return Result.error("只能编辑草稿或退回作业");links.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.backend.com.minzu.entity.AssignmentQuestion>().eq(com.backend.com.minzu.entity.AssignmentQuestion::getAssignmentId,id).eq(com.backend.com.minzu.entity.AssignmentQuestion::getQuestionId,qid));resetQuestionReview(a);return Result.success(true);}
    private void resetQuestionReview(Assignment a){int count=assignmentQuestions(a.getId()).size();a.setQuestionCount(count);a.setTotalScore(count*5);a.setReviewStatus("draft");a.setQuestionReview("[]");a.setReviewComment("");assignments.updateById(a);}

    @PostMapping("/assignment/{id}/remind")
    public Object remind(@PathVariable Long id,@RequestBody Map<String,Object> input,HttpServletRequest request) {
        User u=current(request);
        if(u==null) return ResponseEntity.status(401).body(Result.error("请重新登录"));
        String origin=request.getHeader("Origin");
        if(origin!=null && !origin.equals(request.getScheme()+"://"+request.getHeader("Host")))
            return ResponseEntity.status(403).body(Result.error("不允许跨站操作"));
        Assignment a=assignments.selectById(id);
        if("legal_officer".equals(u.getRole()) || a==null || !u.getId().equals(a.getTeacherId()))
            return ResponseEntity.status(403).body(Result.error("无权发送提醒"));
        Long recipient;
        try { recipient=Long.valueOf(String.valueOf(input.get("toUserId"))); } catch(Exception ex) { return Result.error("请选择接收人"); }
        if(userMapper.selectById(recipient)==null) return Result.error("接收人不存在");
        String message=String.valueOf(input.getOrDefault("message","")).trim();
        if(message.isEmpty() || message.length()>500) return Result.error("提醒内容应为1—500字");
        com.backend.com.minzu.entity.Reminder reminder=new com.backend.com.minzu.entity.Reminder();
        reminder.setAssignmentId(id); reminder.setFromUserId(u.getId()); reminder.setToUserId(recipient);
        reminder.setMessage(message); reminder.setIsRead(0); reminder.setCreateTime(new Date()); reminders.insert(reminder);
        return Result.success(true);
    }

    @PostMapping("/upload-image")
    public Object upload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, HttpServletRequest request) {
        User u=current(request);
        if(u==null) return ResponseEntity.status(401).body(Result.error("请重新登录"));
        String origin=request.getHeader("Origin");
        if(origin!=null && !origin.equals(request.getScheme()+"://"+request.getHeader("Host")))
            return ResponseEntity.status(403).body(Result.error("不允许跨站操作"));
        if("legal_officer".equals(u.getRole())) return ResponseEntity.status(403).body(Result.error("法务不能上传课程封面"));
        if(!Arrays.asList("image/png","image/jpeg","image/webp").contains(file.getContentType()))
            return Result.error("请上传PNG、JPEG或WebP图片");
        return uploads.uploadImage(file);
    }

    /** Clone edited questions so another assignment or the public question bank is not changed. */
    @PutMapping("/assignment/{assignmentId}/question/{questionId}")
    @org.springframework.transaction.annotation.Transactional
    public Object editQuestion(@PathVariable Long assignmentId, @PathVariable Long questionId,
                               @RequestBody Map<String,Object> input, HttpServletRequest request) throws Exception {
        User u=current(request);
        if(u==null) return ResponseEntity.status(401).body(Result.error("请重新登录"));
        String origin=request.getHeader("Origin");
        if(origin!=null && !origin.equals(request.getScheme()+"://"+request.getHeader("Host")))
            return ResponseEntity.status(403).body(Result.error("不允许跨站操作"));
        Assignment a=assignments.selectById(assignmentId);
        if("legal_officer".equals(u.getRole()) || a==null || !u.getId().equals(a.getTeacherId()))
            return ResponseEntity.status(403).body(Result.error("无权修改此作业"));
        if(!Arrays.asList("draft","rejected").contains(a.getReviewStatus())) return Result.error("只能修改草稿或已退回作业");
        List<com.backend.com.minzu.entity.AssignmentQuestion> related=assignmentQuestions(assignmentId);
        com.backend.com.minzu.entity.AssignmentQuestion link=related.stream().filter(q->questionId.equals(q.getQuestionId())).findFirst().orElse(null);
        com.backend.com.minzu.entity.Question old=questions.selectById(questionId);
        if(link==null || old==null) return Result.error("题目不存在");
        String title=String.valueOf(input.getOrDefault("title"," ")).trim();
        String answer=String.valueOf(input.getOrDefault("answer", "")).trim();
        Object options=input.get("options");
        if(title.isEmpty() || answer.isEmpty() || !(options instanceof List) || ((List<?>)options).isEmpty())
            return Result.error("题干、选项、答案不能为空");
        com.backend.com.minzu.entity.Question q=new com.backend.com.minzu.entity.Question();
        org.springframework.beans.BeanUtils.copyProperties(old,q);
        q.setId(null); q.setTeacherId(u.getId()); q.setCreateTime(new Date());
        q.setTitle(title); q.setAnswer(answer); q.setOptions(json.writeValueAsString(options));
        q.setAnalysis(String.valueOf(input.getOrDefault("analysis","")));
        questions.insert(q); link.setQuestionId(q.getId()); links.updateById(link);
        a.setReviewStatus("draft"); a.setQuestionReview("[]"); a.setReviewComment(""); assignments.updateById(a);
        return Result.success(q.getId());
    }

    @PostMapping("/login")
    public Object login(@RequestBody Map<String,String> input, HttpServletRequest request) {
        User user = users.login(input.get("username"), input.get("password"));
        if (user == null) return Result.error("账号或密码错误");
        if (!Arrays.asList("admin", "teacher", "legal_officer").contains(user.getRole()))
            return Result.error("请使用管理员或法务账号；党员学习请使用小程序");
        String portal=input.get("portal");
        if(portal!=null) {
            if(!Arrays.asList("admin","legal_officer").contains(portal)) return Result.error("请选择有效的登录入口");
            boolean legal="legal_officer".equals(user.getRole());
            if(legal != "legal_officer".equals(portal))
                return Result.error(legal?"该账号属于法务审核端，请切换入口":"该账号属于管理员端，请切换入口");
        }
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(1800);
        session.setAttribute("consoleUserId", user.getId());
        Map<String,Object> info = new HashMap<>();
        info.put("id", user.getId()); info.put("role", user.getRole());
        info.put("nickname", user.getNickname() == null ? user.getUsername() : user.getNickname());
        return Result.success(info);
    }
    @PostMapping("/logout")
    public Object logout(HttpServletRequest request) {
        HttpSession s = request.getSession(false); if (s != null) s.invalidate();
        return Result.success(true);
    }
    @GetMapping("/me")
    public Object me(HttpServletRequest request) {
        User u = current(request);
        if (u == null) return ResponseEntity.status(401).body(Result.error("请重新登录"));
        Map<String,Object> info = new HashMap<>(); info.put("id",u.getId());
        info.put("role",u.getRole()); info.put("nickname",u.getNickname()==null?u.getUsername():u.getNickname());
        return Result.success(info);
    }
    private User current(HttpServletRequest request) {
        HttpSession s=request.getSession(false);
        if(s==null || s.getAttribute("consoleUserId")==null) return null;
        User u=users.getUserById((Long)s.getAttribute("consoleUserId"));
        return u!=null && Arrays.asList("admin","teacher","legal_officer").contains(u.getRole()) ? u : null;
    }
    @RequestMapping(value="/data",method={RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT,RequestMethod.DELETE})
    public Object data(@RequestParam String path, @RequestBody(required=false) Map<String,Object> body,
                       HttpServletRequest request) throws Exception {
        User u=current(request);
        if(u==null) return ResponseEntity.status(401).body(Result.error("登录已过期"));
        // Reject cross-origin mutation requests; all browser calls use same-origin JSON.
        String origin=request.getHeader("Origin");
        String expected=request.getScheme()+"://"+request.getHeader("Host");
        if(origin!=null && !origin.equals(expected))
            return ResponseEntity.status(403).body(Result.error("不允许跨站操作"));
        String method=request.getMethod(); String route=path.split("\\?",2)[0];
        boolean legal="legal_officer".equals(u.getRole());
        boolean permitted=false;
        if(legal) {
            permitted=("GET".equals(method) && route.matches("/review/(pending|history|detail/\\d+|course/pending)"))
                || ("POST".equals(method) && route.matches("/review/(\\d+/(approve-question/\\d+|reject-question/\\d+|submit-review)|course/\\d+/(approve|reject))"));
        } else {
            permitted=("GET".equals(method) && route.matches("/(assignment/teacher/list|assignment/detail/\\d+|assignment/\\d+/questions|course/teacher/list|course/detail/\\d+|teacher/assignment/\\d+/(submissions|statistics|progress)|question/list)"))
                || ("POST".equals(method) && route.matches("/(assignment/create|assignment/\\d+/submit-review|assignment/publish/\\d+|course/create|course/\\d+/(submit-review|publish|unpublish)|ai/generateQuestions)"))
                || ("PUT".equals(method) && route.matches("/(assignment|course)/update/\\d+"))
                || ("DELETE".equals(method) && route.matches("/(assignment|course)/\\d+"));
        }
        if(!permitted || !path.matches("/[a-zA-Z0-9/_-]+(?:\\?[a-zA-Z0-9=&]+)?"))
            return ResponseEntity.status(403).body(Result.error("无权执行此操作"));
        if(body==null) body=new HashMap<>();
        if(!legal) {
            // The browser cannot select a different owner or AI user.
            if(route.equals("/assignment/teacher/list") || route.equals("/course/teacher/list")) path=route+"?teacherId="+u.getId();
            body.put("teacherId",u.getId()); body.put("userId",u.getId());
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("/(?:assignment|course)/(?:update/|publish/|detail/)?(\\d+)(?:/.*)?").matcher(route);
            java.util.regex.Matcher t=java.util.regex.Pattern.compile("/teacher/assignment/(\\d+)/.*").matcher(route);
            Long id=null; boolean course=route.startsWith("/course/");
            if(m.matches()) id=Long.valueOf(m.group(1)); else if(t.matches()) id=Long.valueOf(t.group(1));
            if(id!=null) {
                Long owner;
                if(course) { Course c=courses.selectById(id); owner=c==null?null:c.getTeacherId(); }
                else { Assignment a=assignments.selectById(id); owner=a==null?null:a.getTeacherId(); }
                if(!u.getId().equals(owner)) return ResponseEntity.status(403).body(Result.error("只能操作本人创建的内容"));
                if(course && "PUT".equals(method) && "pending_review".equals(courses.selectById(id).getReviewStatus()))
                    return Result.error("审核中的课程不能直接修改");
                if(!course) {
                    Assignment a=assignments.selectById(id);
                    if(route.startsWith("/assignment/publish/") && !"approved".equals(a.getReviewStatus()))
                        return Result.error("只有法务审核通过的作业才能发布");
                    if(("PUT".equals(method) || route.endsWith("submit-review")) && Arrays.asList("pending_review","published").contains(a.getReviewStatus()))
                        return Result.error("审核中或已发布的作业不能直接修改或重复提交");
                    if("PUT".equals(method)) {
                        Map<String,Object> clean=new HashMap<>();
                        for(String key:Arrays.asList("title","description","deadline")) if(body.containsKey(key)) clean.put(key,body.get(key));
                        clean.put("reviewStatus","draft"); clean.put("questionReview","[]"); clean.put("reviewComment","");
                        body=clean;
                    }
                    if(route.endsWith("submit-review")) {
                        a.setQuestionReview("[]"); a.setReviewComment(""); assignments.updateById(a);
                    }
                }
            }
        } else if("POST".equals(method) && route.startsWith("/review/") && !route.startsWith("/review/course/")) {
            long id=Long.parseLong(route.split("/")[2]); Assignment a=assignments.selectById(id);
            if(a==null || !"pending_review".equals(a.getReviewStatus())) return Result.error("该作业已不在待审核状态，请刷新");
            if(route.contains("-question/")) {
                String questionId=route.substring(route.lastIndexOf('/')+1);
                if(assignmentQuestions(id).stream().noneMatch(q->String.valueOf(q.getQuestionId()).equals(questionId)))
                    return Result.error("该题不属于当前作业");
            }
            if(route.endsWith("submit-review")) {
                java.util.List<com.backend.com.minzu.entity.AssignmentQuestion> qs = assignmentQuestions(id);
                java.util.List<Map<String,Object>> reviews=a.getQuestionReview()==null || a.getQuestionReview().trim().isEmpty()?Collections.emptyList():json.readValue(a.getQuestionReview(),new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>(){});
                for(com.backend.com.minzu.entity.AssignmentQuestion q:qs)
                    if(reviews.stream().noneMatch(r -> String.valueOf(q.getQuestionId()).equals(String.valueOf(r.get("questionId"))) && Arrays.asList("approved","rejected").contains(r.get("status"))))
                        return Result.error("请先完成全部题目的逐题审核");
                if(qs.isEmpty()) return Result.error("无题目，不能通过审核");
            }
        } else if(legal && "POST".equals(method) && route.startsWith("/review/course/")) {
            long id=Long.parseLong(route.split("/")[3]); Course c=courses.selectById(id);
            if(c==null || !"pending_review".equals(c.getReviewStatus())) return Result.error("该课程已不在待审核状态，请刷新");
        }
        SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(120000);
        HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response=new RestTemplate(factory).exchange("http://127.0.0.1:"+request.getLocalPort()+"/api"+path,
                HttpMethod.valueOf(method),new HttpEntity<>("GET".equals(method)?null:body,headers),String.class);
        return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(response.getBody());
    }
    @Autowired private com.backend.com.minzu.mapper.AssignmentQuestionMapper links;
    private List<com.backend.com.minzu.entity.AssignmentQuestion> assignmentQuestions(long id) {
        return links.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.backend.com.minzu.entity.AssignmentQuestion>()
            .eq(com.backend.com.minzu.entity.AssignmentQuestion::getAssignmentId,id));
    }
}
