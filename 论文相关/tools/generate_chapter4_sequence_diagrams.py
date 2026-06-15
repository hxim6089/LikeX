from pathlib import Path
import math

from PIL import Image, ImageDraw, ImageFont


OUT_DIR = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关\第4章时序图_无图名")


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
HEAD_FONT = load_font(30, True)
MSG_FONT = load_font(25)
SMALL_FONT = load_font(23)


class SequenceCanvas:
    def __init__(self, title, participants, messages, width=2600):
        self.title = title
        self.participants = participants
        self.messages = messages
        self.width = max(width, 300 + len(participants) * 290)
        self.top = 70
        self.box_w = 250
        self.box_h = 78
        self.margin_x = 150
        self.xs = {}
        span = self.width - 2 * self.margin_x
        for idx, p in enumerate(participants):
            self.xs[p] = self.margin_x + span * idx / (len(participants) - 1)
        self.height = 320 + len(messages) * 92
        self.img = Image.new("RGB", (self.width, self.height), "white")
        self.draw = ImageDraw.Draw(self.img)

    def wrap_text(self, text, font, max_width):
        lines = []
        for para in str(text).split("\n"):
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
        return lines or [""]

    def text_center(self, cx, cy, text, max_width, font, fill="#111827"):
        lines = self.wrap_text(text, font, max_width)
        line_h = font.size + 6
        y = cy - len(lines) * line_h / 2
        for line in lines:
            bbox = self.draw.textbbox((0, 0), line, font=font)
            self.draw.text((cx - (bbox[2] - bbox[0]) / 2, y), line, font=font, fill=fill)
            y += line_h

    def dashed_line(self, p1, p2, dash=16, gap=12, width=2, fill="#6B7280"):
        x1, y1 = p1
        x2, y2 = p2
        length = math.hypot(x2 - x1, y2 - y1)
        if length == 0:
            return
        dx = (x2 - x1) / length
        dy = (y2 - y1) / length
        pos = 0
        while pos < length:
            end = min(pos + dash, length)
            self.draw.line((x1 + dx * pos, y1 + dy * pos, x1 + dx * end, y1 + dy * end), fill=fill, width=width)
            pos += dash + gap

    def arrow_head(self, p1, p2, size=15):
        angle = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
        left = (p2[0] - size * math.cos(angle - math.pi / 6), p2[1] - size * math.sin(angle - math.pi / 6))
        right = (p2[0] - size * math.cos(angle + math.pi / 6), p2[1] - size * math.sin(angle + math.pi / 6))
        self.draw.polygon([p2, left, right], fill="#111827")

    def draw_participants(self):
        bottom_y = self.height - 95
        for p in self.participants:
            x = self.xs[p]
            y1 = self.top
            y2 = self.top + self.box_h
            self.draw.rounded_rectangle((x - self.box_w / 2, y1, x + self.box_w / 2, y2), radius=12, fill="#F9FAFB", outline="#111827", width=3)
            self.text_center(x, (y1 + y2) / 2, p, self.box_w - 20, HEAD_FONT)
            self.dashed_line((x, y2), (x, bottom_y), dash=13, gap=10, width=2)
            self.draw.rounded_rectangle((x - self.box_w / 2, bottom_y, x + self.box_w / 2, bottom_y + self.box_h), radius=12, fill="#FFFFFF", outline="#111827", width=2)
            self.text_center(x, bottom_y + self.box_h / 2, p, self.box_w - 20, HEAD_FONT)

    def draw_message(self, msg, y):
        src, dst, text = msg[0], msg[1], msg[2]
        dashed = len(msg) > 3 and msg[3] == "return"
        x1, x2 = self.xs[src], self.xs[dst]
        label_y = y - 38
        max_label_width = max(220, abs(x2 - x1) - 40)
        lines = self.wrap_text(text, MSG_FONT, max_label_width)
        label_h = len(lines) * (MSG_FONT.size + 5)
        label_w = 0
        for line in lines:
            bbox = self.draw.textbbox((0, 0), line, font=MSG_FONT)
            label_w = max(label_w, bbox[2] - bbox[0])
        label_x = (x1 + x2) / 2
        self.draw.rectangle((label_x - label_w / 2 - 10, label_y - 5, label_x + label_w / 2 + 10, label_y + label_h + 5), fill="white")
        yy = label_y
        for line in lines:
            bbox = self.draw.textbbox((0, 0), line, font=MSG_FONT)
            self.draw.text((label_x - (bbox[2] - bbox[0]) / 2, yy), line, font=MSG_FONT, fill="#111827")
            yy += MSG_FONT.size + 5
        if src == dst:
            loop_w = 95
            pts = [(x1, y), (x1 + loop_w, y), (x1 + loop_w, y + 42), (x1, y + 42)]
            self.draw.line(pts, fill="#111827", width=3)
            self.arrow_head((x1 + loop_w, y + 42), (x1, y + 42))
            return
        if dashed:
            self.dashed_line((x1, y), (x2, y), dash=16, gap=10, width=3, fill="#111827")
        else:
            self.draw.line((x1, y, x2, y), fill="#111827", width=3)
        self.arrow_head((x1, y), (x2, y))

    def render(self, filename):
        self.draw_participants()
        y = self.top + self.box_h + 90
        for msg in self.messages:
            self.draw_message(msg, y)
            y += 92
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        path = OUT_DIR / filename
        self.img.save(path, quality=95)
        return path


def build_all():
    specs = []
    specs.append((
        "图4-1 用户认证与内容互动模块时序图",
        ["用户", "前端页面", "认证接口", "内容/行为接口", "业务服务", "数据库", "通知推送"],
        [
            ("用户", "前端页面", "输入账号密码或提交注册信息"),
            ("前端页面", "认证接口", "提交登录或注册请求"),
            ("认证接口", "数据库", "查询用户、校验密码或保存新用户"),
            ("数据库", "认证接口", "返回用户、角色和状态信息", "return"),
            ("认证接口", "前端页面", "返回令牌与用户信息", "return"),
            ("用户", "前端页面", "发布帖子、评论、点赞或转发"),
            ("前端页面", "内容/行为接口", "提交内容请求或行为请求"),
            ("内容/行为接口", "业务服务", "调用内容服务和行为服务"),
            ("业务服务", "数据库", "保存内容、行为和标签数据"),
            ("业务服务", "通知推送", "互动场景生成通知并推送"),
            ("内容/行为接口", "前端页面", "返回内容或互动处理结果", "return"),
            ("前端页面", "用户", "刷新信息流、计数和通知状态", "return"),
        ],
        "图4-1_用户认证与内容互动模块时序图.png",
    ))
    specs.append((
        "图4-2 推荐排序与用户画像模块时序图",
        ["用户", "前端页面", "推荐服务", "画像服务", "混合策略", "数据库", "可视化组件"],
        [
            ("用户", "前端页面", "访问推荐流或算法对比页"),
            ("前端页面", "推荐服务", "请求个性化推荐列表"),
            ("推荐服务", "数据库", "读取候选内容、行为、标签和负反馈"),
            ("推荐服务", "画像服务", "根据用户行为构建画像"),
            ("画像服务", "数据库", "查询近期行为、搜索记录和标签关联"),
            ("画像服务", "推荐服务", "返回兴趣权重、作者偏好和近期兴趣序列", "return"),
            ("推荐服务", "混合策略", "传入候选内容和画像特征"),
            ("混合策略", "混合策略", "计算标签匹配、文本相似度、协同过滤和时间衰减"),
            ("混合策略", "推荐服务", "返回排序结果和评分拆分", "return"),
            ("推荐服务", "前端页面", "返回推荐列表、多样性指标和对比指标", "return"),
            ("前端页面", "可视化组件", "渲染信息流、评分明细和推荐对比图"),
            ("可视化组件", "用户", "展示推荐结果与用户画像", "return"),
        ],
        "图4-2_推荐排序与用户画像模块时序图.png",
    ))
    specs.append((
        "图4-3 AI推荐与后台管理模块时序图",
        ["管理员/用户", "前端页面", "后台接口", "策略管理", "推荐服务", "AI打标服务", "AI推荐策略", "AI服务", "数据库/缓存"],
        [
            ("管理员/用户", "前端页面", "进入后台或请求 AI 推荐流"),
            ("前端页面", "后台接口", "查询推荐策略、模型状态或提交策略切换"),
            ("后台接口", "策略管理", "读取或保存传统推荐与 AI 推荐策略"),
            ("策略管理", "后台接口", "返回当前策略配置", "return"),
            ("前端页面", "后台接口", "触发 AI 批量打标"),
            ("后台接口", "AI打标服务", "遍历内容并请求生成标签"),
            ("AI打标服务", "AI服务", "发送帖子正文并获取语义标签"),
            ("AI打标服务", "数据库/缓存", "写入内容标签和处理状态"),
            ("前端页面", "推荐服务", "请求个性化推荐流"),
            ("推荐服务", "策略管理", "根据当前策略选择推荐方式"),
            ("策略管理", "AI推荐策略", "AI 策略下请求推荐排序"),
            ("AI推荐策略", "数据库/缓存", "读取候选摘要或命中缓存"),
            ("AI推荐策略", "AI服务", "缓存未命中时请求排序和推荐理由"),
            ("AI服务", "AI推荐策略", "返回排序结果或异常信息", "return"),
            ("AI推荐策略", "数据库/缓存", "成功写入缓存，异常记录降级"),
            ("AI推荐策略", "推荐服务", "返回 AI 排序结果或传统推荐降级结果", "return"),
            ("推荐服务", "前端页面", "返回推荐列表和策略标识", "return"),
        ],
        "图4-3_AI推荐与后台管理模块时序图.png",
    ))
    specs.append((
        "图4-4 广告分发模块时序图",
        ["用户", "信息流页面", "广告服务", "画像服务", "广告数据层", "数据库", "广告卡片"],
        [
            ("用户", "信息流页面", "浏览首页推荐流"),
            ("信息流页面", "广告服务", "请求广告配置和匹配广告"),
            ("广告服务", "画像服务", "读取用户画像与兴趣标签"),
            ("画像服务", "广告服务", "返回画像标签和偏好权重", "return"),
            ("广告服务", "广告数据层", "查询启用广告、频率和投放限制"),
            ("广告数据层", "数据库", "读取广告配置和素材数据"),
            ("数据库", "广告数据层", "返回广告配置和素材", "return"),
            ("广告服务", "广告服务", "计算标签匹配度并筛选广告"),
            ("广告服务", "信息流页面", "返回广告配置和匹配广告列表", "return"),
            ("信息流页面", "信息流页面", "按配置间隔插入广告卡片"),
            ("信息流页面", "广告卡片", "渲染广告并记录曝光"),
            ("广告卡片", "广告服务", "用户点击广告时上报点击事件"),
            ("广告服务", "数据库", "更新展示量、点击量和点击率统计"),
        ],
        "图4-4_广告分发模块时序图.png",
    ))
    specs.append((
        "图4-5 数据统计与画像可视化模块时序图",
        ["用户/管理员", "可视化页面", "统计接口", "统计服务", "画像/对比服务", "数据库", "图表组件"],
        [
            ("用户/管理员", "可视化页面", "打开统计页、画像页或推荐对比页"),
            ("可视化页面", "统计接口", "请求统计数据并携带时间范围"),
            ("统计接口", "统计服务", "汇总平台运行、行为和内容生态数据"),
            ("统计服务", "数据库", "查询用户、内容、行为、标签和广告统计"),
            ("数据库", "统计服务", "返回聚合所需基础数据", "return"),
            ("可视化页面", "画像/对比服务", "请求用户画像或推荐对比指标"),
            ("画像/对比服务", "数据库", "读取行为序列、画像标签和推荐结果"),
            ("画像/对比服务", "可视化页面", "返回画像、近期兴趣序列和多样性指标", "return"),
            ("统计服务", "可视化页面", "返回行为分布、内容标签分布等统计数据", "return"),
            ("可视化页面", "图表组件", "传入图表数据并触发渲染"),
            ("图表组件", "用户/管理员", "展示折线图、饼图、柱状图、词云和雷达图", "return"),
        ],
        "图4-5_数据统计与画像可视化模块时序图.png",
    ))

    paths = []
    for title, participants, messages, filename in specs:
        paths.append(SequenceCanvas(title, participants, messages).render(filename))
    return paths


def main():
    for path in build_all():
        print(path)


if __name__ == "__main__":
    main()
