# 推荐系统项目备忘录 (Project Memo)

> **最后更新时间**: 2026-01-27 21:50
> **项目名称**: recommendation-system (类Twitter推荐系统)
> **技术栈**: Spring Boot 3.2.1 + Vue 3 + MySQL + Redis

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
- 智能推荐算法（混合推荐策略）
- 实时通知（WebSocket）
- 用户画像分析
- 负面信号过滤（屏蔽、静音、不感兴趣）

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
| 双候选源 | ✅ | In-Network + Out-of-Network |
| 多行为加权评分 | ✅ | 点赞0.5 + 评论1.2 + 转发2.0 |
| 时间衰减 | ✅ | 对数衰减函数 |
| 作者多样性惩罚 | ✅ | 同作者多篇降权 |
| 个性化加成 | ✅ | 匹配用户兴趣标签 |
| 负面信号过滤 | ✅ | 屏蔽/静音/不感兴趣 |
| 热门话题加成 | ✅ | TrendingService |
| 协同过滤 | ✅ | CollaborativeFilteringService |
| 互动率因子 | ✅ | 高互动率内容加分 |

### ✅ 用户画像
| 功能 | 状态 | 说明 |
|------|------|------|
| 兴趣标签分析 | ✅ | 基于点赞行为统计分类 |
| 画像称号 | ✅ | Tech Enthusiast, Life Observer 等 |
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
用户请求Feed → 获取候选内容 → 应用负面信号过滤 → 计算评分 → 排序 → 返回结果
```

**评分公式**:
```
finalScore = baseScore × 行为权重 × (1 + 个性化加成) × 时间衰减 × 作者多样性惩罚
```

**行为权重**:
- VIEW: 0.1
- LIKE: 0.5
- COMMENT: 1.0
- REPOST: 1.5
- QUOTE: 2.0

**时间衰减**: 
- 超过48小时的内容分数乘以0.5

### 2. 用户画像计算

```java
// PersonaService.getUserPersona(userId)
1. 查询用户基础信息
2. 获取用户所有LIKE行为
3. 统计点赞内容的分类分布
4. 取Top3分类作为兴趣标签
5. 根据第一兴趣分类生成称号
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

### 搜索 (SearchController) - Phase 26 新增
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/search` | 综合搜索 (帖子/用户/话题) |
| GET | `/api/search?type=posts` | 仅搜索帖子 |
| GET | `/api/search?type=users` | 仅搜索用户 |
| GET | `/api/search?type=topics` | 仅搜索话题 |
| GET | `/api/search/suggest` | 搜索建议 (自动补全) |

### 热门话题 (TrendingController) - Phase 26 新增
| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/trending` | 获取热门话题列表 |

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
