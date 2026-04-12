package com.example.rec.controller;

import com.example.rec.model.User;
import com.example.rec.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    /**
     * 更新用户资料
     * @param updates JSON对象，包含 bio, handle, avatarUrl 等字段
     */
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return userService.updateUser(id, updates);
    }

    @GetMapping("/all")
    public java.util.List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * 切换用户角色
     */
    @PutMapping("/{id}/role")
    public User updateRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return userService.updateRole(id, payload.get("role"));
    }

    /**
     * 切换用户封禁状态
     */
    @PutMapping("/{id}/ban")
    public User toggleBan(@PathVariable Long id) {
        return userService.toggleBan(id);
    }
}
