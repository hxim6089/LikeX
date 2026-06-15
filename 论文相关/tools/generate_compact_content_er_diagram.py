from pathlib import Path
import math

from PIL import Image, ImageDraw, ImageFont


OUT = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关\内容实体关系图_紧凑版.png")


def font_path(*names):
    for name in names:
        path = Path(r"C:\Windows\Fonts") / name
        if path.exists():
            return str(path)
    return None


FONT_HEI = font_path("simhei.ttf", "msyh.ttc", "simsun.ttc")
FONT_SONG = font_path("simsun.ttc", "msyh.ttc", "simhei.ttf")
FONT_EN = font_path("times.ttf", "timesbd.ttf", "simsun.ttc")


def f(size, bold=False, en=False):
    if en:
        return ImageFont.truetype(FONT_EN, size)
    return ImageFont.truetype(FONT_HEI if bold else FONT_SONG, size)


W, H = 980, 390
img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)

black = "#111827"
light = "#F3F6FA"
dash = "#53687E"
border = "#E2E8F0"


def text_center(x, y, text, font, fill=black):
    box = d.textbbox((0, 0), text, font=font)
    d.text((x - (box[2] - box[0]) / 2, y - (box[3] - box[1]) / 2), text, fill=fill, font=font)


def table_box(x, y, w, h, title, fields, dashed=False, field_step=22):
    if dashed:
        dashed_rect((x, y, x + w, y + h), outline=dash, width=2, dash_len=7)
        d.rectangle((x, y, x + w, y + 36), fill="#FAFBFC")
        dashed_line((x, y + 36), (x + w, y + 36), fill=dash, width=2)
    else:
        d.rectangle((x, y, x + w, y + h), fill="white", outline=black, width=2)
        d.rectangle((x, y, x + w, y + 36), fill=light, outline=black, width=2)
        d.line((x, y + 36, x + w, y + 36), fill=black, width=2)
    text_center(x + w / 2, y + 18, title, f(18, True))
    yy = y + 52
    for field in fields:
        d.text((x + 16, yy), field, fill=black, font=f(14, en=True))
        yy += field_step


def arrow_head(p1, p2, size=12, fill=black):
    angle = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
    left = (p2[0] - size * math.cos(angle - math.pi / 6), p2[1] - size * math.sin(angle - math.pi / 6))
    right = (p2[0] - size * math.cos(angle + math.pi / 6), p2[1] - size * math.sin(angle + math.pi / 6))
    d.polygon([p2, left, right], fill=fill)


def line(points, width=2, fill=black, arrow=False):
    d.line(points, fill=fill, width=width)
    if arrow:
        arrow_head(points[-2], points[-1], fill=fill)


def dashed_line(p1, p2, fill=dash, width=2, dash_len=8, gap=6):
    x1, y1 = p1
    x2, y2 = p2
    length = math.hypot(x2 - x1, y2 - y1)
    if length == 0:
        return
    dx = (x2 - x1) / length
    dy = (y2 - y1) / length
    pos = 0
    while pos < length:
        end = min(pos + dash_len, length)
        d.line((x1 + dx * pos, y1 + dy * pos, x1 + dx * end, y1 + dy * end), fill=fill, width=width)
        pos += dash_len + gap


def dashed_rect(box, outline=dash, width=2, dash_len=8, gap=6):
    x1, y1, x2, y2 = box
    dashed_line((x1, y1), (x2, y1), outline, width, dash_len, gap)
    dashed_line((x2, y1), (x2, y2), outline, width, dash_len, gap)
    dashed_line((x2, y2), (x1, y2), outline, width, dash_len, gap)
    dashed_line((x1, y2), (x1, y1), outline, width, dash_len, gap)


def label(text, x, y, font=None):
    font = font or f(14)
    box = d.textbbox((0, 0), text, font=font)
    d.rectangle((x - 4, y - 3, x + (box[2] - box[0]) + 4, y + (box[3] - box[1]) + 5), fill="white")
    d.text((x, y), text, fill=black, font=font)


def card(text, x, y, font=None):
    font = font or f(14)
    box = d.textbbox((0, 0), text, font=font)
    d.rectangle((x - 4, y - 3, x + (box[2] - box[0]) + 4, y + (box[3] - box[1]) + 5), fill="white")
    d.text((x, y), text, fill=black, font=font)


# light outer boundary, matching the original pasted diagram style
d.rectangle((14, 14, W - 15, H - 15), outline=border, width=1)

user = (56, 78, 185, 178)
content = (400, 56, 235, 220)
stats = (748, 92, 180, 154)

table_box(
    user[0],
    user[1],
    user[2],
    user[3],
    "用户 User",
    ["PK id", "username", "handle", "role", "banned", "created_at"],
)

table_box(
    content[0],
    content[1],
    content[2],
    content[3],
    "内容 Content",
    ["PK id", "FK author_id", "parent_id", "repost_of_id", "quote_of_id", "content", "image_url", "category", "created_at"],
    field_step=19,
)

table_box(
    stats[0],
    stats[1],
    stats[2],
    stats[3],
    "内容统计字段",
    ["view_count", "like_count", "comment_count", "dislike_count", "repost_count"],
    dashed=True,
)

# User publishes content
y_pub = 154
line([(user[0] + user[2], y_pub), (content[0], y_pub)], arrow=True)
label("1", user[0] + user[2] + 6, y_pub - 20, f(16, True))
label("N", content[0] - 18, y_pub - 20, f(16, True))
label("发布", 304, y_pub - 21, f(14))

# Content statistics
y_stats = 154
line([(content[0] + content[2], y_stats), (stats[0], y_stats)], width=2, fill=dash)
arrow_head((content[0] + content[2], y_stats), (stats[0], y_stats), fill=dash)
label("1", content[0] + content[2] + 5, y_stats - 20, f(16, True))
label("1", stats[0] - 21, y_stats - 20, f(16, True))
label("冗余统计", 670, y_stats - 21, f(14))

# Self relations below content, compact and closer to the table
content_bottom = content[1] + content[3]

# comment/reply relation: one parent content may have many child comments
x_comment_left = 400
x_comment_mid = 520
y_comment = 330
line([(x_comment_left, content_bottom), (x_comment_left, y_comment), (x_comment_mid, y_comment), (x_comment_mid, content_bottom)], width=2)
arrow_head((x_comment_mid, y_comment), (x_comment_mid, content_bottom), fill=black)
label("1", x_comment_left + 112, content_bottom - 18, f(15, True))
label("N", x_comment_left - 17, content_bottom - 21, f(15, True))
card("评论/回复", 428, y_comment - 22, f(13))

# repost/quote source relation: repost/quote points back to source content
x_repost = 565
x_repost_right = 635
y_repost = 350
line([(x_repost, content_bottom), (x_repost, y_repost), (x_repost_right, y_repost), (x_repost_right, content_bottom)], width=2)
arrow_head((x_repost_right, y_repost), (x_repost_right, content_bottom), fill=black)
label("1", x_repost + 8, content_bottom - 18, f(15, True))
label("N", x_repost_right - 15, content_bottom - 82, f(15, True))
card("转发/引用来源", 573, y_repost - 21, f(13))

OUT.parent.mkdir(parents=True, exist_ok=True)
img.save(OUT, quality=95)
print(OUT)
