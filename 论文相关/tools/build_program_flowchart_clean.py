from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math
import textwrap


OUT = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关\图4-X_系统程序流程图_论文插入版.png")
W, H = 1700, 2450
LINE = "#111827"
TEXT = "#475569"
BG = "white"
BLUE = "#EEF6FF"
GREEN = "#F0FDF4"
ORANGE = "#FFF7ED"
GRAY = "#F8FAFC"


def font(size, bold=False):
    for p in [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
    ]:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


TITLE = font(44, True)
HEAD = font(28, True)
BODY = font(23, False)
SMALL = font(20, False)


def size(draw, text, fnt):
    b = draw.textbbox((0, 0), text, font=fnt)
    return b[2] - b[0], b[3] - b[1]


def wrap(text, n):
    out = []
    for part in text.split("\n"):
        if len(part) <= n:
            out.append(part)
        else:
            out += textwrap.wrap(part, width=n, break_long_words=True, replace_whitespace=False)
    return out


def center_lines(draw, cx, cy, lines, fnt, color, line_h):
    total = len(lines) * line_h
    y = cy - total / 2
    for line in lines:
        tw, _ = size(draw, line, fnt)
        draw.text((cx - tw / 2, y), line, font=fnt, fill=color)
        y += line_h


def terminal(draw, cx, cy, w, h, text):
    x, y = cx - w / 2, cy - h / 2
    draw.rounded_rectangle((x, y, x + w, y + h), radius=h // 2, fill=GREEN, outline=LINE, width=3)
    center_lines(draw, cx, cy, [text], HEAD, LINE, 34)


def process(draw, cx, cy, w, h, title, body="", fill=GRAY, chars=16):
    x, y = cx - w / 2, cy - h / 2
    draw.rounded_rectangle((x, y, x + w, y + h), radius=12, fill=fill, outline=LINE, width=3)
    if body:
        tw, _ = size(draw, title, HEAD)
        draw.text((cx - tw / 2, y + 14), title, font=HEAD, fill=LINE)
        center_lines(draw, cx, y + h / 2 + 15, wrap(body, chars), BODY, TEXT, 29)
    else:
        center_lines(draw, cx, cy, [title], HEAD, LINE, 34)


def io(draw, cx, cy, w, h, title, body="", chars=16):
    x, y = cx - w / 2, cy - h / 2
    skew = 36
    pts = [(x + skew, y), (x + w, y), (x + w - skew, y + h), (x, y + h)]
    draw.polygon(pts, fill=BLUE, outline=LINE)
    for i in range(4):
        draw.line([pts[i], pts[(i + 1) % 4]], fill=LINE, width=3)
    if body:
        tw, _ = size(draw, title, HEAD)
        draw.text((cx - tw / 2, y + 14), title, font=HEAD, fill=LINE)
        center_lines(draw, cx, y + h / 2 + 15, wrap(body, chars), BODY, TEXT, 29)
    else:
        center_lines(draw, cx, cy, [title], HEAD, LINE, 34)


def decision(draw, cx, cy, w, h, text, chars=10):
    pts = [(cx, cy - h / 2), (cx + w / 2, cy), (cx, cy + h / 2), (cx - w / 2, cy)]
    draw.polygon(pts, fill="white", outline=LINE)
    for i in range(4):
        draw.line([pts[i], pts[(i + 1) % 4]], fill=LINE, width=3)
    center_lines(draw, cx, cy, wrap(text, chars), HEAD, LINE, 34)


def label(draw, text, x, y):
    tw, th = size(draw, text, SMALL)
    draw.rounded_rectangle((x - tw / 2 - 7, y - th / 2 - 6, x + tw / 2 + 7, y + th / 2 + 6), radius=7, fill="white")
    draw.text((x - tw / 2, y - th / 2 - 1), text, font=SMALL, fill=TEXT)


def arrow(draw, p1, p2, text=None, offset=(0, 0)):
    x1, y1 = p1
    x2, y2 = p2
    draw.line((x1, y1, x2, y2), fill=LINE, width=4)
    ang = math.atan2(y2 - y1, x2 - x1)
    s = 16
    tri = [
        (x2, y2),
        (x2 - s * math.cos(ang - math.pi / 6), y2 - s * math.sin(ang - math.pi / 6)),
        (x2 - s * math.cos(ang + math.pi / 6), y2 - s * math.sin(ang + math.pi / 6)),
    ]
    draw.polygon(tri, fill=LINE)
    if text:
        label(draw, text, (x1 + x2) / 2 + offset[0], (y1 + y2) / 2 + offset[1])


def poly(draw, pts, text=None, label_seg=0):
    for a, b in zip(pts, pts[1:]):
        draw.line((a[0], a[1], b[0], b[1]), fill=LINE, width=4)
    x1, y1 = pts[-2]
    x2, y2 = pts[-1]
    ang = math.atan2(y2 - y1, x2 - x1)
    s = 16
    tri = [
        (x2, y2),
        (x2 - s * math.cos(ang - math.pi / 6), y2 - s * math.sin(ang - math.pi / 6)),
        (x2 - s * math.cos(ang + math.pi / 6), y2 - s * math.sin(ang + math.pi / 6)),
    ]
    draw.polygon(tri, fill=LINE)
    if text:
        label_seg = min(label_seg, len(pts) - 2)
        a, b = pts[label_seg], pts[label_seg + 1]
        label(draw, text, (a[0] + b[0]) / 2, (a[1] + b[1]) / 2)


img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

title = "实时信息流排序与分发系统程序流程图"
tw, _ = size(d, title, TITLE)
d.text(((W - tw) / 2, 42), title, font=TITLE, fill=LINE)
d.line((135, 112, W - 135, 112), fill="#CBD5E1", width=3)

cx = 850
terminal(d, cx, 170, 360, 70, "开始")
io(d, cx, 285, 560, 86, "访问系统", "浏览器进入 Vue 前端页面")
process(d, cx, 420, 560, 92, "登录或注册", "提交账号密码，保存 Token")
decision(d, cx, 575, 300, 126, "认证是否通过")
process(d, 1275, 575, 390, 92, "提示认证失败", "返回登录页面重新输入", ORANGE, 13)
process(d, cx, 735, 560, 92, "进入系统首页", "加载用户状态和基础信息")
decision(d, cx, 890, 300, 126, "是否管理员")

process(d, 1275, 890, 390, 105, "后台管理流程", "策略切换、用户管理、广告配置、数据导入", BLUE, 13)
process(d, 1275, 1045, 390, 92, "保存管理结果", "更新数据库和系统配置", GRAY, 13)

io(d, cx, 1045, 560, 92, "选择用户操作", "浏览推荐流、发帖、搜索、互动")
decision(d, cx, 1205, 330, 126, "是否请求推荐流")

process(d, 375, 1205, 390, 92, "处理内容互动", "发帖、评论、点赞、关注、私信", GRAY, 13)
process(d, 375, 1360, 390, 92, "写入行为记录", "记录浏览、点赞、评论、搜索", ORANGE, 13)
process(d, 375, 1515, 390, 92, "更新用户画像", "兴趣标签、作者偏好、近期兴趣", GREEN, 13)

process(d, cx, 1360, 560, 92, "构建候选内容池", "读取内容、标签、行为和画像数据", GREEN, 17)
decision(d, cx, 1515, 330, 126, "推荐策略是否为 AI")
process(d, 1275, 1515, 390, 92, "AI 推荐处理", "调用 Ollama 生成排序或理由", GRAY, 13)
decision(d, 1275, 1670, 300, 126, "AI 是否可用")
process(d, cx, 1670, 560, 92, "传统混合推荐", "标签匹配、TF-IDF、协同过滤、时间衰减", GRAY, 17)
process(d, cx, 1830, 560, 92, "排序重排与过滤", "作者多样性、标签覆盖率、负反馈过滤", GREEN, 17)
process(d, cx, 1990, 560, 92, "广告匹配插入", "根据画像标签和广告配置插入广告", GREEN, 17)
io(d, cx, 2150, 560, 92, "返回信息流结果", "内容列表、评分明细、策略徽标")
decision(d, cx, 2305, 300, 108, "继续操作")
terminal(d, cx, 2410, 360, 70, "结束")

# Main vertical arrows
arrow(d, (cx, 205), (cx, 242))
arrow(d, (cx, 328), (cx, 374))
arrow(d, (cx, 466), (cx, 512))
arrow(d, (cx, 638), (cx, 689), "是")
arrow(d, (cx, 781), (cx, 827))
arrow(d, (cx, 953), (cx, 999), "否")
arrow(d, (cx, 1091), (cx, 1142))
arrow(d, (cx, 1268), (cx, 1314), "是")
arrow(d, (cx, 1406), (cx, 1452))
arrow(d, (cx, 1578), (cx, 1624), "否")
arrow(d, (cx, 1716), (cx, 1784))
arrow(d, (cx, 1876), (cx, 1944))
arrow(d, (cx, 2036), (cx, 2104))
arrow(d, (cx, 2196), (cx, 2251))
arrow(d, (cx, 2359), (cx, 2375), "否")

# Auth fail loop
poly(d, [(1000, 575), (1080, 575)], "否")
poly(d, [(1275, 529), (1275, 420), (1130, 420)])

# Admin branch
poly(d, [(1000, 890), (1080, 890)], "是")
arrow(d, (1275, 942), (1275, 999))
poly(d, [(1275, 1091), (1275, 1160), (1015, 1160)], "回到操作")

# Non-feed branch
poly(d, [(685, 1205), (570, 1205)], "否")
arrow(d, (375, 1251), (375, 1314))
arrow(d, (375, 1406), (375, 1469))
poly(d, [(375, 1561), (375, 1608), (665, 1608), (665, 1360)], "影响推荐", 1)

# AI branch
poly(d, [(1015, 1515), (1080, 1515)], "是")
arrow(d, (1275, 1561), (1275, 1607))
poly(d, [(1125, 1670), (1130, 1670)], "否/降级")
poly(d, [(1275, 1733), (1275, 1830), (1130, 1830)], "是")

# Continue loop
poly(d, [(700, 2305), (240, 2305), (240, 1045), (570, 1045)], "是", 0)

img.save(OUT, quality=95)
print(OUT)
