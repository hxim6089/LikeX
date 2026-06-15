package com.example.rec.controller;

import com.example.rec.model.Message;
import com.example.rec.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public Message sendMessage(@RequestBody Map<String, Object> payload) {
        Long senderId = Long.valueOf(payload.get("senderId").toString());
        Long recipientId = Long.valueOf(payload.get("recipientId").toString());
        String content = (String) payload.get("content");
        return messageService.sendMessage(senderId, recipientId, content);
    }

    @GetMapping("/conversations")
    public List<Map<String, Object>> getConversations(@RequestParam Long userId) {
        return messageService.getConversations(userId);
    }

    @GetMapping("/history")
    public List<Message> getHistory(@RequestParam Long userId, @RequestParam Long targetId) {
        return messageService.getChatHistory(userId, targetId);
    }
}
