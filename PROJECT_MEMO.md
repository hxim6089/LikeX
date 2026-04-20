# 推荐系统项目备忘录 (Project Memo)

> **最后更新时间**: 2026-03-27 23:19
> **项目名称**: recommendation-system (类Twitter推荐系统)
> **技术栈**: Spring Boot 3.2.1 + Vue 3 + MySQL + Redis + ECharts

---

## 📋 目录

1. [项目概述](#项目概述)
2. [技术配置](#技术配置)
3. [已实现功能](#已实现功能)
4. [业务逻辑说明](#业务逻辑说明)
5. [重要注意事项](#重要注意事项)
6. [已知问题与解决方案](#已知问题与解决方案)
7. [API 端点列表](#api-端点列表)
8. [数据模型](#数据模型)

---

## 项目概述

这是一个类似 Twitter/X 的社交媒体推荐系统，包含以下核心功能：
- 用户认证与个人资料管理
- 内容发布（帖子、评论、图片）
- 社交互动（点赞、评论、关注、转发、引用）
- 智能推荐算法（混合推荐策略 + TF-IDF 内容相似度）
- 实时通知（WebSocket）
- 用户画像分析（多维度可视化）
- 负面信号过滤（屏蔽、静音、不感兴趣）
- AI 集成基础设施（支持本地/远程大模型）
- 广告智能分发
- 算法可视化与对比实验（答辩展示）
- 外部数据导入（Python 爬虫）

---

## 技术配置

### 后端 (backend/)
| 配置项 | 值 | 说明 |
|--------|-----|------|
| 端口 | **8888** | ⚠️ 不使用8080/8081，因为被Windows Hyper-V保留 |
| 数据库 | MySQL `rec_db` | localhost:3306 |
| 缓存 | Redis | localhost:6379，可设为 `none` 禁用 |
| 文件上传 | 10MB限制 | 保存在 `backend/uploads/` |
| 图片访问 | `/images/**` | 映射到 `uploads/` 目录 |

### 前端 (frontend/)
| 配置项 | 值 | 说明 |
|--------|-----|------|
| 端口 | 5173 | Vite 开发服务器 |
| API 基础URL | `http://localhost:8888/api` | 在 `src/api.js` 中配置 |
| WebSocket | `http://localhost:8888/ws` | 在 `src/utils/websocket.js` 中配置 |
| 代理 | `/api` → `localhost:8888` | 在 `vite.config.js` 中配置 |

### 关键配置文件
- `backend/src/main/resources/application.properties` - 后端配置
- `frontend/src/api.js` - API 基础URL
- `frontend/vite.config.js` - Vite代理配置
- `frontend/src/utils/websocket.js` - WebSocket连接配置

---

## 已实现功能

### ✅ 用户系统
| 功能 | 状态 | 说明 |
|------|------|------|
| 用户注册 | ✅ | POST `/api/auth/register` |
| 用户登录 | ✅ | POST `/api/auth/login` |
| 个人资料查看 | ✅ | GET `/api/user/{id}/persona` |
| 个人资料编辑 | ✅ | PUT `/api/user/{id}` |
| 头像上传 | ✅ | POST `/api/upload` |

### ✅ 内容系统
| 功能 | 状态 | 说明 |
|------|------|------|
| 发布帖子 | ✅ | POST `/api/content/publish` |
| 获取信息流 | ✅ | GET `/api/content/feed` |
| 获取单条内容 | ✅ | GET `/api/content/{id}` |
| 获取用户帖子 | ✅ | GET `/api/content/user/{userId}` |
| 图片上传 | ✅ | POST `/api/upload` |
| 评论 | ✅ | POST `/api/content/{id}/comment` |
| 转发 (Repost) | ✅ | POST `/api/content/{id}/repost` |
| 引用 (Quote) | ✅ | POST `/api/content/{id}/quote` |

### ✅ 社交互动
| 功能 | 状态 | 说明 |
|------|------|------|
| 点赞/取消点赞 | ✅ | POST `/api/content/{id}/like`, `/unlike` |
| 关注用户 | ✅ | POST `/api/relation/follow` |
| 取消关注 | ✅ | POST `/api/relation/unfollow` |
| 关注状态查询 | ✅ | GET `/api/relation/status` |

### ✅ 推荐系统
| 功能 | 状态 | 说明 |
|------|------|------|
| 混合推荐策略 | ✅ | HybridRecommendationStrategy.java |
| 双候选源 | ✅ | In-Network (Thunder) + Out-of-Network (Phoenix) |
| 多行为加权评分 | ✅ | like×0.5 + comment×1.2 + repost×2.0 + quote×1.8 |
| 分段时间衰减 | ✅ | 0-6h黄金期 / 6-24h正常 / 24-72h加速 / 72h+对数长尾 |
| 作者多样性惩罚 | ✅ | 同作者多篇降权 (×0.7^N) |
| 个性化标签匹配 | ✅ | 匹配用户兴趣标签 +100分 |
| TF-IDF 内容相似度 | ✅ | 余弦相似度 × 80.0 加成 |
| 负面信号过滤 | ✅ | 屏蔽/静音/不感兴趣 |
| 热门话题加成 | ✅ | TrendingService ×50.0 |
| 协同过滤 | ✅ | CollaborativeFilteringService |
| 互动率因子 | ✅ | 高互动率内容加分 |
| 随机抖动 (Jitter) | ✅ | ±15% 随机抖动确保每次刷新结果不同 |
| 加权随机采样 | ✅ | Exploration-Exploitation 机制 |
| 动态权重调节 | ✅ | 前端参数调节面板，按用户存储自定义权重 |
| 算法对比实验 | ✅ | 推荐流 vs 时间流并排对比 |
| 管道漏斗图 | ✅ | 候选池 → 过滤 → 评分 → 多样性 可视化 |

### ✅ 用户画像
| 功能 | 状态 | 说明 |
|------|------|------|
| 兴趣标签分析 | ✅ | 基于点赞行为统计分类 |
| 兴趣衰减评分 | ✅ | 30/60/90天分段计算，趋势判断 (rising/falling/stable) |
| 用户行为分型 | ✅ | Creator / Interactor / Consumer 三角模型 |
| 活跃度等级 | ✅ | Power User / High / Medium / Low |
| 活跃时段分布 | ✅ | 24小时 ECharts 柱状图 + 峰值高亮 |
| 内容偏好分析 | ✅ | 阅读长度/图片偏好/话题多样性 |
| 概览仪表盘 | ✅ | 6指标摘要面板 (用户类型/活跃度/阅读偏好/图片偏好/多样性/夜猫子) |
| 分类偏好环形图 | ✅ | ECharts 饼图，中心显示总点赞数 |
| 兴趣词云 | ✅ | echarts-wordcloud 可视化 |
| 行为雷达图 | ✅ | 点赞/评论/转发/浏览 四维雷达 |
| 推荐匹配度 | ✅ | 基于互动行为计算匹配准确度 |
| 画像卡片导出 | ✅ | html2canvas 导出 PNG |
| 关注数/粉丝数 | ✅ | 实时查询数据库 |
| 加入时间 | ✅ | 从 createdAt 动态格式化 |
| 入场动画 | ✅ | CSS stagger fadeSlideUp |
| Redis缓存 | ⚠️ | 需要LocalDateTime转String避免序列化错误 |

### ✅ 实时通知
| 功能 | 状态 | 说明 |
|------|------|------|
| WebSocket连接 | ✅ | STOMP over SockJS |
| 点赞通知 | ✅ | 推送到 `/user/{userId}/queue/notifications` |
| 评论通知 | ✅ | 同上 |
| 关注通知 | ✅ | 同上 |
| 私信通知 | ✅ | 推送到 `/user/{userId}/queue/messages` |

### ✅ 负面信号
| 功能 | 状态 | 说明 |
|------|------|------|
| 屏蔽用户 | ✅ | BLOCK信号 |
| 静音用户 | ✅ | MUTE信号 |
| 不感兴趣 | ✅ | NOT_INTERESTED信号 |
| 推荐过滤 | ✅ | 负面信号内容/作者不再推荐 |

---

## 业务逻辑说明

### 1. 推荐算法流程

```
用户请求Feed → 构建候选池(In-Network+Out-of-Network)
→ 负面信号过滤 → 多因子评分 → 作者多样性惩罚
→ 随机抖动(±15%) → 排序 → 返回结果
```

**评分公式**:
```
finalScore = (baseEngagement / timeDecay)
           + personalizationBoost
           + tfidfSimilarity
           + trendingBoost
           + jitter(±15%)
```

**行为权重**:
- VIEW: 0.05
- LIKE: 0.5
- COMMENT: 1.2
- REPOST: 2.0
- QUOTE: 1.8

**分段时间衰减**:
- 0-6h: 黄金期（几乎不衰减）
- 6-24h: 正常指数衰减
- 24-72h: 加速衰减
- 72h+: 对数长尾衰减

### 2. 用户画像计算

```java
// PersonaService.getUserPersona(userId)
1. 查询用户基础信息 + 关注数/粉丝数
2. 获取用户所有行为记录 (VIEW/LIKE/COMMENT/REPOST)
3. 计算兴趣标签分布 (Top3 分类)
4. 兴趣衰减评分 (30/60/90天分段)
5. 用户行为分型 (Creator/Interactor/Consumer)
6. 活跃度等级 + 活跃时段分布
7. 内容偏好分析 (阅读长度/图片偏好/话题多样性)
8. 词云数据生成
9. 生成称号 + 推荐匹配度
```

### 3. WebSocket通知机制

```
后端创建通知 → SimpMessagingTemplate.convertAndSendToUser() 
→ 前端STOMP订阅 /user/{userId}/queue/notifications
→ ElNotification弹窗显示
```

### 4. 负面信号过滤

```java
// NegativeSignalService
1. 用户标记内容为"不感兴趣" → 记录 (userId, contentId, NOT_INTERESTED)
2. 用户屏蔽某作者 → 记录 (userId, authorId, BLOCK)
3. 推荐时过滤:
   - 排除用户已标记NOT_INTERESTED的内容
   - 排除用户BLOCK/MUTE的作者发布的内容
```

---

## 重要注意事项

### ⚠️ 端口配置
- **后端必须使用8888端口**，不要使用8080或8081
- Windows Hyper-V会保留7981-8080和8081-8180端口范围
- 更改端口后需要同步修改前端所有涉及端口的文件

### ⚠️ 图片URL
- 数据库中的图片URL必须使用当前后端端口 (8888)
- 如果更换端口，需要执行SQL更新:
  ```sql
  UPDATE users SET avatar_url = REPLACE(avatar_url, 'localhost:旧端口', 'localhost:新端口');
  UPDATE contents SET image_url = REPLACE(image_url, 'localhost:旧端口', 'localhost:新端口');
  ```

### ⚠️ Redis缓存
- `@Cacheable` 注解的方法返回值必须是可序列化的
- `LocalDateTime` 类型需要转换为 `String` 再放入Map
- 可通过设置 `spring.cache.type=none` 临时禁用缓存排查问题

### ⚠️ SockJS兼容性
- 前端 `index.html` 需要添加 `window.global = window;` polyfill
- 否则会报 `global is not defined` 错误

### ⚠️ 前端模块引用
- Vue组件中使用Element Plus图标需要显式导入
- 例如: `import { Search } from '@element-plus/icons-vue'`

---

## 已知问题与解决方案

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Port 8080 was already in use | Windows Hyper-V保留端口 | 使用8888端口 |
| global is not defined | sockjs-client与Vite不兼容 | index.html添加polyfill |
| 图片加载失败 | 数据库URL端口不对 | 执行SQL更新URL |
| 500 Internal Server Error (Persona) | LocalDateTime序列化失败 | 转换为String |
| Failed to resolve component: Search | 未导入Element Plus图标 | 添加import语句 |
| 个人主页需二次点击加载 | 首次请求可能超时 | 检查后端是否正常运行 |

---

## API 端点列表

### 认证 (AuthController)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |

### 内容 (ContentController)
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/content/feed` | 获取推荐信息流 |
| GET | `/api/content/{id}` | 获取单条内容 |
| GET | `/api/content/user/{userId}` | 获取用户帖子 |
| POST | `/api/content/publish` | 发布内容 |
| POST | `/api/content/{id}/like` | 点赞 |
| POST | `/api/content/{id}/unlike` | 取消点赞 |
| POST | `/api/content/{id}/comment` | 评论 |
| POST | `/api/content/{id}/repost` | 转发 |
| POST | `/api/content/{id}/quote` | 引用 |

### 用户 (UserController / PersonaController)
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/user/{id}/persona` | 获取用户画像 |
| PUT | `/api/user/{id}` | 更新用户信息 |

### 关系 (RelationController)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/relation/follow` | 关注 |
| POST | `/api/relation/unfollow` | 取消关注 |
| GET | `/api/relation/status` | 查询关注状态 |

### 负面信号 (NegativeSignalController)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/negative-signal` | 记录负面信号 |
| DELETE | `/api/negative-signal` | 移除负面信号 |
| GET | `/api/negative-signal` | 查询负面信号 |

### 通知 (NotificationController)
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/notifications` | 获取通知列表 |

### 文件上传 (FileUploadController)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/upload` | 上传文件 |

### 搜索 (SearchController) - Phase 26
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/search` | 综合搜索 (帖子/用户/话题) |
| GET | `/api/search?type=posts` | 仅搜索帖子 |
| GET | `/api/search?type=users` | 仅搜索用户 |
| GET | `/api/search?type=topics` | 仅搜索话题 |
| GET | `/api/search/suggest` | 搜索建议 (自动补全) |

### 热门话题 (TrendingController) - Phase 26
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/trending` | 获取热门话题列表 |

### AI 助手 (AiController)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | AI 问答对话 (支持 Ollama/OpenAI) |

### 广告 (AdController)
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/ads/relevant` | 获取用户匹配广告 |

### 数据导入 (ImportController)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/import/scraped-data` | 导入 Python 爬虫抓取的数据 |

### 算法对比 (ExperimentController) - Phase 28
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/experiment/compare` | 获取推荐流 vs 时间流对比数据 |
| GET | `/api/experiment/pipeline` | 获取管道漏斗图数据 |

### 权重调节 (WeightsController) - Phase 28
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/weights/{userId}` | 获取用户自定义权重 |
| PUT | `/api/weights/{userId}` | 保存用户自定义权重 |
| DELETE | `/api/weights/{userId}` | 恢复默认权重 |

---

## 数据模型

### User (用户)
```java
- id: Long (PK)
- username: String
- handle: String (@用户名)
- password: String
- avatarUrl: String
- bio: String
- role: String (USER/ADMIN)
- createdAt: LocalDateTime
```

### Content (内容/帖子)
```java
- id: Long (PK)
- title: String
- content: String (TEXT)
- imageUrl: String
- category: String (Tech/Life/Sports/News)
- author: User (FK)
- parentContent: Content (FK, 用于评论)
- repostOf: Content (FK, 转发原帖)
- quoteOf: Content (FK, 引用原帖)
- viewCount, likeCount, commentCount, repostCount: Integer
- tags: Set<Tag> (多对多)
- createdAt: LocalDateTime
```

### Behavior (用户行为)
```java
- id: Long (PK)
- userId: Long
- contentId: Long
- type: String (VIEW/LIKE/COMMENT/REPOST/QUOTE)
- createdAt: LocalDateTime
```

### NegativeSignal (负面信号)
```java
- id: Long (PK)
- userId: Long
- targetId: Long
- targetType: Enum (CONTENT/USER)
- signalType: Enum (BLOCK/MUTE/NOT_INTERESTED)
- createdAt: LocalDateTime
```

### Notification (通知)
```java
- id: Long (PK)
- recipientId: Long
- actorId: Long
- type: String (LIKE/COMMENT/FOLLOW/REPOST/QUOTE)
- entityId: Long
- isRead: Boolean
- createdAt: LocalDateTime
```

---

## 变更日志

| 日期 | 变更内容 |
|------|---------|
| 2026-01-27 | 创建备忘录文件 |
| 2026-01-27 | 端口从8080改为8888 (避免Hyper-V冲突) |
| 2026-01-27 | 修复PersonaService LocalDateTime序列化问题 |
| 2026-01-27 | 添加sockjs global polyfill |
| 2026-01-27 | 更新数据库图片URL端口 |
| 2026-01-29 | **Phase 25**: 推荐算法优化 - 新增转发/引用权重、热门话题加成、互动率因子 |
| 2026-01-29 | **Phase 25**: 新增 TrendingService, CollaborativeFilteringService |
| 2026-01-29 | **Phase 26**: 新增 SearchController 综合搜索 API |
| 2026-01-29 | **Phase 26**: 新增 TrendingController 热门话题 API |
| 2026-01-29 | **Phase 26**: 前端新增 UserCard, TopicCard 组件 |
| 2026-01-29 | **Phase 26**: 更新 SearchView 添加标签页切换 |
| 2026-01-29 | **Phase 26**: 更新 RightPanel 热门话题展示和搜索建议 |
| 2026-03-27 | **Phase 29**: 精细化用户画像 - 概览仪表盘(6指标) |
| 2026-03-27 | **Phase 29**: 分类偏好进度条改为 ECharts 环形饼图 |
| 2026-03-27 | **Phase 29**: 活跃时段热力图改为 ECharts 柱状图 |
| 2026-03-27 | **Phase 29**: 新增卡片逐级淡入动画 |
| 2026-03-27 | **Phase 29**: 修复关注数/粉丝数/加入时间硬编码问题 |
| 2026-03-27 | **修复**: 推荐流每次刷新结果相同 - 添加±15%随机抖动 |
| 2026-03-27 | **修复**: Insights 面板显示比例 - 宽度扩展800px + 图表高度增加 |
| 2026-03-27 | **新增**: Admin Dashboard 画像详情弹窗 (📊 详情按钮) |
| 2026-03-28 | **修复**: Insights ECharts 图表尺寸异常 - v-show→v-if 避免隐藏容器初始化 |
| 2026-04-04 | **新增**: JWT Token 鉴权 v2（软校验模式: permitAll + Filter 仅注入 Context） |
| 2026-04-04 | **新增**: @EnableAsync 启用异步线程池 |
| 2026-04-07 | **新增**: 全局错误提示拦截器 (api.js ElMessage toast) |
| 2026-04-07 | **新增**: 关注/粉丝列表弹窗 (ProfileView 点击数字查看) |
| 2026-04-07 | **新增**: 帖子删除功能 (TweetCard 垃圾桶图标, 仅作者可删) |
| 2026-04-07 | **新增**: 搜索结果分页 (posts tab 支持"加载更多") |
| 2026-04-12 | **新增**: 管理员权限体系 (Sidebar v-if + 路由守卫) |
| 2026-04-12 | **新增**: 管理员删除任意帖子 (ADMIN 角色跳过作者校验) |
| 2026-04-12 | **新增**: 用户角色管理 (Admin Switch 切换 USER/ADMIN) |
| 2026-04-12 | **新增**: 平台数据概览 (AdminController /stats + 统计卡片) |
| 2026-04-12 | **新增**: AI 批量打标按钮 (调用 /ai/tag-all) |
| 2026-04-12 | **新增**: 用户封禁/解封功能 (User.banned + 登录拦截) |
| 2026-04-19 | **修复**: 侧边栏选中状态样式 - 添加背景色(#e8f5fd)和字体颜色(#1d9bf0)高亮，支持 router-link-exact-active |
| 2026-04-19 | **修复**: 多级评论刷新丢失 - getComments 改为递归加载，支持任意深度嵌套评论 |
| 2026-04-19 | **优化**: Ask Grok 帖文分析提示词 - 注入作者/标签/互动数据/分类等元信息，多维度分析 |
| 2026-04-19 | **新增**: Grok 聊天记录持久化 - localStorage 保存对话历史，支持清空对话按钮 |
| 2026-04-19 | **新增**: Grok 多轮对话上下文 - 前端发送最近20条历史，后端拼装完整 messages 数组 |
| 2026-04-19 | **新增**: Grok 系统提示词 - AiService 添加 SYSTEM_PROMPT 注入平台功能描述，AI 具备平台认知 |
| 2026-04-20 | **新增**: Ask Grok 分析结果缓存 - localStorage 按帖子ID缓存AI回复(key: grok_analysis_{id})，避免重复API调用 |

