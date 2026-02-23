package com.example.rec.controller;

import com.example.rec.model.Content;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.TagRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagRepository tagRepository;
    private final ContentRepository contentRepository;

    public TagController(TagRepository tagRepository, ContentRepository contentRepository) {
        this.tagRepository = tagRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * 根据标签名获取相关内容
     * @param name 标签名 (不带 #)
     * @return 包含该标签的所有推文列表
     */
    @GetMapping("/{name}")
    public List<Content> getContentByTag(@PathVariable String name) {
        // 直接通过关联属性查询
        return contentRepository.findByTags_Name(name);
    }
}
