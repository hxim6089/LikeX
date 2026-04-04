package com.example.rec.controller;

import com.example.rec.model.User;
import com.example.rec.service.AuthService;
import com.example.rec.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        User saved = authService.register(user);
        String token = jwtUtil.generateToken(saved.getId(), saved.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("user", saved);
        result.put("token", token);
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        User user = authService.login(username, password);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("token", token);
        return result;
    }
}
