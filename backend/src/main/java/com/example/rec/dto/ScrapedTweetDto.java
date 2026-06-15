package com.example.rec.dto;

public class ScrapedTweetDto {
    private String handle;
    private String content;
    private String imageUrl;
    private String timestamp;
    private String source;

    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
