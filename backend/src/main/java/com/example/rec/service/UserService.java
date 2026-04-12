package com.example.rec.service;

import com.example.rec.model.User;
import com.example.rec.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 更新用户信息
     * 支持更新: bio (简介), avatarUrl (头像), handle (用户标识)
     * @param id 用户ID
     * @param updates包含需要更新的字段和值
     */
    public User updateUser(Long id, Map<String, Object> updates) {
        User user = getUser(id);
        
        if (updates.containsKey("bio")) {
            user.setBio((String) updates.get("bio"));
        }
        if (updates.containsKey("avatarUrl")) {
            Object val = updates.get("avatarUrl");
            if (val instanceof String) {
                user.setAvatarUrl((String) val);
            }
        }
        if (updates.containsKey("handle")) {
            // 在实际应用中, 此处应检查 handle 的唯一性
            user.setHandle((String) updates.get("handle"));
        }
        
        return userRepository.save(user);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 切换用户角色
     */
    public User updateRole(Long id, String role) {
        User user = getUser(id);
        user.setRole(role);
        return userRepository.save(user);
    }

    /**
     * 切换用户封禁状态
     */
    public User toggleBan(Long id) {
        User user = getUser(id);
        user.setBanned(!Boolean.TRUE.equals(user.getBanned()));
        return userRepository.save(user);
    }
}
