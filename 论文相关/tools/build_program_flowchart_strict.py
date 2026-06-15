from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math
import textwrap


OUT = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关\图4-X_系统程序流程图_标准版.png")
W, H = 1800, 2600
LINE = "#111827"
MUTED = "#475569"
BG = "white"
PROCESS = "#F8FAFC"
DECISION = "#FFFFFF"
IO = "#EEF6FF"
WARN = "#FFF7ED"
OK = "#F0FDF4"


def font(size, bold=False):
    paths = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
    ]
    for p in paths:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


TITLE = font(48, True)
HEAD = font(30, True)
BODY = font(24, False)
SMALL = font(21, False)


def text_size(draw, text, fnt):
    b = draw.textbbox((0, 0), text, font=fnt)
    return b[2] - b[0], b[3] - b[1]


def wrap_text(text, width):
    lines = []
    for part in text.split("\n"):
        if len(part) <= width:
            lines.append(part)
        else:
            lines.extend(textwrap.wrap(part, width=width, break_long_words=True, replace_whitespace=False))
    return lines


def draw_center_text(draw, cx, cy, lines, fnt=BODY, fill=MUTED, line_h=32):
    total = len(lines) * line_h
    y = cy - total / 2
    for line in lines:
        tw, _ = text_size(draw, line, fnt)
        draw.text((cx - tw / 2, y), line, fill=fill, font=fnt)
        y += line_h


def start_end(draw, cx, cy, w, h, text):
    x, y = cx - w / 2, cy - h / 2
    draw.rounded_rectangle((x, y, x + w, y + h), radius=h // 2, fill=OK, outline=LINE, width=3)
    draw_center_text(draw, cx, cy, [text], HEAD, LINE, 36)


def process(draw, cx, cy, w, h, title, body="", fill=PROCESS, chars=18):
    x, y = cx - w / 2, cy - h / 2
    draw.rounded_rectangle((x, y, x + w, y + h), radius=14, fill=fill, outline=LINE, width=3)
    if body:
        title_w, _ = text_size(draw, title, HEAD)
        draw.text((cx - title_w / 2, y + 16), title, fill=LINE, font=HEAD)
        lines = wrap_text(body, chars)
        draw_center_text(draw, cx, y + h / 2 + 18, lines, BODY, MUTED, 31)
    else:
        draw_center_text(draw, cx, cy, [title], HEAD, LINE, 36)


def io_box(draw, cx, cy, w, h, title, body="", chars=18):
    x, y = cx - w / 2, cy - h / 2
    skew = 42
    pts = [(x + skew, y), (x + w, y), (x + w - skew, y + h), (x, y + h)]
    draw.polygon(pts, fill=IO, outline=LINE)
    for i in range(4):
        draw.line([pts[i], pts[(i + 1) % 4]], fill=LINE, width=3)
    if body:
        title_w, _ = text_size(draw, title, HEAD)
        draw.text((cx - title_w / 2, y + 16), title, fill=LINE, font=HEAD)
        draw_center_text(draw, cx, y + h / 2 + 16, wrap_text(body, chars), BODY, MUTED, 31)
    else:
        draw_center_text(draw, cx, cy, [title], HEAD, LINE, 36)


def decision(draw, cx, cy, w, h, text, chars=10):
    pts = [(cx, cy - h / 2), (cx + w / 2, cy), (cx, cy + h / 2), (cx - w / 2, cy)]
    draw.polygon(pts, fill=DECISION, outline=LINE)
    for i in range(4):
        draw.line([pts[i], pts[(i + 1) % 4]], fill=LINE, width=3)
    draw_center_text(draw, cx, cy, wrap_text(text, chars), HEAD, LINE, 36)


def arrow(draw, p1, p2, label=None, label_offset=(0, 0), width=4):
    x1, y1 = p1
    x2, y2 = p2
    draw.line((x1, y1, x2, y2), fill=LINE, width=width)
    ang = math.atan2(y2 - y1, x2 - x1)
    size = 17
    tri = [
        (x2, y2),
        (x2 - size * math.cos(ang - math.pi / 6), y2 - size * math.sin(ang - math.pi / 6)),
        (x2 - size * math.cos(ang + math.pi / 6), y2 - size * math.sin(ang + math.pi / 6)),
    ]
    draw.polygon(tri, fill=LINE)
    if label:
        lx, ly = (x1 + x2) / 2 + label_offset[0], (y1 + y2) / 2 + label_offset[1]
        tw, th = text_size(draw, label, SMALL)
        draw.rounded_rectangle((lx - tw / 2 - 8, ly - th / 2 - 7, lx + tw / 2 + 8, ly + th / 2 + 7), radius=7, fill="white")
        draw.text((lx - tw / 2, ly - th / 2 - 1), label, fill=MUTED, font=SMALL)


def poly_arrow(draw, pts, label=None, label_at=0):
    for a, b in zip(pts, pts[1:]):
        draw.line((a[0], a[1], b[0], b[1]), fill=LINE, width=4)
    x1, y1 = pts[-2]
    x2, y2 = pts[-1]
    ang = math.atan2(y2 - y1, x2 - x1)
    size = 17
    tri = [
        (x2, y2),
        (x2 - size * math.cos(ang - math.pi / 6), y2 - size * math.sin(ang - math.pi / 6)),
        (x2 - size * math.cos(ang + math.pi / 6), y2 - size * math.sin(ang + math.pi / 6)),
    ]
    draw.polygon(tri, fill=LINE)
    if label:
        idx = min(label_at, len(pts) - 2)
        a, b = pts[idx], pts[idx + 1]
        lx, ly = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
        tw, th = text_size(draw, label, SMALL)
        draw.rounded_rectangle((lx - tw / 2 - 8, ly - th / 2 - 7, lx + tw / 2 + 8, ly + th / 2 + 7), radius=7, fill="white")
        draw.text((lx - tw / 2, ly - th / 2 - 1), label, fill=MUTED, font=SMALL)


img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

title = "实时信息流排序与分发系统程序流程图"
tw, _ = text_size(d, title, TITLE)
d.text(((W - tw) / 2, 44), title, fill=LINE, font=TITLE)
d.line((160, 120, W - 160, 120), fill="#CBD5E1", width=3)

cx = 900
start_end(d, cx, 185, 420, 72, "开始")
io_box(d, cx, 310, 560, 96, "访问系统", "浏览器进入前端页面")
process(d, cx, 450, 560, 102, "用户登录/注册", "提交账号密码，前端保存 Token")
decision(d, cx, 615, 300, 130, "认证是否通过")
process(d, 1370, 615, 430, 100, "返回错误提示", "重新输入账号或密码", WARN, 14)
process(d, cx, 790, 560, 104, "进入首页", "加载推荐流、关注流和页面状态")
decision(d, cx, 965, 330, 140, "用户类型")

# Admin branch
process(d, 1380, 965, 470, 116, "进入后台管理", "用户管理、策略切换、数据导入、广告配置")
process(d, 1380, 1135, 470, 102, "更新系统配置", "推荐策略、广告参数、内容数据")
process(d, 1380, 1300, 470, 102, "写入数据库", "保存配置、导入内容和统计数据")

# User operation branch
io_box(d, cx, 1145, 600, 104, "选择前端操作", "浏览信息流、发布内容、搜索、互动")
decision(d, cx, 1320, 350, 140, "是否请求推荐流")

process(d, 465, 1320, 470, 112, "内容与互动处理", "发帖、评论、点赞、转发、关注、私信")
process(d, 465, 1490, 470, 104, "记录行为数据", "写入浏览、点赞、评论、搜索等行为")
process(d, 465, 1660, 470, 104, "更新用户画像", "统计兴趣标签、作者偏好和近期兴趣")

process(d, cx, 1490, 560, 104, "构建候选内容池", "读取内容、标签、行为和画像数据")
decision(d, cx, 1660, 350, 140, "推荐策略是否为 AI")
process(d, 1370, 1660, 470, 112, "AI 推荐处理", "调用 Ollama 排序或生成推荐理由")
decision(d, 1370, 1835, 330, 138, "AI 是否可用")
process(d, cx, 1835, 560, 112, "传统混合推荐", "标签匹配、TF-IDF、协同过滤、时间衰减")
process(d, cx, 2025, 560, 112, "重排与过滤", "作者多样性、标签覆盖率、负反馈过滤")
process(d, cx, 2195, 560, 104, "广告匹配插入", "按画像标签和广告配置插入广告")
io_box(d, cx, 2355, 560, 104, "返回信息流结果", "内容列表、评分明细、策略徽标")
start_end(d, cx, 2505, 420, 72, "结束")

# Main arrows
arrow(d, (cx, 221), (cx, 262))
arrow(d, (cx, 358), (cx, 399))
arrow(d, (cx, 501), (cx, 550))
arrow(d, (cx, 680), (cx, 738), "是")
arrow(d, (cx, 842), (cx, 895))
arrow(d, (cx, 1035), (cx, 1093), "普通用户")
arrow(d, (cx, 1197), (cx, 1250))
arrow(d, (cx, 1390), (cx, 1438), "是")
arrow(d, (cx, 1542), (cx, 1590))

# Failure loop
poly_arrow(d, [(1050, 615), (1155, 615)], "否")
poly_arrow(d, [(1370, 565), (1370, 450), (1180, 450)])

# Admin branch arrows
poly_arrow(d, [(1065, 965), (1145, 965)], "管理员")
arrow(d, (1380, 1023), (1380, 1084))
arrow(d, (1380, 1186), (1380, 1249))
poly_arrow(d, [(1380, 1351), (1380, 1430), (1180, 1490)], "配置参与推荐")

# Non-recommend operation branch
poly_arrow(d, [(725, 1320), (700, 1320)], "否")
arrow(d, (465, 1376), (465, 1438))
arrow(d, (465, 1542), (465, 1608))
poly_arrow(d, [(700, 1660), (620, 1660)], "画像结果")
poly_arrow(d, [(465, 1712), (465, 2420), (690, 2420)], "刷新后重新参与排序", 1)

# Recommendation branches
poly_arrow(d, [(1075, 1660), (1135, 1660)], "是")
arrow(d, (1370, 1716), (1370, 1766))
poly_arrow(d, [(1205, 1835), (1180, 1835)], "否/降级")
poly_arrow(d, [(1370, 1904), (1370, 2025), (1180, 2025)], "是")
arrow(d, (cx, 1891), (cx, 1969))
arrow(d, (cx, 2081), (cx, 2143))
arrow(d, (cx, 2247), (cx, 2303))
arrow(d, (cx, 2407), (cx, 2469))

# Database side
process(d, 260, 2010, 360, 104, "MySQL 数据库", "用户、内容、行为、标签、广告", IO, 12)
poly_arrow(d, [(620, 1490), (640, 1490)], "读取数据")
poly_arrow(d, [(465, 1542), (260, 1958)], "写入行为")
poly_arrow(d, [(620, 2030), (620, 1835), (620, 1490), (620, 1490)])

img.save(OUT, quality=95)
print(OUT)
