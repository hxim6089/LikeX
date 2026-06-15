from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math
import textwrap


OUT_DIR = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关")
PNG = OUT_DIR / "图4-X_系统主程序功能流程图_论文版.png"


W, H = 2400, 1500
BG = "white"
LINE = "#111827"
MUTED = "#475569"
FILL = "#F8FAFC"
FILL2 = "#EEF6FF"
FILL3 = "#FFF7ED"
FILL4 = "#F0FDF4"


def font(size, bold=False):
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
    ]
    for p in candidates:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


TITLE_FONT = font(48, True)
BOX_FONT = font(29, True)
TEXT_FONT = font(22, False)
SMALL_FONT = font(20, False)


def text_size(draw, text, fnt):
    box = draw.textbbox((0, 0), text, font=fnt)
    return box[2] - box[0], box[3] - box[1]


def wrap_cn(text, max_chars):
    lines = []
    for part in text.split("\n"):
        if len(part) <= max_chars:
            lines.append(part)
        else:
            lines.extend(textwrap.wrap(part, width=max_chars, break_long_words=True, replace_whitespace=False))
    return lines


def round_rect(draw, xy, radius=18, fill=FILL, outline=LINE, width=3):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def diamond(draw, cx, cy, w, h, fill=FILL3, outline=LINE, width=3):
    pts = [(cx, cy - h // 2), (cx + w // 2, cy), (cx, cy + h // 2), (cx - w // 2, cy)]
    draw.polygon(pts, fill=fill, outline=outline)
    for i in range(len(pts)):
        draw.line([pts[i], pts[(i + 1) % len(pts)]], fill=outline, width=width)


def draw_box(draw, x, y, w, h, title, body="", fill=FILL, max_chars=13):
    round_rect(draw, (x, y, x + w, y + h), fill=fill)
    title_w, title_h = text_size(draw, title, BOX_FONT)
    draw.text((x + (w - title_w) / 2, y + 14), title, fill=LINE, font=BOX_FONT)
    if body:
        lines = wrap_cn(body, max_chars)
        line_h = 28
        start_y = y + 52
        for idx, line in enumerate(lines):
            tw, _ = text_size(draw, line, TEXT_FONT)
            draw.text((x + (w - tw) / 2, start_y + idx * line_h), line, fill=MUTED, font=TEXT_FONT)


def draw_start(draw, x, y, w, h, text):
    draw.rounded_rectangle((x, y, x + w, y + h), radius=h // 2, fill="#ECFDF5", outline=LINE, width=3)
    tw, th = text_size(draw, text, BOX_FONT)
    draw.text((x + (w - tw) / 2, y + (h - th) / 2 - 2), text, fill=LINE, font=BOX_FONT)


def arrow(draw, p1, p2, label=None, label_pos=0.5, color=LINE, width=4):
    x1, y1 = p1
    x2, y2 = p2
    draw.line((x1, y1, x2, y2), fill=color, width=width)
    ang = math.atan2(y2 - y1, x2 - x1)
    size = 16
    pts = [
        (x2, y2),
        (x2 - size * math.cos(ang - math.pi / 6), y2 - size * math.sin(ang - math.pi / 6)),
        (x2 - size * math.cos(ang + math.pi / 6), y2 - size * math.sin(ang + math.pi / 6)),
    ]
    draw.polygon(pts, fill=color)
    if label:
        lx = x1 + (x2 - x1) * label_pos
        ly = y1 + (y2 - y1) * label_pos
        tw, th = text_size(draw, label, SMALL_FONT)
        pad = 8
        draw.rounded_rectangle((lx - tw / 2 - pad, ly - th / 2 - pad, lx + tw / 2 + pad, ly + th / 2 + pad), radius=8, fill="white")
        draw.text((lx - tw / 2, ly - th / 2 - 1), label, fill=MUTED, font=SMALL_FONT)


def poly_arrow(draw, pts, label=None, label_index=None):
    for a, b in zip(pts, pts[1:]):
        draw.line((a[0], a[1], b[0], b[1]), fill=LINE, width=4)
    x1, y1 = pts[-2]
    x2, y2 = pts[-1]
    ang = math.atan2(y2 - y1, x2 - x1)
    size = 16
    tri = [
        (x2, y2),
        (x2 - size * math.cos(ang - math.pi / 6), y2 - size * math.sin(ang - math.pi / 6)),
        (x2 - size * math.cos(ang + math.pi / 6), y2 - size * math.sin(ang + math.pi / 6)),
    ]
    draw.polygon(tri, fill=LINE)
    if label:
        idx = label_index if label_index is not None else len(pts) // 2 - 1
        a, b = pts[idx], pts[idx + 1]
        lx, ly = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
        tw, th = text_size(draw, label, SMALL_FONT)
        draw.rounded_rectangle((lx - tw / 2 - 8, ly - th / 2 - 8, lx + tw / 2 + 8, ly + th / 2 + 8), radius=8, fill="white")
        draw.text((lx - tw / 2, ly - th / 2 - 1), label, fill=MUTED, font=SMALL_FONT)


img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

title = "实时信息流排序与分发系统主程序流程图"
tw, th = text_size(d, title, TITLE_FONT)
d.text(((W - tw) / 2, 42), title, fill=LINE, font=TITLE_FONT)
d.line((230, 115, W - 230, 115), fill="#CBD5E1", width=3)

# Center main flow
draw_start(d, 930, 150, 540, 68, "用户访问前端页面")
draw_box(d, 910, 255, 580, 86, "登录认证与会话校验", "注册、登录、角色识别", FILL2, 18)
diamond(d, 1200, 430, 250, 118, "#FFFFFF")
dt = "是否登录"
dtw, dth = text_size(d, dt, BOX_FONT)
d.text((1200 - dtw / 2, 430 - dth / 2 - 2), dt, fill=LINE, font=BOX_FONT)
draw_box(d, 910, 530, 580, 88, "前端业务操作", "推荐流、关注流、发帖、搜索、互动", FILL2, 18)
draw_box(d, 910, 700, 580, 88, "后端接口处理", "Controller 接收请求，Service 处理业务", FILL, 18)
draw_box(d, 910, 870, 580, 88, "候选内容构建", "关注内容、热门内容、外部导入内容", FILL4, 18)
diamond(d, 1200, 1060, 270, 125, "#FFFFFF")
dt = "推荐策略"
dtw, dth = text_size(d, dt, BOX_FONT)
d.text((1200 - dtw / 2, 1060 - dth / 2 - 2), dt, fill=LINE, font=BOX_FONT)
draw_box(d, 910, 1215, 580, 90, "排序重排与广告插入", "多样性控制、负反馈过滤、广告匹配", FILL4, 18)
draw_box(d, 910, 1380, 580, 82, "信息流返回前端", "内容卡片、评分明细、策略徽标", FILL2, 18)

arrow(d, (1200, 218), (1200, 255))
arrow(d, (1200, 341), (1200, 371))
arrow(d, (1200, 489), (1200, 530), "是")
arrow(d, (1200, 618), (1200, 700))
arrow(d, (1200, 788), (1200, 870))
arrow(d, (1200, 958), (1200, 998))
arrow(d, (1200, 1122), (1200, 1215), "评分完成")
arrow(d, (1200, 1305), (1200, 1380))

# Not logged branch
draw_box(d, 1580, 370, 470, 100, "冷启动推荐", "热门内容、时间倒序、默认画像", FILL3, 16)
poly_arrow(d, [(1325, 430), (1580, 430)], "否")
poly_arrow(d, [(1815, 470), (1815, 574), (1490, 574)])

# Left data and behavior flow
draw_box(d, 220, 530, 510, 88, "内容发布与互动", "发帖、评论、点赞、转发、关注", FILL, 16)
draw_box(d, 220, 700, 510, 88, "行为采集", "浏览、点赞、评论、搜索、点踩", FILL3, 16)
draw_box(d, 220, 870, 510, 88, "用户画像更新", "兴趣标签、作者偏好、近期兴趣", FILL4, 16)
draw_box(d, 220, 1215, 510, 90, "传统混合推荐", "标签、TF-IDF、协同过滤、时间衰减", FILL, 16)
draw_box(d, 220, 1380, 510, 82, "MySQL 数据库", "用户、内容、行为、标签、广告", FILL2, 16)

poly_arrow(d, [(910, 574), (730, 574)], "业务事件")
arrow(d, (475, 618), (475, 700))
arrow(d, (475, 788), (475, 870))
poly_arrow(d, [(730, 914), (910, 914)], "画像信号")
poly_arrow(d, [(1065, 1060), (730, 1260)], "traditional")
arrow(d, (475, 958), (475, 1215), "读写")
arrow(d, (475, 1305), (475, 1380))
poly_arrow(d, [(910, 1420), (730, 1420)], "数据持久化")

# Right admin and AI flow
draw_box(d, 1580, 700, 510, 88, "后台管理与数据配置", "策略切换、数据导入、广告配置", FILL2, 16)
draw_box(d, 1580, 1030, 510, 88, "AI 推荐与异常降级", "Ollama 排序；失败回退传统推荐", FILL, 16)
draw_box(d, 1580, 1215, 510, 90, "广告与统计处理", "广告插入、展示点击、统计图表", FILL4, 16)

poly_arrow(d, [(1490, 744), (1580, 744)], "管理请求")
poly_arrow(d, [(1835, 788), (1835, 1030)], "策略配置")
poly_arrow(d, [(1335, 1060), (1580, 1074)], "ai")
arrow(d, (1835, 1118), (1835, 1215), "可用/降级")
poly_arrow(d, [(1580, 1260), (1490, 1260)])
poly_arrow(d, [(1580, 744), (1490, 914)], "内容参数")

# Feedback loop
poly_arrow(d, [(1200, 1462), (1200, 1480), (475, 1480), (475, 1462)], "新互动更新画像", 1)

img.save(PNG, quality=95)
print(PNG)
