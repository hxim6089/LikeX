package com.example.rec.controller;

import com.example.rec.model.Content;
import com.example.rec.repository.ContentRepository;
import com.example.rec.service.AiService;
import com.example.rec.service.AiTaggingService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AiTaggingService aiTaggingService;
    private final ContentRepository contentRepository;

    public AiController(AiService aiService, 
                        AiTaggingService aiTaggingService,
                        ContentRepository contentRepository) {
        this.aiService = aiService;
        this.aiTaggingService = aiTaggingService;
        this.contentRepository = contentRepository;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        return aiService.chat(message);
    }

    /**
     * AI 自动打标签 - 单条帖文
     */
    @PostMapping("/tag/{contentId}")
    public Map<String, Object> tagSingle(@PathVariable Long contentId) {
        Map<String, Object> result = new HashMap<>();
        Optional<Content> opt = contentRepository.findById(contentId);
        if (opt.isEmpty()) {
            result.put("error", "Content not found: " + contentId);
            return result;
        }

        List<String> tags = aiTaggingService.tagContent(opt.get());
        result.put("contentId", contentId);
        result.put("tags", tags);
        result.put("success", !tags.isEmpty());
        return result;
    }

    /**
     * AI 批量补标 - 所有无标签帖文
     */
    @PostMapping("/tag-all")
    public Map<String, Object> tagAll() {
        return aiTaggingService.batchTagAll();
    }
}
