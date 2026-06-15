package com.example.rec.service;

import com.example.rec.dto.ScrapedTweetDto;
import com.example.rec.model.Content;
import com.example.rec.model.User;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ImportService {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final com.example.rec.repository.TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ImportService(UserRepository userRepository, ContentRepository contentRepository, RestTemplate restTemplate, com.example.rec.repository.TagRepository tagRepository) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.restTemplate = restTemplate;
        this.tagRepository = tagRepository;
        this.objectMapper = new ObjectMapper();
    }

    public String importScrapedData() {
        try {
            // Read JSON from resources (in production this path might vary, but for MVP it works)
            File file = new File("src/main/resources/scraped_data.json");
            if (!file.exists()) {
                return "scraped_data.json not found. Run the python scraper first!";
            }

            List<ScrapedTweetDto> tweets = objectMapper.readValue(file, new TypeReference<List<ScrapedTweetDto>>() {});
            int count = 0;

            for (ScrapedTweetDto tweet : tweets) {
                // 1. Find or Create User
                User author = userRepository.findByUsername(tweet.getHandle()).orElse(null);
                if (author == null) {
                    author = new User();
                    author.setUsername(tweet.getHandle());
                    author.setHandle("@" + tweet.getHandle());
                    author.setPassword("imported123"); // Default password
                    author.setBio("Imported from X");
                    // Assign a random avatar or default
                    author.setAvatarUrl("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
                    userRepository.save(author);
                }

                // 2. Download Image (if exists) -> Local Update
                String localImageUrl = null;
                if (StringUtils.hasText(tweet.getImageUrl())) {
                    try {
                        String ext = tweet.getImageUrl().contains(".png") ? ".png" : ".jpg";
                        String filename = UUID.randomUUID().toString() + ext;
                        
                        // Download
                        URL url = new URL(tweet.getImageUrl());
                        try (InputStream in = url.openStream();
                             FileOutputStream out = new FileOutputStream("uploads/" + filename)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                        localImageUrl = "http://localhost:8080/images/" + filename;
                    } catch (Exception e) {
                        System.err.println("Failed to download image: " + e.getMessage());
                    }
                }

                // 3. Save Content
                Content content = new Content();
                content.setAuthor(author);
                content.setTitle("Tweet from " + tweet.getHandle()); // Required field
                content.setContent(tweet.getContent());
                content.setImageUrl(localImageUrl);
                content.setCreatedAt(LocalDateTime.now()); // Simplify time for now
                content.setLikeCount(0);
                content.setViewCount(0);
                content.setCommentCount(0);
                content.setCommentCount(0);
                content.setTags(new java.util.HashSet<>()); 
                
                // Parse Hashtags
                if (content.getContent() != null) {
                   java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#([a-zA-Z0-9_\\u4e00-\\u9fa5]+)");
                   java.util.regex.Matcher matcher = pattern.matcher(content.getContent());
                   while (matcher.find()) {
                       String tagName = matcher.group(1);
                       com.example.rec.model.Tag tag = tagRepository.findByName(tagName)
                               .orElseGet(() -> tagRepository.save(new com.example.rec.model.Tag(tagName)));
                       content.getTags().add(tag);
                   }
                }
                
                contentRepository.save(content);
                count++;
            }
            
            return "Successfully imported " + count + " tweets.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Import failed: " + e.getMessage();
        }
    }
}
