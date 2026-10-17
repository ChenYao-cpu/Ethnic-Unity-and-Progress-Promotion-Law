package com.backend.com.minzu.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role; // student 或 teacher
}
