package com.example.rec.service;

import com.example.rec.model.User;
import com.example.rec.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 用户注册
     * @param user 前端传入的用户信息(username, password)
     * @return 注册成功的用户实体
     */
    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        // TODO: 在生产环境中, 密码必须加密 (例如使用 BCrypt)
        user.setRole("USER");
        
        // 自动生成随机头像 (使用 DiceBear API)
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getUsername());
        }
        // 自动生成 Handle (例如 @john)
        if (user.getHandle() == null) {
            user.setHandle("@" + user.getUsername().toLowerCase().replace(" ", ""));
        }
        return userRepository.save(user);
    }

    /**
     * 用户登录
     * @return 登录成功的用户实体 (包含 ID, 头像等)
     */
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // TODO: 生产环境应比对加密后的密码
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        throw new RuntimeException("Invalid credentials");
    }
}
