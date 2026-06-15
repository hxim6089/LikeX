package com.example.rec.repository;

import com.example.rec.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    // 用户搜索：按用户名或handle模糊匹配
    List<User> findByUsernameContainingIgnoreCaseOrHandleContainingIgnoreCase(String username, String handle);
}
