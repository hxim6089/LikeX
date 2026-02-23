package com.example.rec.service;

import com.example.rec.model.Message;
import com.example.rec.model.User;
import com.example.rec.repository.MessageRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Message sendMessage(Long senderId, Long recipientId, String content) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setRecipientId(recipientId);
        msg.setContent(content);
        Message saved = messageRepository.save(msg);
        
        // 实时推送私信
        User sender = userRepository.findById(senderId).orElse(null);
        messagingTemplate.convertAndSendToUser(
            recipientId.toString(),
            "/queue/messages",
            Map.of(
                "id", saved.getId(),
                "senderId", senderId,
                "senderName", sender != null ? sender.getUsername() : "Unknown",
                "content", content,
                "createdAt", saved.getCreatedAt().toString()
            )
        );
        
        return saved;
    }

    public List<Message> getChatHistory(Long userId1, Long userId2) {
        return messageRepository.findChatHistory(userId1, userId2);
    }

    /**
     * Get a list of "Conversations" for the dashboard.
     * Returns a list of Users (the other person) with the latest message attached if possible.
     * Simplified logic: Fetch all messages -> Group by "Partner" -> Pick latest.
     */
    public List<Map<String, Object>> getConversations(Long userId) {
        List<Message> allMessages = messageRepository.findAllByUserId(userId);

        Map<Long, Message> latestMessageMap = new HashMap<>();

        for (Message m : allMessages) {
            Long partnerId = m.getSenderId().equals(userId) ? m.getRecipientId() : m.getSenderId();
            
            // Because list is sorted DESC, the first one we encounter is the latest
            if (!latestMessageMap.containsKey(partnerId)) {
                latestMessageMap.put(partnerId, m);
            }
        }

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestMessageMap.entrySet()) {
            Long partnerId = entry.getKey();
            Message latest = entry.getValue();

            User partner = userRepository.findById(partnerId).orElse(null);
            if (partner != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("partner", partner);
                item.put("latestMessage", latest.getContent());
                item.put("timestamp", latest.getCreatedAt());
                conversations.add(item);
            }
        }
        
        // Sort by timestamp desc
        conversations.sort((a, b) -> ((java.time.LocalDateTime) b.get("timestamp"))
                .compareTo((java.time.LocalDateTime) a.get("timestamp")));

        return conversations;
    }
}
