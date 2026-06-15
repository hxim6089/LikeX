from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


OUT_DIR = Path("论文相关")
W, H = 2400, 1500


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
        r"C:\Windows\Fonts\simsun.ttc",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size=size)
        except OSError:
            continue
    return ImageFont.load_default()


FONT_TITLE = font(54, True)
FONT_BOX = font(34, True)
FONT_SMALL = font(26)
FONT_TINY = font(24)

INK = "#1F2937"
BLUE = "#2563EB"
BLUE_DARK = "#1D4ED8"
BLUE_LIGHT = "#DBEAFE"
GREEN = "#059669"
GREEN_LIGHT = "#D1FAE5"
GRAY = "#6B7280"
GRAY_LIGHT = "#F3F4F6"
AMBER = "#B45309"
AMBER_LIGHT = "#FEF3C7"
RED = "#B91C1C"
RED_LIGHT = "#FEE2E2"
WHITE = "#FFFFFF"


def wrap_text(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.ImageFont, max_width: int) -> list[str]:
    lines: list[str] = []
    for raw in text.split("\n"):
        if not raw:
            lines.append("")
            continue
        line = ""
        for ch in raw:
            candidate = line + ch
            if draw.textbbox((0, 0), candidate, font=fnt)[2] <= max_width or not line:
                line = candidate
            else:
                lines.append(line)
                line = ch
        if line:
            lines.append(line)
    return lines


def center_text(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    fnt: ImageFont.ImageFont,
    fill: str = INK,
    line_gap: int = 8,
) -> None:
    x1, y1, x2, y2 = box
    lines = wrap_text(draw, text, fnt, x2 - x1 - 42)
    heights = [draw.textbbox((0, 0), line, font=fnt)[3] - draw.textbbox((0, 0), line, font=fnt)[1] for line in lines]
    total_h = sum(heights) + line_gap * (len(lines) - 1)
    y = y1 + (y2 - y1 - total_h) / 2
    for line, h in zip(lines, heights):
        bbox = draw.textbbox((0, 0), line, font=fnt)
        x = x1 + (x2 - x1 - (bbox[2] - bbox[0])) / 2
        draw.text((x, y), line, font=fnt, fill=fill)
        y += h + line_gap


def rounded_box(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    fill: str = BLUE_LIGHT,
    outline: str = BLUE,
    text_fill: str = INK,
    width: int = 4,
    radius: int = 26,
    fnt: ImageFont.ImageFont = FONT_BOX,
) -> None:
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)
    center_text(draw, box, text, fnt, text_fill)


def diamond(
    draw: ImageDraw.ImageDraw,
    cx: int,
    cy: int,
    w: int,
    h: int,
    text: str,
    fill: str = AMBER_LIGHT,
    outline: str = AMBER,
) -> tuple[int, int, int, int]:
    pts = [(cx, cy - h // 2), (cx + w // 2, cy), (cx, cy + h // 2), (cx - w // 2, cy)]
    draw.polygon(pts, fill=fill, outline=outline)
    draw.line(pts + [pts[0]], fill=outline, width=4)
    center_text(draw, (cx - w // 2 + 18, cy - h // 2 + 18, cx + w // 2 - 18, cy + h // 2 - 18), text, FONT_SMALL)
    return (cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2)


def arrow(
    draw: ImageDraw.ImageDraw,
    start: tuple[int, int],
    end: tuple[int, int],
    color: str = BLUE_DARK,
    width: int = 6,
    dashed: bool = False,
    label: str | None = None,
    label_offset: tuple[int, int] = (0, 0),
) -> None:
    x1, y1 = start
    x2, y2 = end
    if dashed:
        segments = 22
        for i in range(segments):
            if i % 2 == 0:
                xa = x1 + (x2 - x1) * i / segments
                ya = y1 + (y2 - y1) * i / segments
                xb = x1 + (x2 - x1) * (i + 1) / segments
                yb = y1 + (y2 - y1) * (i + 1) / segments
                draw.line((xa, ya, xb, yb), fill=color, width=width)
    else:
        draw.line((x1, y1, x2, y2), fill=color, width=width)

    angle = math.atan2(y2 - y1, x2 - x1)
    head = 22
    left = (x2 - head * math.cos(angle - math.pi / 6), y2 - head * math.sin(angle - math.pi / 6))
    right = (x2 - head * math.cos(angle + math.pi / 6), y2 - head * math.sin(angle + math.pi / 6))
    draw.polygon([(x2, y2), left, right], fill=color)

    if label:
        lx = (x1 + x2) / 2 + label_offset[0]
        ly = (y1 + y2) / 2 + label_offset[1]
        bbox = draw.textbbox((0, 0), label, font=FONT_TINY)
        pad = 8
        draw.rounded_rectangle(
            (lx - (bbox[2] - bbox[0]) / 2 - pad, ly - 18, lx + (bbox[2] - bbox[0]) / 2 + pad, ly + 18),
            radius=10,
            fill=WHITE,
            outline="#CBD5E1",
        )
        draw.text((lx - (bbox[2] - bbox[0]) / 2, ly - 14), label, font=FONT_TINY, fill=INK)


def polyline_arrow(
    draw: ImageDraw.ImageDraw,
    points: list[tuple[int, int]],
    color: str = BLUE_DARK,
    width: int = 6,
    dashed: bool = False,
    label: str | None = None,
    label_at: tuple[int, int] | None = None,
) -> None:
    for start, end in zip(points, points[1:]):
        x1, y1 = start
        x2, y2 = end
        if dashed:
            segments = max(8, int(math.hypot(x2 - x1, y2 - y1) / 36))
            for i in range(segments):
                if i % 2 == 0:
                    xa = x1 + (x2 - x1) * i / segments
                    ya = y1 + (y2 - y1) * i / segments
                    xb = x1 + (x2 - x1) * (i + 1) / segments
                    yb = y1 + (y2 - y1) * (i + 1) / segments
                    draw.line((xa, ya, xb, yb), fill=color, width=width)
        else:
            draw.line((x1, y1, x2, y2), fill=color, width=width)

    x1, y1 = points[-2]
    x2, y2 = points[-1]
    angle = math.atan2(y2 - y1, x2 - x1)
    head = 22
    left = (x2 - head * math.cos(angle - math.pi / 6), y2 - head * math.sin(angle - math.pi / 6))
    right = (x2 - head * math.cos(angle + math.pi / 6), y2 - head * math.sin(angle + math.pi / 6))
    draw.polygon([(x2, y2), left, right], fill=color)

    if label and label_at:
        lx, ly = label_at
        bbox = draw.textbbox((0, 0), label, font=FONT_TINY)
        pad = 8
        draw.rounded_rectangle(
            (lx - (bbox[2] - bbox[0]) / 2 - pad, ly - 18, lx + (bbox[2] - bbox[0]) / 2 + pad, ly + 18),
            radius=10,
            fill=WHITE,
            outline="#CBD5E1",
        )
        draw.text((lx - (bbox[2] - bbox[0]) / 2, ly - 14), label, font=FONT_TINY, fill=INK)


def title(draw: ImageDraw.ImageDraw, text: str) -> None:
    bbox = draw.textbbox((0, 0), text, font=FONT_TITLE)
    draw.text(((W - bbox[2]) / 2, 42), text, font=FONT_TITLE, fill=INK)
    draw.line((520, 118, W - 520, 118), fill=BLUE, width=5)


def generate_recommendation_pipeline() -> Path:
    img = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(img)
    title(draw, "推荐排序与信息流分发流程图")

    boxes = {
        "behavior": (90, 180, 430, 330),
        "profile": (560, 180, 900, 330),
        "candidate": (1030, 180, 1370, 330),
        "in": (920, 430, 1220, 560),
        "out": (1280, 430, 1580, 560),
        "merge": (1060, 660, 1440, 810),
        "filter": (1530, 660, 1910, 810),
        "score": (540, 980, 980, 1160),
        "diversity": (1080, 980, 1460, 1160),
        "ads": (1560, 980, 1940, 1160),
        "feed": (2010, 980, 2330, 1160),
        "feedback": (90, 980, 430, 1160),
    }

    rounded_box(draw, boxes["behavior"], "用户行为数据\n浏览/点赞/评论/搜索", GREEN_LIGHT, GREEN)
    rounded_box(draw, boxes["profile"], "用户画像生成\n兴趣标签/动态权重", BLUE_LIGHT, BLUE)
    rounded_box(draw, boxes["candidate"], "候选内容获取", BLUE_LIGHT, BLUE)
    rounded_box(draw, boxes["in"], "关注源候选\nIn-Network", GRAY_LIGHT, GRAY, fnt=FONT_SMALL)
    rounded_box(draw, boxes["out"], "全站源候选\nOut-of-Network", GRAY_LIGHT, GRAY, fnt=FONT_SMALL)
    rounded_box(draw, boxes["merge"], "双源合并与去重", BLUE_LIGHT, BLUE)
    rounded_box(draw, boxes["filter"], "负反馈过滤\n屏蔽/点踩/跳过", RED_LIGHT, RED, fnt=FONT_SMALL)
    rounded_box(draw, boxes["score"], "多因子评分\n标签匹配  TF-IDF\n协同过滤  热度  时间衰减", BLUE_LIGHT, BLUE, fnt=FONT_SMALL)
    rounded_box(draw, boxes["diversity"], "作者多样性\n与随机扰动", BLUE_LIGHT, BLUE, fnt=FONT_SMALL)
    rounded_box(draw, boxes["ads"], "广告插入\n与信息流混排", AMBER_LIGHT, AMBER, fnt=FONT_SMALL)
    rounded_box(draw, boxes["feed"], "推荐流展示", GREEN_LIGHT, GREEN)
    rounded_box(draw, boxes["feedback"], "行为反馈回写", GREEN_LIGHT, GREEN)

    arrow(draw, (430, 255), (560, 255))
    arrow(draw, (900, 255), (1030, 255))
    arrow(draw, (1200, 330), (1070, 430))
    arrow(draw, (1200, 330), (1430, 430))
    arrow(draw, (1070, 560), (1180, 660))
    arrow(draw, (1430, 560), (1320, 660))
    arrow(draw, (1440, 735), (1530, 735))
    polyline_arrow(draw, [(1720, 810), (1720, 900), (760, 900), (760, 980)])
    arrow(draw, (980, 1070), (1080, 1070))
    arrow(draw, (1460, 1070), (1560, 1070))
    arrow(draw, (1940, 1070), (2010, 1070))
    arrow(draw, (540, 1070), (430, 1070), color=GREEN)
    polyline_arrow(
        draw,
        [(2170, 980), (2170, 145), (260, 145), (260, 180)],
        color=GREEN,
        dashed=True,
        label="反馈闭环",
        label_at=(1740, 145),
    )

    note = "说明：流程体现从行为采集、画像生成、候选召回、排序分发到反馈回写的闭环。"
    draw.text((90, 1370), note, font=FONT_SMALL, fill=GRAY)

    path = OUT_DIR / "图3-5_推荐排序与信息流分发流程图.png"
    img.save(path, dpi=(300, 300))
    return path


def generate_ai_fallback() -> Path:
    img = Image.new("RGB", (W, H), WHITE)
    draw = ImageDraw.Draw(img)
    title(draw, "AI 推荐与降级流程图")

    start = (90, 190, 470, 340)
    cache = (580, 190, 920, 340)
    cached = (1490, 190, 1830, 340)
    prompt = (330, 560, 710, 720)
    ollama = (820, 560, 1200, 720)
    parse = (1630, 560, 2030, 720)
    rank = (1630, 820, 2030, 980)
    error = (1010, 940, 1350, 1100)
    fallback = (1460, 1040, 1860, 1200)
    result = (1980, 1040, 2340, 1200)

    rounded_box(draw, start, "输入\n用户画像 + 候选内容摘要", BLUE_LIGHT, BLUE, fnt=FONT_SMALL)
    rounded_box(draw, cache, "查询 AI 排序缓存", GRAY_LIGHT, GRAY, fnt=FONT_SMALL)
    diamond(draw, 1140, 265, 330, 190, "缓存命中？")
    rounded_box(draw, cached, "使用缓存\nAI 排序结果", GREEN_LIGHT, GREEN, fnt=FONT_SMALL)
    rounded_box(draw, prompt, "构造 Prompt\n约束输出 JSON", BLUE_LIGHT, BLUE, fnt=FONT_SMALL)
    rounded_box(draw, ollama, "调用 Ollama\n推荐模型", BLUE_LIGHT, BLUE, fnt=FONT_SMALL)
    diamond(draw, 1420, 650, 360, 210, "返回有效\nJSON？")
    rounded_box(draw, parse, "解析 ranking\n与 reasons", GREEN_LIGHT, GREEN, fnt=FONT_SMALL)
    rounded_box(draw, rank, "按 AI 排名\n重排候选列表", GREEN_LIGHT, GREEN, fnt=FONT_SMALL)
    rounded_box(draw, result, "返回推荐流\n展示推荐理由", GREEN_LIGHT, GREEN, fnt=FONT_SMALL)
    rounded_box(draw, error, "超时/异常\n格式不合法", RED_LIGHT, RED, fnt=FONT_SMALL)
    rounded_box(draw, fallback, "回退传统\n混合推荐策略", AMBER_LIGHT, AMBER, fnt=FONT_SMALL)

    arrow(draw, (470, 265), (580, 265))
    arrow(draw, (920, 265), (975, 265))
    arrow(draw, (1305, 265), (1490, 265), color=GREEN, label="是", label_offset=(0, -36))
    polyline_arrow(draw, [(1140, 360), (1140, 455), (520, 455), (520, 560)], label="否", label_at=(980, 455))
    arrow(draw, (710, 640), (820, 640))
    arrow(draw, (1200, 640), (1240, 640))
    arrow(draw, (1600, 650), (1630, 640), color=GREEN, label="是", label_offset=(0, -42))
    arrow(draw, (1830, 720), (1830, 820), color=GREEN)
    arrow(draw, (2030, 900), (2160, 1040), color=GREEN)
    polyline_arrow(draw, [(1420, 755), (1420, 870), (1180, 870), (1180, 940)], color=RED, label="否", label_at=(1360, 870))
    arrow(draw, (1350, 1020), (1460, 1120), color=AMBER)
    arrow(draw, (1860, 1120), (1980, 1120), color=AMBER)
    polyline_arrow(draw, [(1660, 265), (2160, 265), (2160, 1040)], color=GREEN)

    note = "说明：AI 路径成功时使用模型排序；缓存未命中、调用超时或 JSON 解析失败时，自动回退传统推荐，保证信息流可用。"
    draw.text((90, 1370), note, font=FONT_SMALL, fill=GRAY)

    path = OUT_DIR / "图4-17_AI推荐与降级流程图.png"
    img.save(path, dpi=(300, 300))
    return path


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for path in (generate_recommendation_pipeline(), generate_ai_fallback()):
        print(path.resolve())


if __name__ == "__main__":
    main()
