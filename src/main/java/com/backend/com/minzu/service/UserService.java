// UserService.java
package com.backend.com.minzu.service;

import com.backend.com.minzu.entity.User;

public interface UserService {
    User login(String username, String password);
    boolean register(User user);
    User getUserById(Long userId);
    boolean updateUser(User user);
}
