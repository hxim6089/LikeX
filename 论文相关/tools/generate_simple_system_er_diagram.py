from pathlib import Path
import math

from PIL import Image, ImageDraw, ImageFont


OUT = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关\图3-3_系统总体ER图_简略版.png")


def font_path(*names):
    for name in names:
        p = Path(r"C:\Windows\Fonts") / name
        if p.exists():
            return str(p)
    return None


FONT_HEI = font_path("simhei.ttf", "msyh.ttc", "simsun.ttc")
FONT_SONG = font_path("simsun.ttc", "msyh.ttc", "simhei.ttf")


def font(size, bold=False):
    return ImageFont.truetype(FONT_HEI if bold else FONT_SONG, size)


W, H = 1500, 980
img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)

BLACK = "#111827"
LIGHT = "#F3F6FA"
GRID = "#E5E7EB"


def center_text(x, y, text, fnt, fill=BLACK):
    box = d.textbbox((0, 0), text, font=fnt)
    d.text((x - (box[2] - box[0]) / 2, y - (box[3] - box[1]) / 2), text, font=fnt, fill=fill)


def wrap_text(text, fnt, width):
    lines = []
    cur = ""
    for ch in text:
        box = d.textbbox((0, 0), cur + ch, font=fnt)
        if box[2] - box[0] <= width:
            cur += ch
        else:
            if cur:
                lines.append(cur)
            cur = ch
    if cur:
        lines.append(cur)
    return lines


def entity(key, x, y, w, h, title, fields):
    d.rectangle((x, y, x + w, y + h), fill="white", outline=BLACK, width=2)
    d.rectangle((x, y, x + w, y + 42), fill=LIGHT, outline=BLACK, width=2)
    center_text(x + w / 2, y + 21, title, font(20, True))
    yy = y + 62
    for field in fields:
        d.text((x + 18, yy), field, font=font(15), fill=BLACK)
        yy += 26
    boxes[key] = (x, y, w, h)


def anchor(key, side):
    x, y, w, h = boxes[key]
    if side == "left":
        return (x, y + h / 2)
    if side == "right":
        return (x + w, y + h / 2)
    if side == "top":
        return (x + w / 2, y)
    if side == "bottom":
        return (x + w / 2, y + h)
    return (x + w / 2, y + h / 2)


def arrow_head(p1, p2, size=12):
    angle = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
    left = (p2[0] - size * math.cos(angle - math.pi / 6), p2[1] - size * math.sin(angle - math.pi / 6))
    right = (p2[0] - size * math.cos(angle + math.pi / 6), p2[1] - size * math.sin(angle + math.pi / 6))
    d.polygon([p2, left, right], fill=BLACK)


def label(text, x, y, size=15):
    fnt = font(size)
    box = d.textbbox((0, 0), text, font=fnt)
    d.rectangle((x - 5, y - 3, x + box[2] - box[0] + 5, y + box[3] - box[1] + 5), fill="white")
    d.text((x, y), text, font=fnt, fill=BLACK)


def cardinal(text, x, y):
    label(text, x, y, 16)


def relation(src, dst, src_side, dst_side, text, src_card, dst_card, via=None):
    p1 = anchor(src, src_side)
    p2 = anchor(dst, dst_side)
    pts = [p1] + (via or []) + [p2]
    d.line(pts, fill=BLACK, width=2)
    arrow_head(pts[-2], pts[-1])
    mx = sum(p[0] for p in pts) / len(pts)
    my = sum(p[1] for p in pts) / len(pts)
    label(text, mx - 26, my - 28, 15)
    cardinal(src_card, p1[0] + (8 if src_side == "right" else -22), p1[1] - 24)
    cardinal(dst_card, p2[0] + (8 if dst_side == "right" else -22), p2[1] - 24)


boxes = {}

# Outer frame only, no internal title; figure caption should be added in Word.
d.rectangle((18, 18, W - 18, H - 18), outline=GRID, width=1)

entity("user", 80, 95, 250, 170, "用户 User", ["PK id", "username", "role", "banned"])
entity("content", 610, 70, 290, 210, "内容 Content", ["PK id", "FK author_id", "content", "image_url", "created_at"])
entity("tag", 1165, 95, 230, 135, "标签 Tag", ["PK id", "name"])
entity("ctag", 1020, 345, 230, 120, "内容标签", ["FK content_id", "FK tag_id"])

entity("follow", 80, 370, 250, 145, "关注 Follow", ["PK id", "FK follower_id", "FK followee_id"])
entity("behavior", 610, 370, 290, 170, "行为 Behavior", ["PK id", "FK user_id", "FK content_id", "type"])
entity("notice", 1035, 585, 250, 160, "通知 Notification", ["PK id", "FK recipient_id", "FK actor_id", "type"])

entity("message", 80, 645, 250, 155, "私信 Message", ["PK id", "FK sender_id", "FK recipient_id", "content"])
entity("ad", 610, 665, 290, 155, "广告 Ad", ["PK id", "targetTags", "impressions", "clicks"])
entity("adcfg", 1035, 815, 250, 120, "广告配置 AdConfig", ["PK id", "enabled", "frequency"])

# Main relationships, simplified to avoid crossings.
relation("user", "content", "right", "left", "发布", "1", "N")
relation("content", "ctag", "right", "top", "内容标签关联", "1", "N", via=[(970, 175), (1135, 175)])
relation("ctag", "tag", "top", "bottom", "对应", "N", "1", via=[(1135, 270)])

relation("user", "follow", "bottom", "top", "关注关系", "1", "N")
relation("user", "behavior", "right", "left", "产生行为", "1", "N", via=[(455, 180), (455, 455)])
relation("content", "behavior", "bottom", "top", "被互动", "1", "N")

relation("user", "message", "bottom", "top", "发送/接收", "1", "N", via=[(205, 565)])
relation("user", "notice", "right", "left", "接收通知", "1", "N", via=[(420, 180), (420, 665)])
relation("content", "notice", "bottom", "top", "关联内容", "1", "N", via=[(755, 585), (1160, 585)])

relation("ad", "adcfg", "right", "left", "投放控制", "N", "1", via=[(955, 742), (955, 875)])
relation("behavior", "ad", "bottom", "top", "画像匹配投放", "N", "N")

OUT.parent.mkdir(parents=True, exist_ok=True)
img.save(OUT, quality=95)
print(OUT)
