package com.backend.com.minzu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.*;

/** Independent agent turns share the configured model, never share hidden conversations. */
@Component
public class AgentModelClient {
    @Value("${ai.deepseek.api-key:}") private String key;
    @Value("${ai.deepseek.url:https://api.deepseek.com/v1/chat/completions}") private String url;
    @Value("${ai.deepseek.model:deepseek-chat}") private String model;
    private final ObjectMapper json=new ObjectMapper();
    public String modelName(){return model;}
    public JsonNode call(String system,String input) {
        if(key==null || key.trim().isEmpty()) throw new IllegalStateException("未配置模型API密钥，请设置DEEPSEEK_API_KEY");
        SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10000);f.setReadTimeout(90000);
        Map<String,Object> body=new LinkedHashMap<>();body.put("model",model);body.put("stream",false);
        body.put("temperature",0.15);body.put("max_tokens",6000);
        body.put("response_format",Collections.singletonMap("type","json_object"));
        body.put("messages",Arrays.asList(Map.of("role","system","content",system),Map.of("role","user","content",input)));
        HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);h.setBearerAuth(key);
        try {
            ResponseEntity<String> response=new RestTemplate(f).postForEntity(url,new HttpEntity<>(body,h),String.class);
            JsonNode envelope=json.readTree(response.getBody());
            String text=envelope.path("choices").path(0).path("message").path("content").asText("").trim();
            if(text.startsWith("```")) text=text.replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","");
            if(text.isEmpty()) throw new IllegalStateException("模型未返回内容");
            return json.readTree(text);
        } catch(HttpStatusCodeException ex) {throw new IllegalStateException("模型服务返回HTTP "+ex.getRawStatusCode()+"，未使用本地题目替代");}
        catch(ResourceAccessException ex) {throw new IllegalStateException("模型连接失败或超时，未使用本地题目替代");}
        catch(IllegalStateException ex){throw ex;}
        catch(Exception ex){throw new IllegalStateException("模型输出不是有效JSON，请重试");}
    }
}
