from pathlib import Path
import math

from PIL import Image, ImageDraw, ImageFont


OUT_DIR = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关\第4章流程图")


def font_path(*names):
    for name in names:
        path = Path(r"C:\Windows\Fonts") / name
        if path.exists():
            return str(path)
    return None


FONT_HEI = font_path("simhei.ttf", "msyh.ttc", "simsun.ttc")
FONT_SONG = font_path("simsun.ttc", "msyh.ttc", "simhei.ttf")


def load_font(size, bold=False):
    return ImageFont.truetype(FONT_HEI if bold else FONT_SONG, size)


TITLE_FONT = load_font(48, True)
NODE_FONT = load_font(34)
NODE_FONT_BOLD = load_font(34, True)
SMALL_FONT = load_font(28)


class FlowCanvas:
    def __init__(self, title, width=1800, height=2100):
        self.width = width
        self.height = height
        self.img = Image.new("RGB", (width, height), "white")
        self.draw = ImageDraw.Draw(self.img)
        self.nodes = {}
        self.title = title
        self.draw_title()

    def draw_title(self):
        x1, y1, x2, y2 = self.draw.textbbox((0, 0), self.title, font=TITLE_FONT)
        self.draw.text(((self.width - (x2 - x1)) / 2, 55), self.title, fill="#111827", font=TITLE_FONT)

    def wrap_text(self, text, font, max_width):
        lines = []
        for para in text.split("\n"):
            current = ""
            for ch in para:
                candidate = current + ch
                bbox = self.draw.textbbox((0, 0), candidate, font=font)
                if bbox[2] - bbox[0] <= max_width:
                    current = candidate
                else:
                    if current:
                        lines.append(current)
                    current = ch
            if current:
                lines.append(current)
        return lines

    def text_center(self, cx, cy, text, max_width, font=NODE_FONT, fill="#111827"):
        lines = self.wrap_text(text, font, max_width)
        line_h = font.size + 8
        total_h = line_h * len(lines)
        y = cy - total_h / 2
        for line in lines:
            bbox = self.draw.textbbox((0, 0), line, font=font)
            self.draw.text((cx - (bbox[2] - bbox[0]) / 2, y), line, fill=fill, font=font)
            y += line_h

    def box(self, key, cx, cy, w, h, text, bold=False):
        x1, y1, x2, y2 = cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2
        self.draw.rounded_rectangle((x1, y1, x2, y2), radius=16, fill="#F9FAFB", outline="#111827", width=4)
        self.text_center(cx, cy, text, w - 38, NODE_FONT_BOLD if bold else NODE_FONT)
        self.nodes[key] = {"shape": "box", "cx": cx, "cy": cy, "w": w, "h": h, "bbox": (x1, y1, x2, y2)}

    def oval(self, key, cx, cy, w, h, text):
        x1, y1, x2, y2 = cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2
        self.draw.ellipse((x1, y1, x2, y2), fill="#FFFFFF", outline="#111827", width=4)
        self.text_center(cx, cy, text, w - 38, NODE_FONT_BOLD)
        self.nodes[key] = {"shape": "oval", "cx": cx, "cy": cy, "w": w, "h": h, "bbox": (x1, y1, x2, y2)}

    def diamond(self, key, cx, cy, w, h, text):
        pts = [(cx, cy - h / 2), (cx + w / 2, cy), (cx, cy + h / 2), (cx - w / 2, cy)]
        self.draw.polygon(pts, fill="#FFFFFF", outline="#111827")
        self.draw.line(pts + [pts[0]], fill="#111827", width=4)
        self.text_center(cx, cy, text, w * 0.62, NODE_FONT)
        self.nodes[key] = {"shape": "diamond", "cx": cx, "cy": cy, "w": w, "h": h, "bbox": (cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)}

    def anchor(self, key, side):
        n = self.nodes[key]
        cx, cy, w, h = n["cx"], n["cy"], n["w"], n["h"]
        if side == "top":
            return (cx, cy - h / 2)
        if side == "bottom":
            return (cx, cy + h / 2)
        if side == "left":
            return (cx - w / 2, cy)
        if side == "right":
            return (cx + w / 2, cy)
        return (cx, cy)

    def arrow_head(self, p1, p2, size=18):
        angle = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
        left = (p2[0] - size * math.cos(angle - math.pi / 6), p2[1] - size * math.sin(angle - math.pi / 6))
        right = (p2[0] - size * math.cos(angle + math.pi / 6), p2[1] - size * math.sin(angle + math.pi / 6))
        self.draw.polygon([p2, left, right], fill="#111827")

    def line_arrow(self, points, label=None, label_pos=0.5):
        pts = [(int(x), int(y)) for x, y in points]
        self.draw.line(pts, fill="#111827", width=4, joint="curve")
        self.arrow_head(pts[-2], pts[-1])
        if label:
            idx = max(0, min(len(pts) - 2, int((len(pts) - 1) * label_pos)))
            x = (pts[idx][0] + pts[idx + 1][0]) / 2
            y = (pts[idx][1] + pts[idx + 1][1]) / 2
            bbox = self.draw.textbbox((0, 0), label, font=SMALL_FONT)
            pad = 8
            self.draw.rectangle((x - (bbox[2] - bbox[0]) / 2 - pad, y - 23, x + (bbox[2] - bbox[0]) / 2 + pad, y + 18), fill="white")
            self.draw.text((x - (bbox[2] - bbox[0]) / 2, y - 19), label, fill="#111827", font=SMALL_FONT)

    def connect(self, src, dst, src_side="bottom", dst_side="top", label=None, via=None):
        p1 = self.anchor(src, src_side)
        p2 = self.anchor(dst, dst_side)
        points = [p1]
        if via:
            points.extend(via)
        points.append(p2)
        self.line_arrow(points, label=label)

    def save(self, filename):
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        path = OUT_DIR / filename
        self.img.save(path, quality=95)
        return path


def chart_4_1():
    c = FlowCanvas("用户认证与内容互动模块程序流程图")
    x = 900
    c.oval("start", x, 170, 330, 90, "开始")
    c.box("visit", x, 310, 520, 110, "用户访问登录页或首页")
    c.diamond("logged", x, 485, 420, 160, "是否已登录")
    c.box("login", 430, 660, 470, 130, "提交注册/登录信息\nAuthController")
    c.box("verify", 430, 840, 470, 130, "校验账号、密码、角色\n生成 Token")
    c.box("home", x, 840, 520, 130, "进入首页信息流\n加载用户信息")
    c.box("compose", x, 1030, 520, 130, "发布正文或上传图片\nContentController")
    c.diamond("image", x, 1220, 420, 150, "是否包含图片")
    c.box("upload", 430, 1400, 470, 125, "FileUploadController\n保存图片并返回 URL")
    c.box("save", x, 1400, 520, 125, "ContentService 保存内容\n解析话题标签")
    c.box("interact", x, 1585, 520, 130, "点赞、评论、转发、关注\nBehaviorService")
    c.box("notice", x, 1775, 560, 130, "更新计数并生成通知/私信\nWebSocket 推送")
    c.oval("end", x, 1960, 330, 90, "结束")
    c.connect("start", "visit")
    c.connect("visit", "logged")
    c.connect("logged", "login", "left", "top", "否", via=[(430, 485)])
    c.connect("login", "verify")
    c.connect("verify", "home", "right", "left", via=[(670, 840)])
    c.connect("logged", "home", label="是")
    c.connect("home", "compose")
    c.connect("compose", "image")
    c.connect("image", "upload", "left", "top", "是", via=[(430, 1220)])
    c.connect("upload", "save", "right", "left", via=[(665, 1400)])
    c.connect("image", "save", label="否")
    c.connect("save", "interact")
    c.connect("interact", "notice")
    c.connect("notice", "end")
    return c.save("图4-1_用户认证与内容互动模块程序流程图.png")


def chart_4_2():
    c = FlowCanvas("推荐排序与用户画像模块程序流程图", height=2250)
    x = 900
    c.oval("start", x, 170, 330, 90, "开始")
    c.box("request", x, 315, 560, 120, "用户请求 For You 推荐流\n或打开用户画像页")
    c.box("behavior", x, 500, 560, 125, "读取 Behavior 行为记录\nVIEW / LIKE / COMMENT / SEARCH")
    c.box("weight", x, 690, 560, 125, "按行为类型和时间衰减赋权\n得到近期兴趣信号")
    c.box("profile", x, 880, 560, 130, "聚合兴趣标签、作者偏好\n文本偏好和新鲜度偏好")
    c.box("candidate", x, 1075, 560, 130, "构建候选池\n关注内容、全站内容、热门内容")
    c.box("score", x, 1280, 620, 145, "混合推荐打分\n标签匹配、TF-IDF、协同过滤、热门话题")
    c.box("filter", x, 1490, 620, 145, "负反馈过滤与多样性重排\n作者去重率、标签覆盖率")
    c.box("result", x, 1700, 560, 130, "生成推荐列表和评分拆分\nContentWithScore")
    c.box("view", x, 1895, 560, 130, "前端展示信息流、画像词云\n推荐对比指标")
    c.oval("end", x, 2090, 330, 90, "结束")
    for a, b in [
        ("start", "request"),
        ("request", "behavior"),
        ("behavior", "weight"),
        ("weight", "profile"),
        ("profile", "candidate"),
        ("candidate", "score"),
        ("score", "filter"),
        ("filter", "result"),
        ("result", "view"),
        ("view", "end"),
    ]:
        c.connect(a, b)
    return c.save("图4-2_推荐排序与用户画像模块程序流程图.png")


def chart_4_3():
    c = FlowCanvas("AI推荐与后台管理模块程序流程图", height=2250)
    x = 900
    c.oval("start", x, 170, 330, 90, "开始")
    c.box("admin", x, 315, 560, 120, "管理员进入 AdminView\n查询策略和 Ollama 状态")
    c.diamond("action", x, 510, 460, 165, "选择后台操作")
    c.box("strategy", 430, 710, 470, 125, "切换推荐策略\ntraditional / ai")
    c.box("tagging", 1370, 710, 470, 125, "执行 AI 批量打标\nAiTaggingService")
    c.box("save_strategy", 430, 900, 470, 125, "保存策略配置\nRecommendationStrategyManager")
    c.box("save_tags", 1370, 900, 470, 125, "调用模型生成标签\n写入 content_tags")
    c.box("request", x, 1095, 560, 125, "用户请求 AI 推荐流\nAiRecommendationStrategy")
    c.diamond("cache", x, 1290, 420, 155, "缓存是否命中")
    c.box("return_cache", 430, 1480, 470, 125, "返回缓存排序结果\n和推荐理由")
    c.box("call_ai", 1370, 1480, 470, 125, "构造候选摘要\n调用 Ollama / AiService")
    c.diamond("success", 1370, 1685, 420, 155, "模型调用是否成功")
    c.box("parse", 1370, 1885, 470, 125, "解析排序结果\n写入缓存并返回")
    c.box("fallback", 430, 1885, 470, 125, "超时或异常\n回退传统混合推荐")
    c.oval("end", x, 2090, 330, 90, "结束")
    c.connect("start", "admin")
    c.connect("admin", "action")
    c.connect("action", "strategy", "left", "top", "策略切换", via=[(430, 510)])
    c.connect("action", "tagging", "right", "top", "AI打标", via=[(1370, 510)])
    c.connect("strategy", "save_strategy")
    c.connect("tagging", "save_tags")
    c.connect("save_strategy", "request", "right", "left", via=[(665, 1095)])
    c.connect("save_tags", "request", "left", "right", via=[(1135, 1095)])
    c.connect("request", "cache")
    c.connect("cache", "return_cache", "left", "top", "是", via=[(430, 1290)])
    c.connect("cache", "call_ai", "right", "top", "否", via=[(1370, 1290)])
    c.connect("call_ai", "success")
    c.connect("success", "parse", label="是")
    c.connect("success", "fallback", "left", "top", "否", via=[(430, 1685)])
    c.connect("return_cache", "end", "right", "left", via=[(650, 2090)])
    c.connect("parse", "end", "left", "right", via=[(1150, 2090)])
    c.connect("fallback", "end", "right", "left", via=[(650, 2090)])
    return c.save("图4-3_AI推荐与后台管理模块程序流程图.png")


def chart_4_4():
    c = FlowCanvas("广告分发模块程序流程图")
    x = 900
    c.oval("start", x, 170, 330, 90, "开始")
    c.box("feed", x, 320, 560, 120, "用户请求首页信息流\nAdService 参与组装")
    c.box("config", x, 510, 560, 125, "读取启用广告配置\nfrequency / maxAdsPerPage")
    c.box("profile", x, 700, 560, 125, "读取用户画像标签\n和候选广告 targetTags")
    c.box("match", x, 895, 560, 130, "计算画像与广告标签匹配度\n生成广告候选列表")
    c.diamond("available", x, 1090, 430, 155, "是否存在可投放广告")
    c.box("normal", 430, 1280, 470, 125, "无匹配广告\n返回普通信息流")
    c.box("insert", 1370, 1280, 470, 125, "按间隔插入广告卡片\n合并内容与广告")
    c.box("render", x, 1475, 560, 125, "前端渲染 AdCard\n记录曝光次数")
    c.diamond("click", x, 1665, 430, 155, "用户是否点击广告")
    c.box("clicklog", 1370, 1850, 470, 125, "记录点击次数\n更新 CTR 和统计结果")
    c.oval("end", 430, 1850, 330, 90, "结束浏览")
    c.connect("start", "feed")
    c.connect("feed", "config")
    c.connect("config", "profile")
    c.connect("profile", "match")
    c.connect("match", "available")
    c.connect("available", "normal", "left", "top", "否", via=[(430, 1090)])
    c.connect("available", "insert", "right", "top", "是", via=[(1370, 1090)])
    c.connect("normal", "render", "right", "left", via=[(665, 1475)])
    c.connect("insert", "render", "left", "right", via=[(1135, 1475)])
    c.connect("render", "click")
    c.connect("click", "end", "left", "top", "否", via=[(430, 1665)])
    c.connect("click", "clicklog", "right", "top", "是", via=[(1370, 1665)])
    c.connect("clicklog", "end", "left", "right", via=[(1135, 1850)])
    return c.save("图4-4_广告分发模块程序流程图.png")


def chart_4_5():
    c = FlowCanvas("数据统计与画像可视化模块程序流程图", height=2250)
    x = 900
    c.oval("start", x, 170, 330, 90, "开始")
    c.box("open", x, 315, 560, 120, "打开 AnalyticsView\nPersonaDetailCard 或 CompareView")
    c.box("request", x, 500, 560, 125, "前端请求统计接口\nAnalytics / Persona / Compare")
    c.box("read", x, 690, 560, 125, "读取用户、内容、行为\n标签、广告和推荐结果")
    c.diamond("range", x, 885, 430, 155, "是否选择时间范围")
    c.box("filter", 430, 1070, 470, 125, "按今日、7天、30天\n或全部范围过滤")
    c.box("all", 1370, 1070, 470, 125, "使用默认统计范围\n聚合全量数据")
    c.box("aggregate", x, 1265, 620, 140, "聚合统计指标\n行为分布、标签分布、内容趋势、兴趣序列")
    c.box("dto", x, 1470, 560, 125, "生成 DTO 数据\n返回前端页面")
    c.box("charts", x, 1665, 620, 140, "ECharts、词云、雷达图\n折线图和指标卡片渲染")
    c.diamond("refresh", x, 1875, 430, 155, "是否切换筛选条件")
    c.oval("end", x, 2090, 330, 90, "结束")
    c.connect("start", "open")
    c.connect("open", "request")
    c.connect("request", "read")
    c.connect("read", "range")
    c.connect("range", "filter", "left", "top", "是", via=[(430, 885)])
    c.connect("range", "all", "right", "top", "否", via=[(1370, 885)])
    c.connect("filter", "aggregate", "right", "left", via=[(665, 1265)])
    c.connect("all", "aggregate", "left", "right", via=[(1135, 1265)])
    c.connect("aggregate", "dto")
    c.connect("dto", "charts")
    c.connect("charts", "refresh")
    c.connect("refresh", "request", "left", "left", "是", via=[(190, 1875), (190, 500)])
    c.connect("refresh", "end", label="否")
    return c.save("图4-5_数据统计与画像可视化模块程序流程图.png")


def main():
    paths = [chart_4_1(), chart_4_2(), chart_4_3(), chart_4_4(), chart_4_5()]
    for path in paths:
        print(path)


if __name__ == "__main__":
    main()
