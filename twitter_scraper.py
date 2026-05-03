"""
Twitter/X 数据采集脚本
使用 twscrape 库，支持中英文关键词，每个关键词采集约50条
"""

import asyncio
import pandas as pd
import os
import time
from datetime import datetime

# ========== 安装依赖 ==========
# pip install twscrape pandas openpyxl

# ========== 配置区域（只需修改这里）==========

# 你的 X 账号（普通账号即可，不需要开发者权限）
ACCOUNTS = [
    {
        "username": "TheEnd",
        "password": "Text@123456",
        "email": "TheEnd123456789@proton.me",
        "email_password": "Text@123456"  # 可留空 ""
    },
    # 可添加多个账号，避免单账号被限速
]

# 搜索关键词（中英文混合）
KEYWORDS = [
    # === 英文关键词 ===
    "artificial intelligence",
    "ChatGPT",
    "climate change",
    "Ukraine war",
    "electric vehicle",

    # === 中文关键词 ===
    "人工智能",
    "气候变化",
    "新能源汽车",
    "ChatGPT",
    "经济复苏",
]

# 每个关键词采集数量
TWEETS_PER_KEYWORD = 50

# 时间范围（可选，格式 YYYY-MM-DD，留空则不限制）
DATE_SINCE = "2024-01-01"
DATE_UNTIL = ""  # 留空表示到现在

# 输出文件名
OUTPUT_FILE = f"tweets_{datetime.now().strftime('%Y%m%d_%H%M')}.xlsx"

# ========== 主程序 ==========

async def scrape():
    from twscrape import API, gather
    from twscrape.logger import set_log_level
    set_log_level("ERROR")

    api = API()

    # 添加账号
    print("🔐 正在登录账号...")
    for acc in ACCOUNTS:
        await api.pool.add_account(
            username=acc["username"],
            password=acc["password"],
            email=acc["email"],
            email_password=acc.get("email_password", "")
        )
    await api.pool.login_all()
    print("✅ 登录成功\n")

    all_tweets = []

    for keyword in KEYWORDS:
        print(f"🔍 正在采集关键词：【{keyword}】")

        # 构建搜索语句
        query = keyword
        if DATE_SINCE:
            query += f" since:{DATE_SINCE}"
        if DATE_UNTIL:
            query += f" until:{DATE_UNTIL}"
        query += " -filter:retweets"  # 过滤转发，只要原创

        count = 0
        try:
            async for tweet in api.search(query, limit=TWEETS_PER_KEYWORD):
                all_tweets.append({
                    "keyword":      keyword,
                    "tweet_id":     tweet.id,
                    "date":         tweet.date.strftime("%Y-%m-%d %H:%M:%S"),
                    "username":     tweet.user.username,
                    "display_name": tweet.user.displayname,
                    "followers":    tweet.user.followersCount,
                    "content":      tweet.rawContent,
                    "lang":         tweet.lang,
                    "likes":        tweet.likeCount,
                    "retweets":     tweet.retweetCount,
                    "replies":      tweet.replyCount,
                    "views":        tweet.viewCount or 0,
                    "url":          f"https://x.com/{tweet.user.username}/status/{tweet.id}"
                })
                count += 1

            print(f"   ✅ 采集到 {count} 条\n")

        except Exception as e:
            print(f"   ❌ 出错：{e}\n")

        # 每个关键词间隔，避免被限速
        await asyncio.sleep(3)

    return all_tweets


def save_results(tweets):
    if not tweets:
        print("❌ 没有采集到任何数据")
        return

    df = pd.DataFrame(tweets)

    # 去重（同一条推文可能被多个关键词搜到）
    df_dedup = df.drop_duplicates(subset=["tweet_id"])

    print(f"\n📊 采集结果统计：")
    print(f"   总条数（含重复）：{len(df)}")
    print(f"   去重后条数：{len(df_dedup)}")
    print(f"   覆盖关键词：{df['keyword'].nunique()} 个")
    print(f"   语言分布：\n{df_dedup['lang'].value_counts().to_string()}")

    # 保存到 Excel（多个 Sheet）
    with pd.ExcelWriter(OUTPUT_FILE, engine="openpyxl") as writer:
        # Sheet1：所有数据（去重）
        df_dedup.to_excel(writer, sheet_name="全部数据", index=False)

        # Sheet2：按关键词分组统计
        summary = df.groupby("keyword").agg(
            条数=("tweet_id", "count"),
            平均点赞=("likes", "mean"),
            平均转发=("retweets", "mean"),
            平均回复=("replies", "mean"),
        ).round(1)
        summary.to_excel(writer, sheet_name="关键词统计")

        # Sheet3：每个关键词单独一个Sheet
        for kw in df["keyword"].unique():
            sheet_name = kw[:28]  # Excel Sheet名最长31字符
            df[df["keyword"] == kw].to_excel(
                writer, sheet_name=sheet_name, index=False
            )

    print(f"\n✅ 数据已保存：{OUTPUT_FILE}")
    print(f"   打开文件查看 {len(df_dedup)} 条推文数据\n")


# ========== 运行 ==========

if __name__ == "__main__":
    print("=" * 50)
    print("  Twitter/X 数据采集工具  ")
    print("=" * 50 + "\n")

    tweets = asyncio.run(scrape())
    save_results(tweets)
