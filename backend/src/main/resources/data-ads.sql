-- Phase 29: 模拟广告种子数据
-- 10 条广告，覆盖 Tech/Life/Education/Sports/Finance 各 2 条

INSERT INTO ads (title, description, image_url, target_url, advertiser, target_tags, category, bid_price, impression_count, click_count, active, created_at) VALUES
-- Tech 类
('ChatGPT Pro 限时优惠', '解锁 GPT-4o 全部能力，助力你的工作效率提升 10 倍。', 'https://placehold.co/600x300/1DA1F2/white?text=ChatGPT+Pro', 'https://chat.openai.com', 'OpenAI', 'Tech,AI,Programming', 'Tech', 3.5, 1200, 48, true, CURRENT_TIMESTAMP),
('GitHub Copilot 企业版', 'AI 编程助手，让你的团队编码速度提升 55%。免费试用 30 天。', 'https://placehold.co/600x300/24292e/white?text=GitHub+Copilot', 'https://github.com/features/copilot', 'GitHub', 'Tech,Programming,AI', 'Tech', 4.2, 800, 56, true, CURRENT_TIMESTAMP),

-- Life 类
('星巴克春季新品 ☕', '樱花拿铁限定回归，第二杯半价。到店即享优惠。', 'https://placehold.co/600x300/00704A/white?text=Starbucks+Spring', 'https://www.starbucks.com', 'Starbucks', 'Life,Food,Daily', 'Life', 2.0, 2000, 120, true, CURRENT_TIMESTAMP),
('Dyson V15 无线吸尘器', '激光探测隐藏灰尘，智能调节吸力。限时直降 800 元。', 'https://placehold.co/600x300/6C2D82/white?text=Dyson+V15', 'https://www.dyson.com', 'Dyson', 'Life,Tech,Home', 'Life', 5.0, 600, 18, true, CURRENT_TIMESTAMP),

-- Education 类
('Coursera Plus 年度会员', '7000+ 门课程无限学习，Google/Meta 专业证书一键解锁。', 'https://placehold.co/600x300/0056D2/white?text=Coursera+Plus', 'https://www.coursera.org', 'Coursera', 'Education,Tech,Career', 'Education', 2.8, 1500, 90, true, CURRENT_TIMESTAMP),
('Duolingo 超级会员', '零广告学外语，AI 实时对话练习。首月仅 1 元。', 'https://placehold.co/600x300/58CC02/white?text=Duolingo+Super', 'https://www.duolingo.com', 'Duolingo', 'Education,Language,Daily', 'Education', 1.5, 3000, 210, true, CURRENT_TIMESTAMP),

-- Sports 类
('Nike Air Max Dn', '全新气垫科技，跑步体验再升级。新品首发 ¥1299。', 'https://placehold.co/600x300/111111/white?text=Nike+Air+Max', 'https://www.nike.com', 'Nike', 'Sports,Fashion,Running', 'Sports', 3.0, 900, 36, true, CURRENT_TIMESTAMP),
('Keep 智能跑步机', '家庭健身新选择，千人千面训练计划。限时免息分期。', 'https://placehold.co/600x300/2CCC6E/white?text=Keep+Treadmill', 'https://www.keep.com', 'Keep', 'Sports,Health,Fitness', 'Sports', 2.5, 700, 28, true, CURRENT_TIMESTAMP),

-- Finance 类
('招商银行信用卡', '新户开卡享 200 元刷卡金，每周五外卖立减 10 元。', 'https://placehold.co/600x300/C41230/white?text=CMB+Credit+Card', 'https://www.cmbchina.com', 'CMB', 'Finance,Daily,Life', 'Finance', 6.0, 500, 15, true, CURRENT_TIMESTAMP),
('支付宝理财精选', '稳健型基金年化 4.5%+，新人专享万元体验金。', 'https://placehold.co/600x300/1677FF/white?text=Alipay+Finance', 'https://www.alipay.com', 'Alipay', 'Finance,Investment,Tech', 'Finance', 4.0, 400, 20, true, CURRENT_TIMESTAMP);
