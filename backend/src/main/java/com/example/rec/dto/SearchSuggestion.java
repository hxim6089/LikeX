package com.example.rec.dto;

/**
 * 搜索建议 DTO
 * 用于搜索自动补全
 */
public class SearchSuggestion {
    private String type;       // "user" / "topic" / "keyword"
    private String value;      // 显示文本
    private String subtext;    // 副标题 (用户: @handle, 话题: 帖子数)
    private String icon;       // 图标URL (用户头像)

    public SearchSuggestion() {}

    public SearchSuggestion(String type, String value, String subtext, String icon) {
        this.type = type;
        this.value = value;
        this.subtext = subtext;
        this.icon = icon;
    }

    // 便捷工厂方法
    public static SearchSuggestion forUser(String username, String handle, String avatarUrl) {
        return new SearchSuggestion("user", username, handle, avatarUrl);
    }

    public static SearchSuggestion forTopic(String tagName, Long postCount) {
        return new SearchSuggestion("topic", "#" + tagName, postCount + " 帖子", null);
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getSubtext() { return subtext; }
    public void setSubtext(String subtext) { this.subtext = subtext; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
