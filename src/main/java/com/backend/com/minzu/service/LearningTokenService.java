package com.backend.com.minzu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LearningTokenService {
    @Autowired private JdbcTemplate db;
    @PostConstruct public void init(){db.execute("CREATE TABLE IF NOT EXISTS learning_session (token_hash VARCHAR(64) PRIMARY KEY,user_id BIGINT NOT NULL,expires_at DATETIME NOT NULL,INDEX idx_learning_user(user_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");}
    private String hash(String token){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b));return s.toString();}catch(Exception e){throw new IllegalStateException("令牌生成失败");}}
    public String issue(long user){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);db.update("DELETE FROM learning_session WHERE expires_at<NOW()");db.update("INSERT INTO learning_session(token_hash,user_id,expires_at) VALUES(?,?,DATE_ADD(NOW(),INTERVAL 1 DAY))",hash(token),user);return token;}
    public Long user(String token){if(token==null || token.length()>100)return null;List<Long> ids=db.query("SELECT user_id FROM learning_session WHERE token_hash=? AND expires_at>NOW()",(rs,n)->rs.getLong(1),hash(token));return ids.isEmpty()?null:ids.get(0);}
}
