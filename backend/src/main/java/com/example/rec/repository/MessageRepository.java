package com.example.rec.repository;

import com.example.rec.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find chat history between two users
    @Query("SELECT m FROM Message m WHERE (m.senderId = :userId1 AND m.recipientId = :userId2) " +
           "OR (m.senderId = :userId2 AND m.recipientId = :userId1) ORDER BY m.createdAt ASC")
    List<Message> findChatHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // Find latest messages for a user (to build conversation list)
    // This is a simplified query; for production, we might need a more complex group by or distinct logic
    // OR we can fetch all messages involving the user and process in memory for MVP simplicity
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId OR m.recipientId = :userId ORDER BY m.createdAt DESC")
    List<Message> findAllByUserId(@Param("userId") Long userId);
}
