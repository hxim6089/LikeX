package com.example.rec.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ad_config")
public class AdConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 每隔多少条帖子插入一条广告 */
    private Integer adInterval = 5;

    /** 单页最大广告数 */
    private Integer maxAdsPerPage = 3;

    /** 全局广告开关 */
    private Boolean globalEnabled = true;
}
