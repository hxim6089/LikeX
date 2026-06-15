"""
大规模内容导入脚本 v2
- 300+ 条中英双语帖文
- 30 天均匀时间分布
- 用户画像绑定：每个用户有偏好分类，行为数据围绕其偏好倾斜
- 生成 LIKE/COMMENT/REPOST/VIEW 行为，让推荐算法有充足信号

运行: python x_scraper_v2.py
"""

import os
import pymysql
import random
from datetime import datetime, timedelta

DB_CONFIG = {
    'host': 'localhost', 'port': 3306,
    'user': 'root', 'password': os.environ.get('DB_PASSWORD'),
    'database': 'rec_db', 'charset': 'utf8mb4'
}

# ===================================================================
# 300+ 条帖文模板 (中英双语, 5 大分类)
# ===================================================================

POSTS = {
  "Tech": [
    # 中文
    "刚试了 GPT-4o 的新功能，代码补全能力太强了！写了个自动化脚本，原来要2小时的工作10分钟搞定 🔥",
    "分享一下我用 Python 做数据分析的工作流：Pandas 清洗 → Matplotlib 可视化 → Sklearn 建模，效率拉满",
    "Apple Vision Pro 体验报告：空间计算的交互确实颠覆性的，但内容生态还需要时间建设",
    "最近在学 Rust，所有权系统一开始确实劝退，但习惯之后写出来的代码质量高很多",
    "推荐算法真的厉害，刷了几条健身视频之后首页全是健身内容了，协同过滤+内容理解配合得很好",
    "Claude 3.5 Sonnet 的代码能力已经超过 GPT-4 了，特别是对长上下文的理解能力",
    "今天部署了一个 Docker 集群，K8s 真的是运维利器，虽然学习曲线陡峭但值得投入",
    "M4 芯片的 MacBook Pro 跑大模型的速度快了40%，统一内存架构确实适合 AI 推理",
    "React 19 的 Server Components 终于稳定了，Next.js 的开发体验又上了一个台阶",
    "VS Code 插件推荐：Codeium，免费的 AI 代码补全，对学生党很友好",
    "微软 Copilot+ PC 发布了，NPU 芯片专门跑本地 AI，Windows 终于认真做 AI 了",
    "今天面试大厂，问了推荐系统设计题，幸好之前学过协同过滤和内容推荐的原理",
    "Cursor 编辑器体验：Tab 补全太智能了，连注释都帮你写好，编程效率直接翻倍",
    "Linux 内核6.8发布，支持原生 Rust 驱动开发，内核安全性又上一个台阶",
    "用 Stable Diffusion XL 生成了一组产品设计图，AI 绘画在工业设计领域真的有潜力",
    "OpenAI Sora 生成的视频效果震撼，但离实际商用还有距离，主要是可控性不够",
    "树莓派5装个 HomeAssistant，全屋智能花了不到500块，性价比拉满",
    "最近在研究 LangChain + RAG 做知识库问答，检索增强生成是当前最实用的大模型应用方向",
    "华为鸿蒙 NEXT 纯血版体验：流畅度确实提升了，但第三方应用适配还需要时间",
    "GitHub Copilot Workspace 太强了，描述需求就能生成完整 PR",
    "通义千问2.5中文理解能力比之前强了很多，国产大模型进步很快",
    "WebAssembly 在浏览器里跑 C++ 代码，性能接近原生，前端天花板被打破了",
    "Redis 8.0 内置向量搜索功能，RAG 应用不用再装单独的向量数据库了",
    "开源项目推荐：Ollama，本地跑大模型超方便，M1 Mac 就能跑 Llama 3",
    "Chrome 推出内置 AI 功能，Gemini Nano 直接在本地运行，隐私保护是个亮点",
    "Spring Boot 3.3 支持虚拟线程了！并发性能直接翻了5倍，不再需要 WebFlux",
    "TypeScript 5.5 引入了 inferred type predicates，类型推断更智能了",
    "Figma 的 AI 功能发布了，从设计稿直接生成前端代码，设计师和开发者的桥梁",
    "PostgreSQL 17 发布了，JSON 性能提升 30%，逐渐成为全能数据库",
    "刚体验了 Arc 浏览器，板块化管理标签页的设计太赞了，终于告别标签页地狱",
    # 英文
    "Just deployed my first app on Vercel with Next.js 14. The DX is incredible—zero config, instant deploys 🚀",
    "Hot take: Rust will replace C++ in systems programming within 10 years. Memory safety without GC is the future.",
    "Been learning Kubernetes for 2 weeks now. Finally understand why everyone says the learning curve is steep but worth it.",
    "The new Claude 3.5 Sonnet is genuinely impressive for coding. It understands project context way better than GPT-4.",
    "Just switched from VS Code to Cursor and my productivity went through the roof. AI-assisted coding is no joke.",
    "Docker Compose makes local dev environments so much easier. No more 'works on my machine' excuses 🐳",
    "Interesting paper on RAG vs fine-tuning: RAG wins for most enterprise use cases. Lower cost, easier to update.",
    "GitHub Copilot just saved me 3 hours of boilerplate code. The future of software engineering is here.",
    "React Server Components finally clicked for me. The mental model shift is real but the performance gains are worth it.",
    "Just hit 100 stars on my open source project! Small milestone but feels amazing. OSS community is the best 🌟",
    "Honest review: Apple Vision Pro is technically impressive but way too heavy for daily use. Maybe v3 will nail it.",
    "PostgreSQL 17 benchmarks are insane. 30% faster JSON queries and native vector search. Bye bye specialized DBs.",
    "Spent the weekend building a RAG chatbot with LangChain. Way easier than expected, the docs have improved a lot.",
    "The AI coding assistant landscape is wild right now. Cursor, Copilot, Codeium, Windsurf... competition is great for devs.",
    "Just migrated our monolith to microservices. Took 6 months but latency dropped 60% and deploys are 10x faster.",
    "Tailwind CSS v4 alpha is looking promising. Native CSS cascade layers and no more config file bloat.",
    "WebAssembly + Rust for the frontend is underrated. We got 5x performance improvement on our data visualization tool.",
    "GraphQL vs REST debate is pointless. Use whatever fits your use case. We use both in our stack and it works great.",
    "Just finished Stanford's CS229 machine learning course online. Free education of this quality is incredible.",
    "The LLM bubble will burst but the technology is real. We need to focus on practical applications, not hype.",
  ],
  "Life": [
    # 中文
    "周末去了南京的先锋书店，文艺气息满满 📚 买了三本村上春树",
    "今天自制了提拉米苏，马斯卡彭奶酪+浓缩咖啡+手指饼干，成品完美 🎂",
    "分享晨间routine：6:30起床→喝水→冥想10分钟→运动30分钟→丰富早餐",
    "去了成都宽窄巷子，火锅走起 🌶️ 麻辣牛油锅底配油碟简直不要太好吃",
    "入手了戴森吹风机，速干效果确实好，发质也没有之前吹完那么毛躁了",
    "今天做了一杯dirty咖啡☕ 浓缩直接倒在冰牛奶上，分层效果太美了",
    "新养的多肉到了，品种是桃蛋和橙梦露，放阳台上等着它们变美 🌱",
    "打卡上海迪士尼，疯狂动物城园区太赞了！加勒比海盗也值得二刷",
    "周末断舍离了30件衣服，空间一下就出来了，心情也好了很多",
    "自学拉花，第一次拉出心形 ♥️ 虽然不完美但超有成就感",
    "开始记手账了，用hobo a6，贴纸+胶带+水彩笔，每页都是一幅画",
    "租了coworking space，比在家效率高太多了，关键是认识了一群有趣的人",
    "轻断食16:8坚持一周确实感觉身体轻盈了很多",
    "杭州西湖，虽然人很多但断桥残雪的意境还是很美 🏔️",
    "做了酸奶燕麦碗，蓝莓+奇亚籽+格兰诺拉，好看又好吃的健康早餐",
    "入住民宿，窗外就是稻田和远山，终于可以远离城市喘口气了",
    "种了一阳台的薄荷和罗勒🌿 做菜时随手摘一把新鲜又有成就感",
    "戚风蛋糕第三次终于没塌腰！松软绵密完全不输外面买的",
    "逛了万圣节限定市集，买了一堆手工艺品，南瓜灯超可爱 🎃",
    "冬日宅家清单：热巧克力+毛毯+Netflix+橘猫，完美周末",
    "学了一首新的吉他曲，《Wonderwall》终于能完整弹下来了 🎸",
    "重新整理了书架，按颜色排列的效果意外地好看",
    "今天试了新的烘焙食谱——法式可丽饼，简单又好吃",
    "带猫去做了年度体检，医生说一切正常，放心了 🐱",
    "收纳控的快乐：用了无印良品的收纳盒整理了厨房抽屉，强迫症舒适了",
    # 英文
    "Sunday morning routine: fresh pour-over coffee, a good book, and my cat purring on my lap. Life is good ☕📖",
    "Made homemade ramen from scratch today. 12-hour bone broth, chashu pork, soft-boiled eggs. Worth every minute 🍜",
    "Just moved into my new apartment! Spent the whole weekend at IKEA. My bank account hurts but the place looks amazing.",
    "Started a 30-day journaling challenge. Day 1: already realized how much I've been bottling up. This is therapeutic.",
    "Adopted a rescue dog today! His name is Biscuit and he's already claimed the couch as his territory 🐕",
    "Tried the viral baked oats recipe. It actually slaps. Banana, PB, chocolate chips—tastes like brownies for breakfast.",
    "Road trip through the Pacific Northwest 🌲 The forests here make you feel so small in the best way possible.",
    "Hosted my first dinner party in the new place. Homemade pasta and tiramisu. Everyone asked for the recipe!",
    "That feeling when you finish a really good novel and just sit there processing... just finished Klara and the Sun.",
    "Decluttered my entire closet using the KonMari method. Got rid of 40% of my clothes and I don't miss any of it.",
    "Spent the afternoon at a local farmers market. Got fresh sourdough, heirloom tomatoes, and the best honey ever 🍯",
    "Learning to cook Korean food from YouTube. Tonight's attempt: kimchi jjigae. It actually tastes authentic!",
    "Weekend project: built a floating shelf for my plants. Now I have a little indoor jungle corner 🌿",
    "Finally nailed the perfect espresso recipe with my new machine. 18g in, 36g out, 25 seconds. Chef's kiss ☕",
    "Took a pottery class for the first time. Made a very lumpy bowl but I love it. Handmade > perfect any day.",
  ],
  "Sports": [
    # 中文
    "今天跑了10公里，配速5分半，感觉状态不错💪 目标年底完成半马",
    "NBA 季后赛太精彩了！关键时刻三分绝杀看得我从沙发上跳起来了 🏀",
    "健身第100天打卡 🎉 从60kg到75kg，增肌效果很明显，坚持就是胜利",
    "五人制足球2:1赢了！进了一个凌空抽射爽到起飞 ⚽",
    "开始学游泳了，蛙泳25米不换气达成！比想象中累多了但很开心 🏊",
    "法网决赛看了吗？纳达尔在红土场上的统治力依然恐怖，14冠传奇 🎾",
    "第一次尝试攀岩，5.10a的线路爬了20分钟才到顶，手臂累到发抖 🧗",
    "家庭健身计划：俯卧撑+深蹲+平板支撑+哑铃，每天30分钟就够了",
    "滑雪季来了！北大壶上了新缆车，雪质也超好 ⛷️",
    "骑了50公里公路，沿着海边骑感觉太棒了 🚴 下次挑战100公里",
    "世锦赛接力决赛，中国队冲进决赛了！苏炳添带领的这一代太强了 🇨🇳",
    "瑜伽坚持半年，柔韧性进步很大，前弯触地轻松了 🧘",
    "羽毛球反手高远球终于打到位了，教练说姿势进步很大 🏸",
    "Apple Watch Ultra 2，GPS精度和运动追踪确实是最强智能手表",
    "跟跑团一起越野赛，爬了800米累计爬升，但风景值得 🏃",
    "拳击课体验：30分钟沙袋训练就出了一身汗，减压效果一级棒 🥊",
    "这赛季的切尔西终于有点起色了，新教练战术思路很清晰",
    "女排世锦赛中国队表现出色！朱婷的进攻火力全开 🏐",
    "冲浪第三次就能站起来了！浪花拍脸的感觉太爽了 🏄",
    "F1上海站门票买好了！第一次现场看F1超期待 🏎️",
    # 英文
    "Just PR'd my deadlift at 180kg! 6 months of progressive overload finally paid off. Consistency is everything 💪",
    "Marathon training week 8: ran 30km this week total. Legs are tired but the mental clarity from running is addictive 🏃",
    "NBA Playoffs are INSANE this year. That buzzer beater last night had me screaming at 2am. Neighbors definitely hate me.",
    "Started bouldering 2 months ago. Just sent my first V5 problem! This sport is so addictive 🧗‍♂️",
    "Morning gym session: bench press, rows, OHP. Hit a new bench PR at 100kg. Small wins matter 🏋️",
    "Champions League final was absolutely electric! Best match I've watched all season ⚽",
    "Picked up tennis last month. My forehand is decent but backhand needs serious work. Anyone have drill recommendations?",
    "Completed my first triathlon! Swim 1.5km, bike 40km, run 10km. Collapsed at the finish line but worth every second 🏊🚴🏃",
    "Home gym setup complete: power rack, adjustable bench, Olympic bar, plates. Total cost: less than 1 year of gym membership.",
    "Surfing in Bali is a spiritual experience 🏄‍♂️ Caught my longest wave today—rode it for almost 15 seconds!",
    "Just finished a 100-mile ultra marathon. 26 hours of running. My body is destroyed but my mind has never been stronger.",
    "Yoga has completely transformed my flexibility and recovery. Every athlete should incorporate it into their routine 🧘",
    "F1 race was wild! That last-lap overtake was one of the best moves I've ever seen. Pure racing genius 🏎️",
    "Golf is way harder than it looks. Spent 3 hours at the driving range and still can't hit it straight. Respect to pro golfers ⛳",
    "CrossFit competition this weekend. Finished top 10 in my age group. The community support is what makes this sport special.",
  ],
  "News": [
    # 中文
    "SpaceX 星舰第四次试飞成功回收助推器！🚀 人类离火星又近了一步",
    "诺贝尔物理学奖颁给了AI领域的Hinton和Hopfield，深度学习获最高学术认可",
    "全球AI监管峰会召开，28国签署《布莱切利宣言》，AI安全成国际共识",
    "国际油价跌破70美元，新能源汽车销量创新高，能源转型加速中",
    "火星样本返回任务进展：NASA和ESA联合方案确定，预计2033年返回",
    "聚变能源重大突破：ITER项目达成新里程碑，距商业化又近一步",
    "央行降准0.5个百分点，释放长期资金约1万亿，A股午后拉升 📈",
    "嫦娥六号从月球背面采样返回，中国成为首个完成这一壮举的国家 🌙",
    "全球半导体产业链重构：台积电北美厂投产，三星平泽P4产能扩张",
    "世卫组织推荐新一代疟疾疫苗，有望每年挽救数十万儿童生命",
    "碳中和倒计时：欧盟碳关税正式实施，中国碳市场覆盖范围扩大",
    "国际空间站退役方案确定：2030年坠入太平洋",
    "全球人口突破81亿，联合国预测2086年达峰值后下降",
    "量子计算机突破1000比特，纠错能力首次超越物理极限",
    "可控核聚变传捷报：中国环流三号等离子体维持时间突破400秒 ☀️",
    "5G-A 商用正式启动，下行速率达到 10Gbps，元宇宙基础设施就位",
    "全球首个AI法案在欧盟正式生效，高风险AI应用需通过审核才能上市",
    "长征十号火箭首飞成功！载人登月计划迈出关键一步 🚀",
    "印尼迁都计划取得重大进展，新首都努桑塔拉开始接纳政府机关",
    "全球平均气温连续12个月超出历史记录，气候危机警报再升级 🌡️",
    # 英文
    "SpaceX just successfully caught the Super Heavy booster with chopsticks! This changes everything for space logistics 🚀",
    "Nobel Prize in Physics goes to AI pioneers Hinton and Hopfield. The line between physics and CS just got blurrier.",
    "EU AI Act officially in force today. Companies have 2 years to comply. This will reshape the global AI landscape.",
    "Fed cuts rates by 50 basis points—largest cut since 2020. Markets rally as recession fears ease 📈",
    "Fusion energy milestone: ITER achieves first plasma at 150 million degrees. Still decades out but progress is real.",
    "Global EV sales surpass 30% of all new car sales for the first time. The transition is happening faster than expected.",
    "NASA confirms: Mars sample return mission is 2033. We're going to have Mars rocks in our labs within a decade 🔴",
    "OpenAI valued at $150B in latest funding round. The AI gold rush continues.",
    "Climate report: 2024 officially the hottest year on record. Every fraction of a degree matters 🌡️",
    "India's Chandrayaan-4 mission announced: a lunar sample return attempt planned for 2028. Space is getting crowded!",
    "UN report: global population to peak at 10.3 billion in 2084, then decline. Demographics will reshape economies.",
    "Apple announces end of iPhone production in China. Major shift in global supply chain strategy.",
    "World's largest nuclear fusion reactor JT-60SA achieves first plasma in Japan. Fusion future is getting closer.",
    "Quantum computing breakthrough: first error-corrected quantum computation achieved. We're entering a new era.",
    "G20 agrees on global minimum tax of 15%. This could redirect $150B annually to public services worldwide.",
  ],
  "Education": [
    # 中文
    "考研倒计时30天！英语二模拟做了5年真题，平均70分，冲刺 💪",
    "四六级备考方法：每天背50个单词+听BBC+做一套真题，稳过",
    "论文开题通过了！导师说选题不错，接下来就是苦逼的实验阶段了 📝",
    "学习工具推荐：Notion AI 做笔记和知识管理太方便了，学生必备",
    "大三跨考计算机，从零开始学数据结构，408是真的难但不放弃",
    "雅思首考7分！口语6.5是短板，准备二战冲7.5，有口语搭子吗 🗣️",
    "Pomodoro学习法：25分钟专注+5分钟休息，一天能高效学习6小时",
    "MIT OCW 的线性代数课程太棒了，Gilbert Strang 讲得深入浅出",
    "实验室发了第一篇SCI！影响因子3.8，从零到一的突破太开心了 🎉",
    "考公感悟：行测多刷题找规律，申论多读人民日报社论",
    "机器学习课期末项目做了个推荐系统demo，老师给了A",
    "免费学习资源清单：Coursera/edX/Khan Academy/B站，全部免费 📚",
    "保研成功！收到浙大计算机直博offer，三年绩点终于有了回报",
    "旁听了AIGC学术讲座，大模型在教育领域的应用前景很广",
    "GRE备考经验：verbal 狂背单词，quant 对中国学生来说不难",
    "终于搞懂了反向传播算法！手推了一遍梯度下降，数学真的很美",
    "分享我的大学四年书单：每年读30本书，非虚构类书籍让我受益最大",
    "编译原理课太难了但也太有趣了，写了一个简单的词法分析器",
    "参加了ACM校赛，暴力算法只过了3题，明年一定要学好动态规划",
    "毕业论文查重率5.2%！低于学校要求的15%，可以放心答辩了",
    # 英文
    "Just completed Andrew Ng's Machine Learning Specialization on Coursera. Best intro to ML out there, hands down.",
    "PhD life: spent 14 hours debugging code only to find a missing comma. This is fine. Everything is fine. 🔬",
    "The free resources available for learning CS are incredible. Harvard CS50, MIT OCW, freeCodeCamp—no excuses not to learn.",
    "GRE score came back: 330 (170Q, 160V). The math section is definitely easier for international students.",
    "Published my first paper! Impact factor 3.8. Two years of work condensed into 12 pages. Academia is wild.",
    "Hot take: MOOCs are better than most university lectures. Self-paced, expert instructors, and often free.",
    "Learning math proofs is like solving puzzles—frustrating until it clicks, then incredibly satisfying 🧮",
    "Study tip that changed my life: the Feynman Technique. If you can't explain it simply, you don't understand it.",
    "Just got accepted into my dream graduate program! All those late-night study sessions were worth it 🎓",
    "The gap between industry and academia in CS is widening. We need more collaboration, not competition.",
    "Built a mini compiler for my PL course. Lexer, parser, AST, code gen. Now I truly appreciate how complex languages are.",
    "Attending a conference on AI in education. The potential for personalized learning at scale is enormous.",
    "Finished my thesis with a 5% plagiarism score. Time to prepare for the defense. Wish me luck! 📝",
    "Started a study group for algorithms. Teaching others is the best way to solidify your own understanding.",
    "Khan Academy just released new AP courses. Sal Khan is genuinely making education accessible worldwide.",
  ],
}

# 新增英文标签
TAGS_TO_CREATE = [
    "AI", "编程", "科技", "美食", "旅行", "健身", "篮球", "足球",
    "考研", "学习", "航天", "经济", "生活",
    # 新增
    "Programming", "MachineLearning", "Startup", "Fitness", "Running",
    "Cooking", "Travel", "Photography", "Music", "Gaming",
    "MentalHealth", "Productivity", "Finance", "SpaceExploration",
    "Climate", "OpenSource", "Design", "DataScience", "Blockchain", "Education"
]

# 标签匹配规则 (关键词 -> 标签名)
TAG_RULES = {
    "AI":               ["AI", "人工智能", "GPT", "ChatGPT", "大模型", "深度学习", "机器学习", "LLM", "Copilot", "Claude", "Gemini", "Sora"],
    "编程":             ["Python", "编程", "代码", "开发", "GitHub", "Rust", "React", "JavaScript", "TypeScript", "Docker", "K8s"],
    "Programming":      ["code", "coding", "deploy", "developer", "programming", "compiler", "debugging", "API", "frontend", "backend", "microservice"],
    "科技":             ["科技", "芯片", "Apple", "华为", "iPhone", "MacBook", "Vision Pro", "Chrome"],
    "MachineLearning":  ["machine learning", "ML", "neural", "training", "model", "deep learning", "transformer", "RAG"],
    "美食":             ["美食", "咖啡", "蛋糕", "火锅", "早餐", "烘焙", "食谱", "提拉米苏"],
    "Cooking":          ["cook", "recipe", "ramen", "pasta", "coffee", "espresso", "bake"],
    "旅行":             ["旅行", "打卡", "迪士尼", "西湖", "民宿"],
    "Travel":           ["trip", "travel", "road trip", "explore", "Bali", "destination"],
    "健身":             ["健身", "运动", "瑜伽", "游泳", "增肌"],
    "Fitness":          ["gym", "workout", "deadlift", "bench press", "CrossFit", "fitness"],
    "Running":          ["跑步", "马拉松", "running", "marathon", "triathlon", "ultra"],
    "篮球":             ["NBA", "篮球", "basketball"],
    "足球":             ["足球", "世界杯", "football", "soccer", "Champions League", "Premier League"],
    "考研":             ["考研", "考公", "雅思", "GRE", "四六级"],
    "学习":             ["学习", "课程", "论文", "MIT", "Coursera", "study", "learning"],
    "Education":        ["education", "university", "PhD", "thesis", "paper", "course", "student", "graduate"],
    "航天":             ["SpaceX", "NASA", "火星", "嫦娥", "星舰", "fusion", "lunar", "rocket"],
    "SpaceExploration": ["space", "Mars", "moon", "rocket", "orbit", "spacecraft"],
    "经济":             ["央行", "A股", "油价", "GDP", "Fed", "rate", "market", "economy", "funding", "valuation"],
    "Finance":          ["finance", "invest", "stock", "tax", "revenue", "billion"],
    "生活":             ["生活", "日常", "周末", "手账", "收纳", "断舍离"],
    "OpenSource":       ["open source", "OSS", "stars", "repo", "开源"],
    "Climate":          ["climate", "carbon", "temperature", "气候", "碳中和", "nuclear"],
    "Design":           ["Figma", "design", "UI", "UX", "设计"],
    "DataScience":      ["data", "pandas", "visualization", "analytics", "数据"],
    "Productivity":     ["productivity", "Notion", "效率", "routine", "Pomodoro"],
    "Music":            ["吉他", "guitar", "music", "song"],
    "Photography":      ["photo", "camera", "摄影"],
    "MentalHealth":     ["mental", "therapy", "meditation", "冥想", "journal"],
    "Startup":          ["startup", "founder", "创业", "product"],
    "Gaming":           ["game", "gaming", "esports"],
}

# ===================================================================
# 用户画像定义：给每个用户分配 1-2 个偏好分类
# 这是推荐算法的核心信号来源！
# ===================================================================
USER_PROFILES = {}  # user_id -> {"primary": category, "secondary": category}

def build_user_profiles(user_ids):
    """给每个用户分配偏好分类"""
    categories = ["Tech", "Life", "Sports", "News", "Education"]
    for uid in user_ids:
        primary = random.choice(categories)
        secondary = random.choice([c for c in categories if c != primary])
        USER_PROFILES[uid] = {"primary": primary, "secondary": secondary}


def extract_tags(text):
    """从文本中提取标签"""
    found = set()
    text_lower = text.lower()
    for tag_name, keywords in TAG_RULES.items():
        for kw in keywords:
            if kw.lower() in text_lower:
                found.add(tag_name)
                break
    return list(found) if found else []


def main():
    print("=" * 55)
    print("  推荐系统内容导入工具 v2 (中英双语 · 推荐适配)")
    print("=" * 55)

    if not DB_CONFIG['password']:
        raise RuntimeError("DB_PASSWORD environment variable is required")

    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    # 1. 获取用户列表
    cursor.execute("SELECT id FROM users")
    user_ids = [row[0] for row in cursor.fetchall()]
    print(f"\n👥 找到 {len(user_ids)} 个用户")

    build_user_profiles(user_ids)

    # 2. 确保所有标签存在
    tag_cache = {}
    cursor.execute("SELECT id, name FROM tb_tag")
    for row in cursor.fetchall():
        tag_cache[row[1]] = row[0]

    for tag_name in TAGS_TO_CREATE:
        if tag_name not in tag_cache:
            cursor.execute(
                "INSERT INTO tb_tag (name) VALUES (%s) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
                (tag_name,)
            )
            tag_cache[tag_name] = cursor.lastrowid
    conn.commit()
    print(f"🏷️  标签总数: {len(tag_cache)} 个")

    # 3. 插入帖文 (时间均匀分布在过去 30 天)
    all_posts = []
    for category, texts in POSTS.items():
        for text in texts:
            all_posts.append({"text": text, "category": category})
    random.shuffle(all_posts)

    now = datetime.now()
    total_posts = len(all_posts)
    inserted = 0
    content_ids_by_cat = {cat: [] for cat in POSTS.keys()}  # 按分类记录新帖ID

    for i, post in enumerate(all_posts):
        # 均匀时间分布：把 30 天 × 24 小时均匀分给所有帖子
        hours_offset = (i / total_posts) * 30 * 24  # 均匀分布
        hours_offset += random.uniform(-6, 6)  # 加一点随机抖动
        created_at = now - timedelta(hours=max(0, hours_offset))

        # 从该分类偏好的用户中选发布者（80%概率）
        category = post["category"]
        preferred_users = [uid for uid, p in USER_PROFILES.items() if p["primary"] == category]
        if preferred_users and random.random() < 0.8:
            author_id = random.choice(preferred_users)
        else:
            author_id = random.choice(user_ids)

        title = post["text"][:25].rstrip() + "..."

        # 互动量
        pop = random.choices(["low", "mid", "high", "viral"], weights=[30, 40, 20, 10])[0]
        if pop == "low":
            likes, comments, reposts, views = random.randint(1, 15), random.randint(0, 3), random.randint(0, 2), random.randint(20, 300)
        elif pop == "mid":
            likes, comments, reposts, views = random.randint(15, 150), random.randint(3, 25), random.randint(2, 15), random.randint(300, 3000)
        elif pop == "high":
            likes, comments, reposts, views = random.randint(150, 1500), random.randint(25, 150), random.randint(15, 100), random.randint(3000, 30000)
        else:
            likes, comments, reposts, views = random.randint(1500, 15000), random.randint(150, 800), random.randint(100, 600), random.randint(30000, 200000)

        try:
            cursor.execute("""
                INSERT INTO contents (title, content, category, author_id,
                    like_count, comment_count, repost_count, view_count, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (title, post["text"], category, author_id,
                  likes, comments, reposts, views,
                  created_at.strftime("%Y-%m-%d %H:%M:%S")))
            cid = cursor.lastrowid
            content_ids_by_cat[category].append(cid)
            inserted += 1

            # 关联标签
            tags = extract_tags(post["text"])
            for t in tags:
                if t in tag_cache:
                    cursor.execute(
                        "INSERT IGNORE INTO content_tags (content_id, tag_id) VALUES (%s, %s)",
                        (cid, tag_cache[t])
                    )
        except Exception as e:
            print(f"  ⚠️ {e}")

    conn.commit()
    print(f"📄 导入帖文: {inserted} 条")

    # 4. 生成行为数据 (核心：按用户画像偏好倾斜)
    print("\n🔄 生成画像驱动的行为数据...")
    behavior_count = 0
    all_new_ids = []
    for ids in content_ids_by_cat.values():
        all_new_ids.extend(ids)

    # 获取帖子-分类映射
    if all_new_ids:
        placeholders = ','.join(['%s'] * len(all_new_ids))
        cursor.execute(f"SELECT id, category FROM contents WHERE id IN ({placeholders})", all_new_ids)
        content_cat_map = {row[0]: row[1] for row in cursor.fetchall()}
    else:
        content_cat_map = {}

    for uid in user_ids:
        profile = USER_PROFILES[uid]
        primary_cat = profile["primary"]
        secondary_cat = profile["secondary"]

        # 每个用户产生 15-40 条行为
        num_actions = random.randint(15, 40)

        for _ in range(num_actions):
            # 选择目标帖子：60% 偏好分类，25% 次偏好，15% 随机
            roll = random.random()
            if roll < 0.60 and content_ids_by_cat.get(primary_cat):
                cid = random.choice(content_ids_by_cat[primary_cat])
            elif roll < 0.85 and content_ids_by_cat.get(secondary_cat):
                cid = random.choice(content_ids_by_cat[secondary_cat])
            else:
                cid = random.choice(all_new_ids) if all_new_ids else None

            if cid is None:
                continue

            # 行为类型分布
            action = random.choices(
                ["VIEW", "LIKE", "COMMENT", "REPOST"],
                weights=[40, 35, 18, 7], k=1
            )[0]

            # 偏好分类的帖子更容易产生深度行为
            content_category = content_cat_map.get(cid)
            if content_category == primary_cat and action == "VIEW":
                # 偏好内容有 50% 概率升级为 LIKE
                if random.random() < 0.5:
                    action = "LIKE"

            hours_ago = random.uniform(0, 720)  # 过去 30 天
            action_time = now - timedelta(hours=hours_ago)

            try:
                cursor.execute(
                    "INSERT INTO behaviors (user_id, content_id, type, created_at) VALUES (%s, %s, %s, %s)",
                    (uid, cid, action, action_time.strftime("%Y-%m-%d %H:%M:%S"))
                )
                behavior_count += 1
            except:
                pass

    conn.commit()
    print(f"🎯 行为记录: {behavior_count} 条")

    # 5. 打印画像分布统计
    print("\n📊 用户画像偏好分布:")
    from collections import Counter
    primary_dist = Counter(p["primary"] for p in USER_PROFILES.values())
    for cat, cnt in sorted(primary_dist.items()):
        print(f"   {cat}: {cnt} 个用户主偏好")

    # 6. 总量统计
    cursor.execute("SELECT COUNT(*) FROM contents")
    total = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM behaviors")
    total_behaviors = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM tb_tag")
    total_tags = cursor.fetchone()[0]

    print(f"\n{'=' * 55}")
    print(f"  ✅ 完成！数据库总量:")
    print(f"     📄 帖文: {total} 条")
    print(f"     🏷️  标签: {total_tags} 个")
    print(f"     🎯 行为: {total_behaviors} 条")
    print(f"{'=' * 55}")

    cursor.close()
    conn.close()


if __name__ == "__main__":
    main()
