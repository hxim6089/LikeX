package com.example.rec.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ads")
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 500)
    private String description;

    private String imageUrl;

    private String targetUrl;

    private String advertiser;

    /** 逗号分隔的定向标签，如 "Tech,AI,Programming" */
    private String targetTags;

    /** 广告类别：Tech / Life / Education / Sports / Finance */
    private String category;

    /** CPM 出价（元/千次展示） */
    private Double bidPrice;

    private Integer impressionCount = 0;

    private Integer clickCount = 0;

    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.impressionCount == null) this.impressionCount = 0;
        if (this.clickCount == null) this.clickCount = 0;
        if (this.active == null) this.active = true;
    }
}
