package com.example.rec.service;

import com.example.rec.model.Ad;
import com.example.rec.model.AdConfig;
import com.example.rec.repository.AdRepository;
import com.example.rec.repository.AdConfigRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AdService {

    private final AdRepository adRepository;
    private final AdConfigRepository adConfigRepository;
    private final PersonaService personaService;

    /**
     * 简易频控：userId -> { adId -> impressionCount }
     * 生产环境应使用 Redis，这里仅做演示
     */
    private final ConcurrentHashMap<Long, Map<Long, Integer>> freqCapMap = new ConcurrentHashMap<>();
    private static final int MAX_IMPRESSIONS_PER_USER = 3;

    public AdService(AdRepository adRepository, AdConfigRepository adConfigRepository, PersonaService personaService) {
        this.adRepository = adRepository;
        this.adConfigRepository = adConfigRepository;
        this.personaService = personaService;
    }

    /**
     * 获取广告投放配置（单行，不存在则创建默认）
     */
    public AdConfig getAdConfig() {
        return adConfigRepository.findAll().stream().findFirst()
                .orElseGet(() -> adConfigRepository.save(new AdConfig()));
    }

    /**
     * 更新广告投放配置
     */
    public AdConfig updateAdConfig(AdConfig incoming) {
        AdConfig config = getAdConfig();
        if (incoming.getAdInterval() != null) config.setAdInterval(incoming.getAdInterval());
        if (incoming.getMaxAdsPerPage() != null) config.setMaxAdsPerPage(incoming.getMaxAdsPerPage());
        if (incoming.getGlobalEnabled() != null) config.setGlobalEnabled(incoming.getGlobalEnabled());
        return adConfigRepository.save(config);
    }

    public List<Ad> getAllAds() {
        return adRepository.findAll();
    }

    public Ad createAd(Ad ad) {
        return adRepository.save(ad);
    }

    public Ad updateAd(Long id, Ad updated) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ad not found: " + id));
        if (updated.getTitle() != null) ad.setTitle(updated.getTitle());
        if (updated.getDescription() != null) ad.setDescription(updated.getDescription());
        if (updated.getAdvertiser() != null) ad.setAdvertiser(updated.getAdvertiser());
        if (updated.getTargetTags() != null) ad.setTargetTags(updated.getTargetTags());
        if (updated.getCategory() != null) ad.setCategory(updated.getCategory());
        if (updated.getBidPrice() != null) ad.setBidPrice(updated.getBidPrice());
        if (updated.getTargetUrl() != null) ad.setTargetUrl(updated.getTargetUrl());
        if (updated.getImageUrl() != null) ad.setImageUrl(updated.getImageUrl());
        if (updated.getActive() != null) ad.setActive(updated.getActive());
        return adRepository.save(ad);
    }

    /**
     * 根据用户画像匹配最相关的广告
     * 
     * adScore = tagMatchCount × 30 + bidPrice × 10 + qualityScore
     * qualityScore = CTR × 100
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRelevantAds(Long userId, int count) {
        // 1. 获取用户兴趣标签
        Map<String, Object> persona = personaService.getUserPersona(userId);
        List<String> userTags = (List<String>) persona.getOrDefault("interestTags", Collections.emptyList());

        // 2. 获取活跃广告
        List<Ad> activeAds = adRepository.findByActiveTrue();
        if (activeAds.isEmpty()) return Collections.emptyList();

        // 3. 计算得分并排序
        Map<Long, Integer> userFreq = freqCapMap.getOrDefault(userId, Collections.emptyMap());

        List<Map<String, Object>> scoredAds = new ArrayList<>();
        for (Ad ad : activeAds) {
            // 频控检查
            int shown = userFreq.getOrDefault(ad.getId(), 0);
            if (shown >= MAX_IMPRESSIONS_PER_USER) continue;

            // 标签匹配
            List<String> matchedTags = new ArrayList<>();
            if (ad.getTargetTags() != null && !userTags.isEmpty()) {
                String[] adTags = ad.getTargetTags().split(",");
                for (String adTag : adTags) {
                    String trimmed = adTag.trim();
                    if (userTags.contains(trimmed)) {
                        matchedTags.add(trimmed);
                    }
                }
            }

            // 质量分（历史 CTR）
            double ctr = ad.getImpressionCount() > 0 ?
                    (double) ad.getClickCount() / ad.getImpressionCount() : 0.05; // 新广告默认 5%
            double qualityScore = ctr * 100;

            // 综合得分
            double adScore = matchedTags.size() * 30 
                           + (ad.getBidPrice() != null ? ad.getBidPrice() : 1.0) * 10 
                           + qualityScore;

            Map<String, Object> item = new HashMap<>();
            item.put("ad", ad);
            item.put("score", Math.round(adScore * 100.0) / 100.0);
            item.put("matchedTags", matchedTags);
            item.put("ctr", Math.round(ctr * 10000.0) / 100.0); // 百分比保留2位
            scoredAds.add(item);
        }

        // 按得分降序
        scoredAds.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return scoredAds.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * 记录广告展示
     */
    public void recordImpression(Long adId, Long userId) {
        adRepository.findById(adId).ifPresent(ad -> {
            ad.setImpressionCount(ad.getImpressionCount() + 1);
            adRepository.save(ad);
        });

        // 更新频控计数
        if (userId != null) {
            freqCapMap.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                      .merge(adId, 1, Integer::sum);
        }
    }

    /**
     * 记录广告点击
     */
    public void recordClick(Long adId) {
        adRepository.findById(adId).ifPresent(ad -> {
            ad.setClickCount(ad.getClickCount() + 1);
            adRepository.save(ad);
        });
    }

    /**
     * 广告统计报表
     */
    public Map<String, Object> getAdStats() {
        List<Ad> allAds = adRepository.findAll();
        Map<String, Object> stats = new HashMap<>();

        long totalImpressions = allAds.stream().mapToLong(Ad::getImpressionCount).sum();
        long totalClicks = allAds.stream().mapToLong(Ad::getClickCount).sum();
        double overallCtr = totalImpressions > 0 ? (double) totalClicks / totalImpressions * 100 : 0;
        double estimatedRevenue = allAds.stream()
                .mapToDouble(ad -> ad.getImpressionCount() * (ad.getBidPrice() != null ? ad.getBidPrice() : 0) / 1000.0)
                .sum();

        stats.put("totalImpressions", totalImpressions);
        stats.put("totalClicks", totalClicks);
        stats.put("overallCtr", Math.round(overallCtr * 100.0) / 100.0);
        stats.put("estimatedRevenue", Math.round(estimatedRevenue * 100.0) / 100.0);

        // 每条广告的详细数据
        List<Map<String, Object>> adDetails = new ArrayList<>();
        for (Ad ad : allAds) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", ad.getId());
            detail.put("title", ad.getTitle());
            detail.put("description", ad.getDescription());
            detail.put("advertiser", ad.getAdvertiser());
            detail.put("targetTags", ad.getTargetTags());
            detail.put("category", ad.getCategory());
            detail.put("bidPrice", ad.getBidPrice());
            detail.put("targetUrl", ad.getTargetUrl());
            detail.put("imageUrl", ad.getImageUrl());
            detail.put("active", ad.getActive());
            detail.put("impressions", ad.getImpressionCount());
            detail.put("clicks", ad.getClickCount());
            double ctr = ad.getImpressionCount() > 0 ?
                    (double) ad.getClickCount() / ad.getImpressionCount() * 100 : 0;
            detail.put("ctr", Math.round(ctr * 100.0) / 100.0);
            double ecpm = ad.getImpressionCount() > 0 ?
                    (double) ad.getClickCount() / ad.getImpressionCount() * (ad.getBidPrice() != null ? ad.getBidPrice() : 0) * 1000 : 0;
            detail.put("ecpm", Math.round(ecpm * 100.0) / 100.0);
            adDetails.add(detail);
        }
        stats.put("ads", adDetails);

        // 按类别分组的平均 CTR
        Map<String, List<Ad>> byCategory = allAds.stream()
                .filter(ad -> ad.getCategory() != null)
                .collect(Collectors.groupingBy(Ad::getCategory));
        Map<String, Double> categoryCtr = new HashMap<>();
        for (Map.Entry<String, List<Ad>> entry : byCategory.entrySet()) {
            long catImpressions = entry.getValue().stream().mapToLong(Ad::getImpressionCount).sum();
            long catClicks = entry.getValue().stream().mapToLong(Ad::getClickCount).sum();
            categoryCtr.put(entry.getKey(), catImpressions > 0 ? 
                    Math.round((double) catClicks / catImpressions * 10000.0) / 100.0 : 0.0);
        }
        stats.put("categoryCtr", categoryCtr);

        // 各类别展示量占比
        Map<String, Long> categoryImpressions = new HashMap<>();
        for (Map.Entry<String, List<Ad>> entry : byCategory.entrySet()) {
            categoryImpressions.put(entry.getKey(), 
                    entry.getValue().stream().mapToLong(Ad::getImpressionCount).sum());
        }
        stats.put("categoryImpressions", categoryImpressions);

        return stats;
    }
}
