package com.backend.com.minzu.controller;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.dto.LoginRequest;
import com.backend.com.minzu.entity.User;
import com.backend.com.minzu.service.MailService;
import com.backend.com.minzu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired private com.backend.com.minzu.service.LearningTokenService learningTokens;

    @Autowired
    private MailService mailService;

    // 内存存储验证码（生产环境应使用Redis）
    private final Map<String, String> verifyCodeCache = new ConcurrentHashMap<>();
    private final Map<String, Long> verifyCodeTimeCache = new ConcurrentHashMap<>();

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return Result.error("账号或密码不能为空");
        }

        User user = userService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 直接使用数据库中的角色，不再校验前端传入的role
        String userRole = user.getRole() != null ? user.getRole() : "party_member";

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
        data.put("avatar", user.getAvatar());
        data.put("role", userRole);
        data.put("school", user.getSchool());
        data.put("email", user.getEmail());
        data.put("totalScore", user.getTotalScore() != null ? user.getTotalScore() : 0);
        data.put("identity", user.getIdentity()); // 身份JSON
        data.put("authToken", learningTokens.issue(user.getId()));

        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody User user) {
        try {
            // 校验验证码
            String email = user.getEmail();
            String verifyCode = user.getVerifyCode();
            if (email != null && !email.isEmpty() && verifyCode != null && !verifyCode.isEmpty()) {
                if (!checkVerifyCode(email, verifyCode)) {
                    return Result.error("验证码错误或已过期，请重新获取");
                }
            }

            // 默认角色为party_member
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("party_member");
            }
            boolean ok = userService.register(user);
            if (ok) {
                // 注册成功后清除验证码
                if (email != null) {
                    verifyCodeCache.remove(email);
                    verifyCodeTimeCache.remove(email);
                }
                // 返回完整的用户信息（和登录接口保持一致），让前端能正确存储
                Map<String, Object> data = new HashMap<>();
                data.put("id", user.getId());
                data.put("username", user.getUsername());
                data.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
                data.put("avatar", user.getAvatar());
                data.put("role", user.getRole());
                data.put("school", user.getSchool());
                data.put("email", user.getEmail());
                data.put("totalScore", user.getTotalScore() != null ? user.getTotalScore() : 0);
                data.put("identity", user.getIdentity());
                return Result.success(data);
            } else {
                return Result.error("用户名已存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestParam Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/sendCode")
    public Result<String> sendVerifyCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        if (email == null || email.isEmpty()) {
            return Result.error("邮箱不能为空");
        }

        // 生成6位验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // 存储验证码（有效期5分钟）
        verifyCodeCache.put(email, code);
        verifyCodeTimeCache.put(email, System.currentTimeMillis() + 5 * 60 * 1000);

        // 通过QQ邮箱发送验证码
        boolean sent = mailService.sendVerifyCode(email, code);

        // 邮件未配置或发送失败时，控制台打印验证码作为开发备用
        if (!sent) {
            System.out.println("==========================================");
            System.out.println("  📧 验证码（开发模式 - 邮件未配置或发送失败）");
            System.out.println("  邮箱: " + email);
            System.out.println("  验证码: " + code);
            System.out.println("  有效期: 5分钟");
            System.out.println("  配置QQ邮箱SMTP后即可真实发送邮件");
            System.out.println("==========================================");
            // 开发阶段仍返回成功，方便测试
            return Result.success("验证码已生成（开发模式），请查看控制台");
        }

        return Result.success("验证码已发送到 " + email + "，请查收");
    }

    /**
     * 校验验证码（供内部使用）
     */
    private boolean checkVerifyCode(String email, String code) {
        String savedCode = verifyCodeCache.get(email);
        Long expireTime = verifyCodeTimeCache.get(email);
        if (savedCode == null || expireTime == null) return false;
        if (System.currentTimeMillis() > expireTime) {
            verifyCodeCache.remove(email);
            verifyCodeTimeCache.remove(email);
            return false;
        }
        return savedCode.equals(code);
    }
}
