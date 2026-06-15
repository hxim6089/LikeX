package com.example.rec.service;

import com.example.rec.model.Content;
import com.example.rec.model.Tag;
import com.example.rec.model.User;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.TagRepository;
import com.example.rec.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class KaggleImportService {

    private static final Logger log = LoggerFactory.getLogger(KaggleImportService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String KAGGLE_DOWNLOAD_URL = "https://www.kaggle.com/api/v1/datasets/download/%s";
    private static final String DEFAULT_DATASET = "rmisra/news-category-dataset";
    private static final int CACHE_SIZE = 3000;
    private static final int DEFAULT_BATCH_TARGET = 50;

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final HttpClient httpClient;
    private final Random random = new Random();

    @Value("${kaggle.api.key:}")
    private String kaggleApiKey;

    private List<RawPost> cachedPosts = null;
    private String cachedDatasetSlug = null;

    public KaggleImportService(ContentRepository contentRepository,
                               UserRepository userRepository,
                               TagRepository tagRepository) {
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 一键批量导入：先尝试 Kaggle 数据集，不够则用内置内容库补齐
     */
    public ImportResult batchImport(int targetCount) {
        if (targetCount <= 0) targetCount = DEFAULT_BATCH_TARGET;

        ImportResult result = new ImportResult();
        result.startTime = LocalDateTime.now();

        Set<String> existingTexts = loadExistingTexts();
        List<User> systemUsers = userRepository.findAll();
        if (systemUsers.isEmpty()) {
            result.success = false;
            result.message = "系统中没有用户，请先注册用户";
            result.endTime = LocalDateTime.now();
            return result;
        }

        int imported = 0;
        int kaggleImported = 0;
        int libraryImported = 0;

        // === Phase 1: Kaggle 数据集 ===
        try {
            ensureCache(DEFAULT_DATASET);
            List<RawPost> available = new ArrayList<>();
            for (RawPost p : cachedPosts) {
                if (!existingTexts.contains(p.text.trim())) available.add(p);
            }
            Collections.shuffle(available, random);

            for (RawPost p : available) {
                if (imported >= targetCount) break;
                Content content = savePost(p.text, p.category, systemUsers, existingTexts,
                        randomEngagement(), randomEngagement() / 3);
                autoTag(content, p.text);
                imported++;
                kaggleImported++;
            }
        } catch (Exception e) {
            log.warn("Kaggle import failed: {}", e.getMessage());
        }

        // === Phase 2: 内置内容库补齐 ===
        if (imported < targetCount) {
            log.info("Kaggle imported {}/{}, supplementing from built-in library", imported, targetCount);
            List<String[]> pool = buildContentPool();
            Collections.shuffle(pool, random);

            for (String[] item : pool) {
                if (imported >= targetCount) break;
                String text = item[0];
                String category = item[1];
                if (existingTexts.contains(text.trim())) { result.skippedDuplicate++; continue; }

                Content content = savePost(text, category, systemUsers, existingTexts,
                        randomEngagement(), randomEngagement() / 3);
                autoTag(content, text);
                imported++;
                libraryImported++;
            }
        }

        result.importedCount = imported;
        result.endTime = LocalDateTime.now();

        if (imported > 0) {
            result.success = true;
            StringBuilder msg = new StringBuilder();
            msg.append(String.format("成功导入 %d 条帖文", imported));
            if (kaggleImported > 0) msg.append(String.format(" (Kaggle: %d", kaggleImported));
            if (libraryImported > 0) {
                msg.append(kaggleImported > 0
                        ? String.format(", 内容库: %d)", libraryImported)
                        : String.format(" (内容库: %d)", libraryImported));
            } else if (kaggleImported > 0) {
                msg.append(")");
            }
            if (result.skippedDuplicate > 0) msg.append(String.format(", 跳过 %d 条重复", result.skippedDuplicate));
            result.message = msg.toString();
        } else {
            result.success = false;
            result.message = "未能导入任何帖文，内容可能已全部导入过";
        }

        return result;
    }

    /**
     * 从指定 Kaggle 数据集导入
     */
    public ImportResult importFromDataset(String datasetSlug, int targetCount) {
        if (targetCount <= 0) targetCount = DEFAULT_BATCH_TARGET;

        ImportResult result = new ImportResult();
        result.source = datasetSlug;
        result.startTime = LocalDateTime.now();

        try {
            Set<String> existingTexts = loadExistingTexts();
            List<User> systemUsers = userRepository.findAll();
            if (systemUsers.isEmpty()) {
                result.success = false;
                result.message = "系统中没有用户";
                result.endTime = LocalDateTime.now();
                return result;
            }

            ensureCache(datasetSlug);

            List<RawPost> available = new ArrayList<>();
            for (RawPost p : cachedPosts) {
                if (!existingTexts.contains(p.text.trim())) available.add(p);
                else result.skippedDuplicate++;
            }
            Collections.shuffle(available, random);

            int imported = 0;
            for (RawPost p : available) {
                if (imported >= targetCount) break;
                Content content = savePost(p.text, p.category, systemUsers, existingTexts,
                        randomEngagement(), randomEngagement() / 3);
                autoTag(content, p.text);
                imported++;
            }

            result.success = true;
            result.importedCount = imported;
            result.message = String.format("从 %s 导入 %d 条帖文", datasetSlug, imported);
        } catch (Exception e) {
            log.error("Kaggle import failed for {}", datasetSlug, e);
            result.success = false;
            result.message = "导入失败: " + e.getMessage();
        }

        result.endTime = LocalDateTime.now();
        return result;
    }

    // ========== Kaggle API ==========

    private void ensureCache(String datasetSlug) throws Exception {
        if (cachedPosts != null && !cachedPosts.isEmpty() && datasetSlug.equals(cachedDatasetSlug)) {
            return;
        }
        if (kaggleApiKey == null || kaggleApiKey.isBlank()) {
            throw new IllegalStateException("KAGGLE_API_KEY is not configured");
        }
        log.info("Downloading Kaggle dataset: {}", datasetSlug);
        cachedPosts = downloadAndParse(datasetSlug);
        cachedDatasetSlug = datasetSlug;
        log.info("Kaggle dataset cached: {} posts from {}", cachedPosts.size(), datasetSlug);
    }

    private List<RawPost> downloadAndParse(String datasetSlug) throws Exception {
        String url = String.format(KAGGLE_DOWNLOAD_URL, datasetSlug);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + kaggleApiKey)
                .GET()
                .timeout(Duration.ofSeconds(120))
                .build();

        Path tempFile = Files.createTempFile("kaggle_", ".zip");
        try {
            HttpResponse<Path> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

            if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                throw new RuntimeException("Kaggle 认证失败 (HTTP " + resp.statusCode() + ")，请检查 API Key");
            }
            if (resp.statusCode() != 200) {
                throw new RuntimeException("Kaggle API HTTP " + resp.statusCode());
            }

            List<RawPost> posts = new ArrayList<>();

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempFile.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName().toLowerCase();
                    if (name.endsWith(".json") || name.endsWith(".jsonl")) {
                        parseJsonLines(zis, posts);
                    } else if (name.endsWith(".csv")) {
                        parseCsv(zis, posts);
                    }
                    if (posts.size() >= CACHE_SIZE) break;
                }
            }

            Collections.shuffle(posts, random);
            if (posts.size() > CACHE_SIZE) {
                return new ArrayList<>(posts.subList(0, CACHE_SIZE));
            }
            return posts;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void parseJsonLines(InputStream is, List<RawPost> posts) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null && posts.size() < CACHE_SIZE * 2) {
            try {
                JsonNode node = mapper.readTree(line);

                String headline = node.path("headline").asText("");
                String desc = node.path("short_description").asText("");
                String category = node.path("category").asText("");

                String text;
                if (desc.length() > 30) {
                    text = desc;
                } else if (!headline.isEmpty()) {
                    text = headline + (desc.isEmpty() ? "" : " — " + desc);
                } else {
                    text = node.path("text").asText(node.path("content").asText(""));
                }

                if (text.length() < 10) continue;
                if (text.length() > 280) text = text.substring(0, 277) + "...";

                RawPost post = new RawPost();
                post.text = text;
                post.category = mapCategory(category);
                posts.add(post);
            } catch (Exception ignored) {}
        }
    }

    private void parseCsv(InputStream is, List<RawPost> posts) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) return;

        String[] headers = splitCsvLine(headerLine);
        int textCol = -1, catCol = -1;
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase().replace("\"", "");
            if (textCol == -1 && (h.equals("text") || h.equals("content") || h.equals("tweet")
                    || h.equals("headline") || h.equals("title") || h.equals("short_description"))) {
                textCol = i;
            }
            if (catCol == -1 && (h.equals("category") || h.equals("label") || h.equals("topic"))) {
                catCol = i;
            }
        }
        if (textCol == -1) return;

        String line;
        while ((line = reader.readLine()) != null && posts.size() < CACHE_SIZE * 2) {
            try {
                String[] cols = splitCsvLine(line);
                if (cols.length <= textCol) continue;

                String text = cols[textCol].trim();
                if (text.startsWith("\"") && text.endsWith("\"")) text = text.substring(1, text.length() - 1);
                if (text.length() < 10) continue;
                if (text.length() > 280) text = text.substring(0, 277) + "...";

                String category = "News";
                if (catCol >= 0 && cols.length > catCol) {
                    category = mapCategory(cols[catCol].trim().replace("\"", ""));
                }

                RawPost post = new RawPost();
                post.text = text;
                post.category = category;
                posts.add(post);
            } catch (Exception ignored) {}
        }
    }

    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private String mapCategory(String kaggleCategory) {
        if (kaggleCategory == null || kaggleCategory.isEmpty()) return "News";
        String upper = kaggleCategory.toUpperCase().trim();

        if (upper.contains("TECH") || upper.contains("SCIENCE") || upper.contains("TECHNOLOGY")) return "Tech";
        if (upper.contains("SPORT")) return "Sports";
        if (upper.contains("EDUCATION") || upper.contains("COLLEGE")) return "Education";
        if (upper.contains("TRAVEL") || upper.contains("STYLE") || upper.contains("BEAUTY")
                || upper.contains("FOOD") || upper.contains("DRINK") || upper.contains("WELLNESS")
                || upper.contains("ENTERTAINMENT") || upper.contains("COMEDY") || upper.contains("ARTS")
                || upper.contains("HOME") || upper.contains("LIVING") || upper.contains("PARENTING")
                || upper.contains("TASTE") || upper.contains("WEDDING")) return "Life";

        return "News";
    }

    // ========== 辅助 ==========

    private Content savePost(String text, String category, List<User> users,
                             Set<String> existingTexts, int likes, int reposts) {
        User author = users.get(random.nextInt(users.size()));
        int hoursAgo = random.nextInt(30 * 24);

        Content content = new Content();
        content.setAuthor(author);
        content.setTitle(text.length() > 20 ? text.substring(0, 20) + "..." : text);
        content.setContent(text);
        content.setCategory(category);
        content.setCreatedAt(LocalDateTime.now().minusHours(hoursAgo));
        content.setViewCount(likes * (3 + random.nextInt(8)));
        content.setLikeCount(likes);
        content.setCommentCount(Math.max(0, likes / (3 + random.nextInt(5))));
        content.setRepostCount(reposts);
        content.setDislikeCount(0);
        contentRepository.save(content);
        existingTexts.add(text.trim());
        return content;
    }

    private void autoTag(Content content, String text) {
        String lower = text.toLowerCase();
        Map<String, List<String>> tagRules = getTagRules();
        Set<Tag> matched = new HashSet<>();

        for (Map.Entry<String, List<String>> entry : tagRules.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.contains(kw.toLowerCase())) {
                    tagRepository.findByName(entry.getKey()).ifPresent(matched::add);
                    break;
                }
            }
        }

        if (!matched.isEmpty()) {
            content.setTags(matched);
            contentRepository.save(content);
        }
    }

    private int randomEngagement() {
        int tier = random.nextInt(100);
        if (tier < 30) return random.nextInt(15) + 1;
        if (tier < 70) return random.nextInt(135) + 15;
        if (tier < 90) return random.nextInt(1350) + 150;
        return random.nextInt(13500) + 1500;
    }

    private Set<String> loadExistingTexts() {
        Set<String> texts = new HashSet<>();
        contentRepository.findAll().forEach(c -> {
            if (c.getContent() != null) texts.add(c.getContent().trim());
        });
        return texts;
    }

    // ========== 内置内容库 (来自 x_scraper.py) ==========

    private List<String[]> buildContentPool() {
        List<String[]> pool = new ArrayList<>();
        addTech(pool); addLife(pool); addSports(pool); addNews(pool); addEducation(pool);
        return pool;
    }

    private void addTech(List<String[]> p) {
        String c = "Tech";
        p.add(new String[]{"刚试了 GPT-4o 的新功能，代码补全能力太强了！写了个自动化脚本，原来要2小时的工作10分钟搞定 \uD83D\uDD25", c});
        p.add(new String[]{"分享一下我用 Python 做数据分析的工作流：Pandas 清洗 → Matplotlib 可视化 → Sklearn 建模，效率拉满", c});
        p.add(new String[]{"Apple Vision Pro 体验报告：设备重量可以接受，空间计算的交互确实颠覆性的，但内容生态还需要时间", c});
        p.add(new String[]{"最近在学 Rust，所有权系统一开始确实劝退，但习惯之后写出来的代码质量确实高很多", c});
        p.add(new String[]{"TikTok 的推荐算法真的厉害，刷了几条健身视频之后首页全是健身内容了，协同过滤+内容理解配合得很好", c});
        p.add(new String[]{"Claude 3.5 Sonnet 的代码能力我觉得已经超过 GPT-4 了，特别是对长上下文的理解能力", c});
        p.add(new String[]{"今天部署了一个 Docker 集群，K8s 真的是运维利器，虽然学习曲线陡峭但值得投入", c});
        p.add(new String[]{"M4 芯片的 MacBook Pro 跑大模型的速度快了40%，苹果的统一内存架构确实适合 AI 推理", c});
        p.add(new String[]{"React 19 的 Server Components 终于稳定了，Next.js 的开发体验又上了一个台阶", c});
        p.add(new String[]{"分享一个 VS Code 插件：Codeium，免费的 AI 代码补全，对学生党很友好", c});
        p.add(new String[]{"微软 Copilot+ PC 发布了，NPU 芯片专门跑本地 AI，Windows 终于认真做 AI 了", c});
        p.add(new String[]{"今天面试字节跳动，问了推荐系统设计题，幸好之前学过协同过滤和内容推荐的原理", c});
        p.add(new String[]{"Cursor 编辑器体验：Tab 补全太智能了，连注释都帮你写好，编程效率直接翻倍", c});
        p.add(new String[]{"Linux 内核6.8发布，支持原生 Rust 驱动开发，内核安全性又上一个台阶", c});
        p.add(new String[]{"用 Stable Diffusion XL 生成了一组产品设计图，AI 绘画在工业设计领域真的有潜力", c});
        p.add(new String[]{"OpenAI Sora 生成的视频效果震撼，但离实际商用还有距离，主要是可控性不够", c});
        p.add(new String[]{"树莓派5+装个 HomeAssistant，全屋智能花了不到500块，性价比拉满", c});
        p.add(new String[]{"最近在研究 LangChain + RAG 做知识库问答，检索增强生成是当前最实用的大模型应用方向", c});
        p.add(new String[]{"华为鸿蒙 NEXT 纯血版体验：流畅度确实提升了，但第三方应用适配还需要时间", c});
        p.add(new String[]{"GitHub Copilot 的 Workspace 功能太强了，直接在聊天框里描述需求就能生成完整的 PR", c});
        p.add(new String[]{"阿里云发布了通义千问2.5，中文理解能力确实比之前强了很多，国产大模型进步很快", c});
        p.add(new String[]{"今天学了 WebAssembly，在浏览器里跑 C++ 代码，性能接近原生，前端性能天花板又被打破了", c});
        p.add(new String[]{"Redis 8.0 发布，内置向量搜索功能，RAG 应用不用再装单独的向量数据库了", c});
        p.add(new String[]{"分享一个开源项目：Ollama，本地跑大模型超方便，M1 Mac 就能跑 Llama 3", c});
        p.add(new String[]{"Chrome 推出内置 AI 功能，Gemini Nano 直接在浏览器本地运行，隐私保护是个亮点", c});
    }

    private void addLife(List<String[]> p) {
        String c = "Life";
        p.add(new String[]{"周末去了南京的先锋书店，文艺气息满满 \uD83D\uDCDA 买了三本村上春树", c});
        p.add(new String[]{"今天自制了提拉米苏，马斯卡彭奶酪+浓缩咖啡+手指饼干，成品完美 \uD83C\uDF82", c});
        p.add(new String[]{"分享一下我的晨间routine：6:30起床→喝水→冥想10分钟→运动30分钟→丰富早餐", c});
        p.add(new String[]{"去了成都的宽窄巷子，火锅走起 \uD83C\uDF36\uFE0F 麻辣牛油锅底绝了，配上油碟简直不要太好吃", c});
        p.add(new String[]{"入手了戴森吹风机，速干效果确实好，发质也没有之前吹完那么毛躁了", c});
        p.add(new String[]{"今天做了一杯dirty咖啡☕ 浓缩直接倒在冰牛奶上，分层效果太美了", c});
        p.add(new String[]{"收到新养的多肉了，品种是桃蛋和橙梦露，放在阳台上等着它们变美 \uD83C\uDF31", c});
        p.add(new String[]{"打卡上海迪士尼，疯狂动物城园区太赞了！加勒比海盗也值得二刷", c});
        p.add(new String[]{"周末整理了衣柜，断舍离了30件衣服，空间一下子就出来了，心情也好了很多", c});
        p.add(new String[]{"自学了拉花，第一次拉出了心形 ♥\uFE0F 虽然不完美但超有成就感", c});
        p.add(new String[]{"最近开始记手账了，用的是hobo a6，贴纸+胶带+水彩笔，每页都是一幅画", c});
        p.add(new String[]{"租了一间coworking space，比在家效率高太多了，关键是认识了一群有趣的人", c});
        p.add(new String[]{"试了一下轻断食16:8，坚持了一周确实感觉身体轻盈了很多", c});
        p.add(new String[]{"去了杭州西湖，虽然人很多但断桥残雪的意境还是很美的 \uD83C\uDFD4\uFE0F", c});
        p.add(new String[]{"今天做了酸奶燕麦碗，蓝莓+奇亚籽+格兰诺拉，好看又好吃的健康早餐", c});
        p.add(new String[]{"入住了一家民宿，窗外就是稻田和远山，终于可以远离城市喘口气了", c});
        p.add(new String[]{"种了一阳台的薄荷和罗勒\uD83C\uDF3F 做菜的时候随手摘一把，新鲜又有成就感", c});
        p.add(new String[]{"今天学做了戚风蛋糕，第三次终于没有塌腰！松软绵密完全不输外面买的", c});
        p.add(new String[]{"周末逛了万圣节限定市集，买了一堆有趣的手工艺品，南瓜灯超可爱 \uD83C\uDF83", c});
        p.add(new String[]{"分享我的冬日宅家清单：热巧克力+毛毯+Netflix+橘猫，完美周末", c});
    }

    private void addSports(List<String[]> p) {
        String c = "Sports";
        p.add(new String[]{"今天跑了10公里，配速5分半，感觉状态不错\uD83D\uDCAA 目标是年底完成半马", c});
        p.add(new String[]{"NBA 季后赛太精彩了！关键时刻的三分绝杀看得我直接从沙发上跳起来了 \uD83C\uDFC0", c});
        p.add(new String[]{"健身第100天打卡 \uD83C\uDF89 从60kg到75kg，增肌效果很明显，坚持就是胜利", c});
        p.add(new String[]{"踢了一场五人制足球，2:1赢了对面，进了一个凌空抽射爽到起飞 ⚽", c});
        p.add(new String[]{"开始学游泳了，蛙泳25米不换气达成！比想象中累多了但是很开心 \uD83C\uDFCA", c});
        p.add(new String[]{"看了法网决赛，纳达尔在红土场上的统治力依然恐怖，14冠真的是传奇 \uD83C\uDFBE", c});
        p.add(new String[]{"第一次尝试攀岩，5.10a的线路爬了20分钟才到顶，手臂累到发抖但超爽 \uD83E\uDDD7", c});
        p.add(new String[]{"分享我的家庭健身计划：俯卧撑+深蹲+平板支撑+哑铃，每天30分钟就够了", c});
        p.add(new String[]{"滑雪季来了！北大壶上了新缆车，雪质也很好，双板爱好者狂喜 ⛷\uFE0F", c});
        p.add(new String[]{"今天骑了50公里公路，沿着海边骑感觉太棒了 \uD83D\uDEB4 下次挑战100公里", c});
        p.add(new String[]{"世锦赛4×100接力决赛，中国队冲进决赛了！苏炳添带领的这一代太强了 \uD83C\uDDE8\uD83C\uDDF3", c});
        p.add(new String[]{"瑜伽坚持了半年，柔韧性进步很大，现在可以轻松做到前弯触地了 \uD83E\uDDD8", c});
        p.add(new String[]{"打了一场羽毛球，反手高远球终于打到位了，教练说姿势进步很大 \uD83C\uDFF8", c});
        p.add(new String[]{"入手了Apple Watch Ultra 2，GPS精度和运动追踪确实是最强的智能手表", c});
        p.add(new String[]{"跟跑团一起参加了城市越野赛，爬了800米的累计爬升，但风景值得 \uD83C\uDFC3", c});
    }

    private void addNews(List<String[]> p) {
        String c = "News";
        p.add(new String[]{"SpaceX 星舰第四次试飞成功回收助推器！\uD83D\uDE80 人类离火星又近了一步", c});
        p.add(new String[]{"诺贝尔物理学奖颁给了AI领域的Hinton和Hopfield，深度学习获得了最高学术认可", c});
        p.add(new String[]{"全球AI监管峰会召开，28国签署《布莱切利宣言》，AI安全成为国际共识", c});
        p.add(new String[]{"国际油价跌破70美元，新能源汽车销量创历史新高，能源转型加速中", c});
        p.add(new String[]{"火星样本返回任务最新进展：NASA和ESA联合方案确定，预计2033年返回地球", c});
        p.add(new String[]{"聚变能源重大突破：ITER项目达成新里程碑，距离商业化又近一步", c});
        p.add(new String[]{"央行宣布降准0.5个百分点，释放长期资金约1万亿，A股午后拉升 \uD83D\uDCC8", c});
        p.add(new String[]{"嫦娥六号成功从月球背面采样返回，中国成为首个完成这一壮举的国家 \uD83C\uDF19", c});
        p.add(new String[]{"全球半导体产业链重构：台积电北美厂投产，三星平泽P4产能扩张", c});
        p.add(new String[]{"世界卫生组织推荐新一代疟疾疫苗，有望每年挽救数十万儿童生命", c});
        p.add(new String[]{"碳中和倒计时：欧盟碳关税正式实施，中国碳市场覆盖范围进一步扩大", c});
        p.add(new String[]{"国际空间站退役方案确定，SpaceX 获得拆除合同，2030年坠入太平洋", c});
        p.add(new String[]{"全球人口突破81亿，联合国预测2086年达到峰值后开始下降", c});
        p.add(new String[]{"新一代量子计算机突破1000比特，纠错能力首次超越物理极限", c});
        p.add(new String[]{"可控核聚变再传捷报：中国环流三号等离子体维持时间突破400秒 ☀\uFE0F", c});
    }

    private void addEducation(List<String[]> p) {
        String c = "Education";
        p.add(new String[]{"考研倒计时30天！英语二模拟做了5年真题，平均70分，冲刺 \uD83D\uDCAA", c});
        p.add(new String[]{"分享一下我的四六级备考方法：每天背50个单词+听BBC+做一套真题，稳过", c});
        p.add(new String[]{"今天论文开题通过了！导师说选题不错，接下来就是苦逼的实验阶段了 \uD83D\uDCDD", c});
        p.add(new String[]{"推荐一个学习工具：Notion AI，做笔记和知识管理太方便了，学生必备", c});
        p.add(new String[]{"大三决定跨考计算机，从零开始学数据结构，408是真的难但不能放弃", c});
        p.add(new String[]{"雅思首考7分！口语6.5是短板，准备二战冲7.5，有没有口语搭子 \uD83D\uDDE3\uFE0F", c});
        p.add(new String[]{"分享一下我的Pomodoro学习法：25分钟专注+5分钟休息，一天能高效学习6小时", c});
        p.add(new String[]{"MIT OpenCourseWare 的线性代数课程太棒了，Gilbert Strang 讲得深入浅出", c});
        p.add(new String[]{"实验室发了第一篇SCI！影响因子3.8，虽然不是顶刊但从零到一的突破太开心了 \uD83C\uDF89", c});
        p.add(new String[]{"考公路上的一点感悟：行测要多刷题找规律，申论要多读人民日报社论", c});
        p.add(new String[]{"这学期选了一门机器学习课，期末项目做了一个推荐系统demo，老师给了A", c});
        p.add(new String[]{"整理了一份免费学习资源清单：Coursera/edX/Khan Academy/B站，全部免费 \uD83D\uDCDA", c});
        p.add(new String[]{"保研成功！收到了浙大计算机直博的offer，三年的绩点终于有了回报", c});
        p.add(new String[]{"今天旁听了一场关于AIGC的学术讲座，大模型在教育领域的应用前景很广", c});
        p.add(new String[]{"分享我的GRE备考经验：verbal 要狂背单词，quant 对中国学生来说不难", c});
    }

    private Map<String, List<String>> getTagRules() {
        Map<String, List<String>> rules = new HashMap<>();
        rules.put("AI", List.of("AI", "人工智能", "GPT", "ChatGPT", "大模型", "深度学习", "机器学习",
                "artificial intelligence", "machine learning", "deep learning", "neural"));
        rules.put("编程", List.of("Python", "编程", "代码", "开发", "GitHub", "Rust", "React", "前端",
                "programming", "software", "developer", "code", "coding"));
        rules.put("科技", List.of("科技", "芯片", "Apple", "华为", "iPhone", "MacBook",
                "technology", "tech", "digital", "innovation", "startup", "gadget"));
        rules.put("美食", List.of("美食", "咖啡", "蛋糕", "火锅", "早餐",
                "food", "recipe", "cooking", "restaurant", "chef", "meal"));
        rules.put("旅行", List.of("旅行", "打卡", "迪士尼", "西湖",
                "travel", "trip", "vacation", "tourism", "destination"));
        rules.put("健身", List.of("健身", "跑步", "运动", "瑜伽", "游泳",
                "fitness", "workout", "gym", "exercise"));
        rules.put("篮球", List.of("NBA", "篮球", "basketball"));
        rules.put("足球", List.of("足球", "世界杯", "soccer", "football", "FIFA"));
        rules.put("考研", List.of("考研", "考公", "雅思", "GRE"));
        rules.put("学习", List.of("学习", "读书", "课程", "论文",
                "education", "study", "university", "college", "school", "student"));
        rules.put("航天", List.of("SpaceX", "NASA", "火星", "嫦娥",
                "space", "rocket", "Mars", "astronaut"));
        rules.put("经济", List.of("央行", "A股", "油价",
                "economy", "market", "stock", "finance", "inflation"));
        rules.put("生活", List.of("生活", "日常", "周末", "lifestyle", "daily"));
        rules.put("医疗", List.of("health", "vaccine", "disease", "medical", "pandemic"));
        rules.put("政治", List.of("politics", "election", "government", "president", "congress", "law"));
        return rules;
    }

    // ========== 数据类 ==========

    private static class RawPost {
        String text;
        String category;
    }

    public static class ImportResult {
        public String source;
        public boolean success;
        public String message;
        public int importedCount;
        public int skippedDuplicate;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
    }
}
