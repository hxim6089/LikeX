"""
X (Twitter) 帖文爬取脚本
爬取公开推文并导入到推荐系统 MySQL 数据库

使用方法:
  1. pip install tweepy pymysql
  2. python x_scraper.py

说明:
  - 使用 Twitter API v2 (Free tier 支持搜索最近推文)
  - 如果没有 API Key，会退回到使用模拟数据生成模式
  - 爬取中文科技/生活/体育/新闻等领域的帖文
"""

import pymysql
import random
import time
import re
import json
from datetime import datetime, timedelta

# ========== 配置 ==========
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'HXIM6089',
    'database': 'rec_db',
    'charset': 'utf8mb4'
}

# Twitter API Bearer Token (Free tier)
# 申请地址: https://developer.x.com/en/portal/dashboard
# Free tier 每月可读 10,000 条推文
BEARER_TOKEN = "AAAAAAAAAAAAAAAAAAAAAGQx9QEAAAAAdQPRJRhz9JpSLNrfclQsfMVP3Nw%3D5noqwFM0jQ9o0U7HZFtbNpBexoo2aOpA0uXbUqMisHApV7zryI"  # <-- 在此填入你的 Bearer Token

# 爬取的搜索关键词 (覆盖各个分类)
SEARCH_QUERIES = {
    "Tech": [
        "AI 人工智能 lang:zh", "ChatGPT lang:zh", "编程 开发 lang:zh",
        "科技新闻 lang:zh", "Python 教程 lang:zh", "iPhone 发布 lang:zh",
        "数据科学 lang:zh", "机器学习 lang:zh"
    ],
    "Life": [
        "美食推荐 lang:zh", "旅行 打卡 lang:zh", "生活方式 lang:zh",
        "咖啡 日常 lang:zh", "穿搭 分享 lang:zh", "今日份 快乐 lang:zh"
    ],
    "Sports": [
        "NBA 篮球 lang:zh", "健身 运动 lang:zh", "足球 比赛 lang:zh",
        "跑步 马拉松 lang:zh", "世界杯 lang:zh"
    ],
    "News": [
        "热点新闻 lang:zh", "社会热议 lang:zh", "今日头条 lang:zh",
        "国际新闻 lang:zh"
    ],
    "Education": [
        "考研 复习 lang:zh", "学习方法 lang:zh", "大学生活 lang:zh",
        "英语学习 lang:zh", "读书笔记 lang:zh"
    ]
}

# 每个关键词爬取的推文数
TWEETS_PER_QUERY = 15


def try_api_scrape():
    """
    尝试使用 Twitter API v2 爬取
    需要 Bearer Token (Free tier 即可)
    """
    if not BEARER_TOKEN:
        print("⚠️  未配置 BEARER_TOKEN，将使用模拟数据模式")
        return None

    try:
        import tweepy
    except ImportError:
        print("⚠️  tweepy 未安装，运行: pip install tweepy")
        return None

    client = tweepy.Client(bearer_token=BEARER_TOKEN)
    all_tweets = []

    for category, queries in SEARCH_QUERIES.items():
        for query in queries:
            try:
                print(f"  🔍 搜索: {query} (分类: {category})")
                response = client.search_recent_tweets(
                    query=query + " -is:retweet -is:reply",
                    max_results=TWEETS_PER_QUERY,
                    tweet_fields=["created_at", "public_metrics", "text", "author_id"],
                    expansions=["author_id"],
                    user_fields=["username", "name"]
                )

                if response.data:
                    users_map = {}
                    if response.includes and "users" in response.includes:
                        for u in response.includes["users"]:
                            users_map[u.id] = u.username

                    for tweet in response.data:
                        all_tweets.append({
                            "text": tweet.text,
                            "category": category,
                            "author_name": users_map.get(tweet.author_id, "XUser"),
                            "like_count": tweet.public_metrics.get("like_count", 0),
                            "reply_count": tweet.public_metrics.get("reply_count", 0),
                            "retweet_count": tweet.public_metrics.get("retweet_count", 0),
                            "view_count": tweet.public_metrics.get("impression_count", 0),
                            "created_at": tweet.created_at,
                        })

                time.sleep(1)  # 避免限速
            except Exception as e:
                print(f"  ❌ 搜索失败: {e}")
                continue

    return all_tweets if all_tweets else None


def generate_realistic_data():
    """
    生成高质量模拟数据（当 API 不可用时的后备方案）
    内容来源参考真实中文社交媒体帖文风格
    """
    print("📝 使用模拟数据生成模式...")

    templates = {
        "Tech": [
            "刚试了 GPT-4o 的新功能，代码补全能力太强了！写了个自动化脚本，原来要2小时的工作10分钟搞定 🔥",
            "分享一下我用 Python 做数据分析的工作流：Pandas 清洗 → Matplotlib 可视化 → Sklearn 建模，效率拉满",
            "Apple Vision Pro 体验报告：设备重量可以接受，空间计算的交互确实颠覆性的，但内容生态还需要时间",
            "最近在学 Rust，所有权系统一开始确实劝退，但习惯之后写出来的代码质量确实高很多",
            "TikTok 的推荐算法真的厉害，刷了几条健身视频之后首页全是健身内容了，协同过滤+内容理解配合得很好",
            "Claude 3.5 Sonnet 的代码能力我觉得已经超过 GPT-4 了，特别是对长上下文的理解能力",
            "今天部署了一个 Docker 集群，K8s 真的是运维利器，虽然学习曲线陡峭但值得投入",
            "M4 芯片的 MacBook Pro 跑大模型的速度快了40%，苹果的统一内存架构确实适合 AI 推理",
            "React 19 的 Server Components 终于稳定了，Next.js 的开发体验又上了一个台阶",
            "分享一个 VS Code 插件：Codeium，免费的 AI 代码补全，对学生党很友好",
            "微软 Copilot+ PC 发布了，NPU 芯片专门跑本地 AI，Windows 终于认真做 AI 了",
            "今天面试字节跳动，问了推荐系统设计题，幸好之前学过协同过滤和内容推荐的原理",
            "Cursor 编辑器体验：Tab 补全太智能了，连注释都帮你写好，编程效率直接翻倍",
            "Linux 内核6.8发布，支持原生 Rust 驱动开发，内核安全性又上一个台阶",
            "用 Stable Diffusion XL 生成了一组产品设计图，AI 绘画在工业设计领域真的有潜力",
            "OpenAI Sora 生成的视频效果震撼，但离实际商用还有距离，主要是可控性不够",
            "树莓派5+装个 HomeAssistant，全屋智能花了不到500块，性价比拉满",
            "最近在研究 LangChain + RAG 做知识库问答，检索增强生成是当前最实用的大模型应用方向",
            "华为鸿蒙 NEXT 纯血版体验：流畅度确实提升了，但第三方应用适配还需要时间",
            "GitHub Copilot 的 Workspace 功能太强了，直接在聊天框里描述需求就能生成完整的 PR",
            "阿里云发布了通义千问2.5，中文理解能力确实比之前强了很多，国产大模型进步很快",
            "今天学了 WebAssembly，在浏览器里跑 C++ 代码，性能接近原生，前端性能天花板又被打破了",
            "Redis 8.0 发布，内置向量搜索功能，RAG 应用不用再装单独的向量数据库了",
            "分享一个开源项目：Ollama，本地跑大模型超方便，M1 Mac 就能跑 Llama 3",
            "Chrome 推出内置 AI 功能，Gemini Nano 直接在浏览器本地运行，隐私保护是个亮点",
        ],
        "Life": [
            "周末去了南京的先锋书店，文艺气息满满 📚 买了三本村上春树",
            "今天自制了提拉米苏，马斯卡彭奶酪+浓缩咖啡+手指饼干，成品完美 🎂",
            "分享一下我的晨间routine：6:30起床→喝水→冥想10分钟→运动30分钟→丰富早餐",
            "去了成都的宽窄巷子，火锅走起 🌶️ 麻辣牛油锅底绝了，配上油碟简直不要太好吃",
            "入手了戴森吹风机，速干效果确实好，发质也没有之前吹完那么毛躁了",
            "今天做了一杯dirty咖啡☕ 浓缩直接倒在冰牛奶上，分层效果太美了",
            "收到新养的多肉了，品种是桃蛋和橙梦露，放在阳台上等着它们变美 🌱",
            "打卡上海迪士尼，疯狂动物城园区太赞了！加勒比海盗也值得二刷",
            "周末整理了衣柜，断舍离了30件衣服，空间一下子就出来了，心情也好了很多",
            "自学了拉花，第一次拉出了心形 ♥️ 虽然不完美但超有成就感",
            "最近开始记手账了，用的是hobo a6，贴纸+胶带+水彩笔，每页都是一幅画",
            "租了一间coworking space，比在家效率高太多了，关键是认识了一群有趣的人",
            "试了一下轻断食16:8，坚持了一周确实感觉身体轻盈了很多",
            "去了杭州西湖，虽然人很多但断桥残雪的意境还是很美的 🏔️",
            "今天做了酸奶燕麦碗，蓝莓+奇亚籽+格兰诺拉，好看又好吃的健康早餐",
            "入住了一家民宿，窗外就是稻田和远山，终于可以远离城市喘口气了",
            "种了一阳台的薄荷和罗勒🌿 做菜的时候随手摘一把，新鲜又有成就感",
            "今天学做了戚风蛋糕，第三次终于没有塌腰！松软绵密完全不输外面买的",
            "周末逛了万圣节限定市集，买了一堆有趣的手工艺品，南瓜灯超可爱 🎃",
            "分享我的冬日宅家清单：热巧克力+毛毯+Netflix+橘猫，完美周末",
        ],
        "Sports": [
            "今天跑了10公里，配速5分半，感觉状态不错💪 目标是年底完成半马",
            "NBA 季后赛太精彩了！关键时刻的三分绝杀看得我直接从沙发上跳起来了 🏀",
            "健身第100天打卡 🎉 从60kg到75kg，增肌效果很明显，坚持就是胜利",
            "踢了一场五人制足球，2:1赢了对面，进了一个凌空抽射爽到起飞 ⚽",
            "开始学游泳了，蛙泳25米不换气达成！比想象中累多了但是很开心 🏊",
            "看了法网决赛，纳达尔在红土场上的统治力依然恐怖，14冠真的是传奇 🎾",
            "第一次尝试攀岩，5.10a的线路爬了20分钟才到顶，手臂累到发抖但超爽 🧗",
            "分享我的家庭健身计划：俯卧撑+深蹲+平板支撑+哑铃，每天30分钟就够了",
            "滑雪季来了！北大壶上了新缆车，雪质也很好，双板爱好者狂喜 ⛷️",
            "今天骑了50公里公路，沿着海边骑感觉太棒了 🚴 下次挑战100公里",
            "世锦赛4×100接力决赛，中国队冲进决赛了！苏炳添带领的这一代太强了 🇨🇳",
            "瑜伽坚持了半年，柔韧性进步很大，现在可以轻松做到前弯触地了 🧘",
            "打了一场羽毛球，反手高远球终于打到位了，教练说姿势进步很大 🏸",
            "入手了Apple Watch Ultra 2，GPS精度和运动追踪确实是最强的智能手表",
            "跟跑团一起参加了城市越野赛，爬了800米的累计爬升，但风景值得 🏃",
        ],
        "News": [
            "SpaceX 星舰第四次试飞成功回收助推器！🚀 人类离火星又近了一步",
            "诺贝尔物理学奖颁给了AI领域的Hinton和Hopfield，深度学习获得了最高学术认可",
            "全球AI监管峰会召开，28国签署《布莱切利宣言》，AI安全成为国际共识",
            "国际油价跌破70美元，新能源汽车销量创历史新高，能源转型加速中",
            "火星样本返回任务最新进展：NASA和ESA联合方案确定，预计2033年返回地球",
            "聚变能源重大突破：ITER项目达成新里程碑，距离商业化又近一步",
            "央行宣布降准0.5个百分点，释放长期资金约1万亿，A股午后拉升 📈",
            "嫦娥六号成功从月球背面采样返回，中国成为首个完成这一壮举的国家 🌙",
            "全球半导体产业链重构：台积电北美厂投产，三星平泽P4产能扩张",
            "世界卫生组织推荐新一代疟疾疫苗，有望每年挽救数十万儿童生命",
            "碳中和倒计时：欧盟碳关税正式实施，中国碳市场覆盖范围进一步扩大",
            "国际空间站退役方案确定，SpaceX 获得拆除合同，2030年坠入太平洋",
            "全球人口突破81亿，联合国预测2086年达到峰值后开始下降",
            "新一代量子计算机突破1000比特，纠错能力首次超越物理极限",
            "可控核聚变再传捷报：中国环流三号等离子体维持时间突破400秒 ☀️",
        ],
        "Education": [
            "考研倒计时30天！英语二模拟做了5年真题，平均70分，冲刺 💪",
            "分享一下我的四六级备考方法：每天背50个单词+听BBC+做一套真题，稳过",
            "今天论文开题通过了！导师说选题不错，接下来就是苦逼的实验阶段了 📝",
            "推荐一个学习工具：Notion AI，做笔记和知识管理太方便了，学生必备",
            "大三决定跨考计算机，从零开始学数据结构，408是真的难但不能放弃",
            "雅思首考7分！口语6.5是短板，准备二战冲7.5，有没有口语搭子 🗣️",
            "分享一下我的Pomodoro学习法：25分钟专注+5分钟休息，一天能高效学习6小时",
            "MIT OpenCourseWare 的线性代数课程太棒了，Gilbert Strang 讲得深入浅出",
            "实验室发了第一篇SCI！影响因子3.8，虽然不是顶刊但从零到一的突破太开心了 🎉",
            "考公路上的一点感悟：行测要多刷题找规律，申论要多读人民日报社论",
            "这学期选了一门机器学习课，期末项目做了一个推荐系统demo，老师给了A",
            "整理了一份免费学习资源清单：Coursera/edX/Khan Academy/B站，全部免费 📚",
            "保研成功！收到了浙大计算机直博的offer，三年的绩点终于有了回报",
            "今天旁听了一场关于AIGC的学术讲座，大模型在教育领域的应用前景很广",
            "分享我的GRE备考经验：verbal 要狂背单词，quant 对中国学生来说不难",
        ]
    }

    tweets = []
    now = datetime.now()

    for category, texts in templates.items():
        for text in texts:
            hours_ago = random.randint(1, 720)  # 过去30天内随机
            created_at = now - timedelta(hours=hours_ago)

            # 随机互动量
            popularity = random.choice(["low", "medium", "high", "viral"])
            if popularity == "low":
                likes = random.randint(0, 10)
                comments = random.randint(0, 3)
                reposts = random.randint(0, 2)
                views = random.randint(10, 200)
            elif popularity == "medium":
                likes = random.randint(10, 100)
                comments = random.randint(3, 20)
                reposts = random.randint(2, 15)
                views = random.randint(200, 2000)
            elif popularity == "high":
                likes = random.randint(100, 1000)
                comments = random.randint(20, 100)
                reposts = random.randint(15, 80)
                views = random.randint(2000, 20000)
            else:
                likes = random.randint(1000, 10000)
                comments = random.randint(100, 500)
                reposts = random.randint(80, 500)
                views = random.randint(20000, 100000)

            tweets.append({
                "text": text,
                "category": category,
                "author_name": None,
                "like_count": likes,
                "reply_count": comments,
                "retweet_count": reposts,
                "view_count": views,
                "created_at": created_at,
            })

    random.shuffle(tweets)
    return tweets


def extract_hashtags(text):
    """从文本中提取 hashtag 关键词"""
    # 中文关键词提取（简单版）
    keywords = {
        "AI": ["AI", "人工智能", "GPT", "ChatGPT", "大模型", "深度学习", "机器学习"],
        "编程": ["Python", "编程", "代码", "开发", "GitHub", "Rust", "React", "前端"],
        "科技": ["科技", "芯片", "Apple", "华为", "iPhone", "MacBook"],
        "美食": ["美食", "咖啡", "蛋糕", "火锅", "早餐"],
        "旅行": ["旅行", "打卡", "迪士尼", "西湖"],
        "健身": ["健身", "跑步", "运动", "瑜伽", "游泳"],
        "篮球": ["NBA", "篮球"],
        "足球": ["足球", "世界杯"],
        "考研": ["考研", "考公", "雅思", "GRE"],
        "学习": ["学习", "读书", "课程", "论文"],
        "航天": ["SpaceX", "NASA", "火星", "嫦娥"],
        "经济": ["央行", "A股", "油价"],
        "生活": ["生活", "日常", "周末"],
    }

    found = set()
    for tag, kws in keywords.items():
        for kw in kws:
            if kw.lower() in text.lower():
                found.add(tag)
                break
    return list(found) if found else [None]


def import_to_mysql(tweets):
    """将推文数据导入 MySQL"""
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    # 获取现有用户 ID 列表
    cursor.execute("SELECT id FROM users")
    user_ids = [row[0] for row in cursor.fetchall()]
    if not user_ids:
        print("❌ 数据库中没有用户！请先创建用户。")
        return

    print(f"📊 找到 {len(user_ids)} 个用户")

    # 获取/创建标签
    tag_cache = {}  # name -> id
    cursor.execute("SELECT id, name FROM tb_tag")
    for row in cursor.fetchall():
        tag_cache[row[1]] = row[0]

    inserted = 0

    for tweet in tweets:
        try:
            author_id = random.choice(user_ids)
            created_at = tweet["created_at"]
            if isinstance(created_at, datetime):
                created_at_str = created_at.strftime("%Y-%m-%d %H:%M:%S")
            else:
                created_at_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            # 从内容截取前20个字做标题
            title = tweet["text"][:20].rstrip() + "..."

            # 插入帖子
            cursor.execute("""
                INSERT INTO contents (title, content, category, author_id, 
                    like_count, comment_count, repost_count, view_count, 
                    created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (
                title,
                tweet["text"],
                tweet["category"],
                author_id,
                tweet.get("like_count", 0),
                tweet.get("reply_count", 0),
                tweet.get("retweet_count", 0),
                tweet.get("view_count", 0),
                created_at_str
            ))

            content_id = cursor.lastrowid

            # 提取标签并关联
            tags = extract_hashtags(tweet["text"])
            for tag_name in tags:
                if tag_name is None:
                    continue
                # 创建或获取Tag
                if tag_name not in tag_cache:
                    cursor.execute(
                        "INSERT INTO tb_tag (name) VALUES (%s) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                        (tag_name,)
                    )
                    tag_cache[tag_name] = cursor.lastrowid

                tag_id = tag_cache[tag_name]
                # 关联
                cursor.execute(
                    "INSERT IGNORE INTO content_tags (content_id, tag_id) VALUES (%s, %s)",
                    (content_id, tag_id)
                )

            inserted += 1

        except Exception as e:
            print(f"  ⚠️ 插入失败: {e}")
            continue

    conn.commit()

    # 同时生成一些行为数据 (LIKE / VIEW)
    print("🔄 生成用户行为数据...")
    cursor.execute("SELECT id FROM contents ORDER BY id DESC LIMIT %s", (inserted,))
    new_content_ids = [row[0] for row in cursor.fetchall()]

    behavior_count = 0
    for content_id in new_content_ids:
        # 随机选几个用户产生行为
        num_actions = random.randint(1, min(5, len(user_ids)))
        action_users = random.sample(user_ids, num_actions)
        for uid in action_users:
            # 随机行为类型
            action = random.choices(
                ["VIEW", "LIKE", "COMMENT", "REPOST"],
                weights=[50, 30, 15, 5],
                k=1
            )[0]
            hours_ago = random.randint(0, 168)  # 过去一周内
            action_time = datetime.now() - timedelta(hours=hours_ago)
            try:
                cursor.execute(
                    "INSERT INTO behaviors (user_id, content_id, type, created_at) VALUES (%s, %s, %s, %s)",
                    (uid, content_id, action, action_time.strftime("%Y-%m-%d %H:%M:%S"))
                )
                behavior_count += 1
            except:
                pass

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\n✅ 完成！")
    print(f"   📄 导入帖文: {inserted} 条")
    print(f"   🏷️ 标签数量: {len(tag_cache)} 个")
    print(f"   🎯 行为记录: {behavior_count} 条")


def main():
    print("=" * 50)
    print("  X (Twitter) 帖文爬取 & 导入工具")
    print("=" * 50)
    print()

    # 1. 尝试 API 爬取
    tweets = try_api_scrape()

    # 2. 后备：使用模拟数据
    if tweets is None:
        tweets = generate_realistic_data()

    print(f"\n📦 共获取 {len(tweets)} 条帖文")

    # 3. 导入数据库
    print("\n💾 开始导入 MySQL...")
    import_to_mysql(tweets)


if __name__ == "__main__":
    main()
