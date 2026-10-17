package com.backend.com.minzu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Service
public class MailService {

    @Value("${spring.mail.host:smtp.qq.com}")
    private String host;

    @Value("${spring.mail.port:465}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    private JavaMailSender mailSender;

    private boolean configured = false;

    @PostConstruct
    public void init() {
        // 如果还没配置真实邮箱，跳过初始化，不会影响启动
        if (username.isEmpty() || password.isEmpty()
                || username.contains("your-qq-email") || password.contains("your-auth-code")) {
            System.out.println("============================================");
            System.out.println("  ⚠ 邮件服务未配置");
            System.out.println("  请在 application.yml 中填写 QQ邮箱 + 授权码");
            System.out.println("  验证码将打印在控制台（开发模式）");
            System.out.println("============================================");
            configured = false;
            return;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        this.mailSender = sender;
        configured = true;
        System.out.println("✅ 邮件服务已配置 → " + username);
    }

    /**
     * 发送纯文本邮件
     */
    public boolean send(String to, String subject, String content) {
        if (!configured || mailSender == null) {
            System.out.println("[邮件] 未配置，跳过发送。收件人: " + to + ", 内容: " + content);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(username);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            System.out.println("✅ 邮件发送成功 → " + to);
            return true;
        } catch (Exception e) {
            System.err.println("❌ 邮件发送失败 → " + to + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送验证码邮件
     */
    public boolean sendVerifyCode(String to, String code) {
        String subject = "【民族团结促进法学习系统】邮箱验证码";
        String content = "您好！\n\n"
                + "您的验证码是：" + code + "\n\n"
                + "验证码 5 分钟内有效，请勿泄露给他人。\n"
                + "如非本人操作，请忽略此邮件。\n\n"
                + "—— 民族团结促进法学习系统";
        return send(to, subject, content);
    }
}
