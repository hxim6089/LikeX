package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TF-IDF 内容相似度推荐服务
 * 
 * 通过计算帖子内容的 TF-IDF 向量，发现语义相关的帖子。
 * 用户画像向量 = 用户点赞帖子的 TF-IDF 向量加权平均。
 * 推荐时计算帖子向量与用户画像向量的余弦相似度。
 */
@Service
public class TfIdfService {

    private final ContentRepository contentRepository;
    private final BehaviorRepository behaviorRepository;

    // 分词正则：匹配中文字符、英文单词、数字
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "[\\u4e00-\\u9fa5]+|[a-zA-Z]+|\\d+"
    );

    // 停用词集合（常见无意义词）
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been",
            "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "can", "shall",
            "i", "you", "he", "she", "it", "we", "they", "me", "him",
            "her", "us", "them", "my", "your", "his", "its", "our",
            "their", "this", "that", "these", "those",
            "in", "on", "at", "to", "for", "with", "by", "from",
            "of", "and", "or", "but", "not", "no", "so", "if",
            "的", "了", "在", "是", "我", "有", "和", "就",
            "不", "人", "都", "一", "一个", "上", "也", "很",
            "到", "说", "要", "去", "你", "会", "着", "没有",
            "看", "好", "自己", "这"
    );

    public TfIdfService(ContentRepository contentRepository,
                        BehaviorRepository behaviorRepository) {
        this.contentRepository = contentRepository;
        this.behaviorRepository = behaviorRepository;
    }

    /**
     * 对文本进行分词
     * 支持中英文混合分词
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            String token = matcher.group();
            // 过滤停用词和单字符
            if (token.length() > 1 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * 计算单文档的词频 (TF)
     * TF(t) = 词t在文档中出现的次数 / 文档总词数
     */
    public Map<String, Double> computeTf(List<String> tokens) {
        if (tokens.isEmpty()) return Collections.emptyMap();
        
        Map<String, Long> wordCount = tokens.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        
        double totalWords = tokens.size();
        Map<String, Double> tf = new HashMap<>();
        wordCount.forEach((word, count) -> tf.put(word, count / totalWords));
        return tf;
    }

    /**
     * 计算逆文档频率 (IDF)
     * IDF(t) = log(总文档数 / 包含词t的文档数 + 1)
     */
    public Map<String, Double> computeIdf(List<List<String>> allDocTokens) {
        int totalDocs = allDocTokens.size();
        Map<String, Integer> docFrequency = new HashMap<>();

        for (List<String> tokens : allDocTokens) {
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                docFrequency.merge(token, 1, Integer::sum);
            }
        }

        Map<String, Double> idf = new HashMap<>();
        docFrequency.forEach((word, df) -> 
            idf.put(word, Math.log((double) totalDocs / (df + 1)) + 1.0)
        );
        return idf;
    }

    /**
     * 计算 TF-IDF 向量
     */
    public Map<String, Double> computeTfIdf(List<String> tokens, Map<String, Double> idf) {
        Map<String, Double> tf = computeTf(tokens);
        Map<String, Double> tfidf = new HashMap<>();
        
        tf.forEach((word, tfVal) -> {
            double idfVal = idf.getOrDefault(word, 1.0);
            tfidf.put(word, tfVal * idfVal);
        });
        
        return tfidf;
    }

    /**
     * 计算用户画像向量
     * = 用户点赞帖子的 TF-IDF 向量加权平均
     * 
     * @param userId 用户ID
     * @return 用户画像 TF-IDF 向量
     */
    public Map<String, Double> getUserProfileVector(Long userId) {
        if (userId == null) return Collections.emptyMap();

        // 1. 获取用户点赞的内容
        List<Behavior> likes = behaviorRepository.findByUserIdAndType(userId, "LIKE");
        if (likes.isEmpty()) return Collections.emptyMap();

        Set<Long> likedContentIds = likes.stream()
                .map(Behavior::getContentId)
                .collect(Collectors.toSet());
        
        List<Content> likedContents = contentRepository.findAllById(likedContentIds);
        if (likedContents.isEmpty()) return Collections.emptyMap();

        // 2. 获取全部帖子，构建全局 IDF
        List<Content> allContents = contentRepository.findAll();
        List<List<String>> allDocTokens = allContents.stream()
                .map(c -> tokenize(c.getContent()))
                .collect(Collectors.toList());
        Map<String, Double> idf = computeIdf(allDocTokens);

        // 3. 对点赞帖子的 TF-IDF 取平均
        Map<String, Double> profile = new HashMap<>();
        int count = 0;
        for (Content content : likedContents) {
            List<String> tokens = tokenize(content.getContent());
            if (tokens.isEmpty()) continue;
            
            Map<String, Double> tfidf = computeTfIdf(tokens, idf);
            tfidf.forEach((word, val) -> profile.merge(word, val, Double::sum));
            count++;
        }

        // 取平均
        if (count > 0) {
            int finalCount = count;
            profile.replaceAll((k, v) -> v / finalCount);
        }

        return profile;
    }

    /**
     * 计算帖子与用户画像的 TF-IDF 余弦相似度
     * 
     * @param content 待评分帖子
     * @param userProfile 用户画像向量
     * @param idf 全局 IDF 字典
     * @return 相似度得分 [0, 1]
     */
    public double getContentSimilarityScore(Content content, Map<String, Double> userProfile, Map<String, Double> idf) {
        if (userProfile == null || userProfile.isEmpty()) return 0.0;
        
        List<String> tokens = tokenize(content.getContent());
        if (tokens.isEmpty()) return 0.0;
        
        Map<String, Double> contentVector = computeTfIdf(tokens, idf);
        return cosineSimilarity(contentVector, userProfile);
    }

    /**
     * 余弦相似度计算
     * similarity = (A · B) / (|A| × |B|)
     */
    public double cosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        if (vecA.isEmpty() || vecB.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        // 计算点积和 vecA 的模
        for (Map.Entry<String, Double> entry : vecA.entrySet()) {
            double valA = entry.getValue();
            normA += valA * valA;
            Double valB = vecB.get(entry.getKey());
            if (valB != null) {
                dotProduct += valA * valB;
            }
        }

        // 计算 vecB 的模
        for (double valB : vecB.values()) {
            normB += valB * valB;
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 构建全局 IDF 字典（可缓存）
     */
    public Map<String, Double> buildGlobalIdf() {
        List<Content> allContents = contentRepository.findAll();
        List<List<String>> allDocTokens = allContents.stream()
                .map(c -> tokenize(c.getContent()))
                .collect(Collectors.toList());
        return computeIdf(allDocTokens);
    }
}
