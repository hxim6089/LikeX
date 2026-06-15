from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\TheEnd\qnyproj-main\recommendation-system\论文相关")
OUT = ROOT / "图4-13_推荐排序与用户画像模块时序图_高清清晰版.png"


def load_font(size: int, bold: bool = False):
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf" if bold else r"C:\Windows\Fonts\simsun.ttc",
        r"C:\Windows\Fonts\arial.ttf",
    ]
    for c in candidates:
        try:
            return ImageFont.truetype(c, size=size)
        except Exception:
            continue
    return ImageFont.load_default()


FONT_TITLE = load_font(34, bold=True)
FONT_BOX = load_font(30, bold=True)
FONT_TEXT = load_font(24)
FONT_SMALL = load_font(22)


W, H = 2500, 1700
MARGIN_X = 130
TOP = 110
BOTTOM = 1550
BOX_W = 230
BOX_H = 74

actors = ["用户", "前端页面", "推荐服务", "画像服务", "混合策略", "数据库", "可视化组件"]
x_positions = [MARGIN_X + i * ((W - 2 * MARGIN_X) // (len(actors) - 1)) for i in range(len(actors))]
actor_x = dict(zip(actors, x_positions))


def text_size(draw, text, font):
    box = draw.textbbox((0, 0), text, font=font)
    return box[2] - box[0], box[3] - box[1]


def draw_center_text(draw, xy, text, font, fill=(30, 39, 52), line_gap=6):
    x, y = xy
    lines = text.split("\n")
    heights = [text_size(draw, line, font)[1] for line in lines]
    total_h = sum(heights) + line_gap * (len(lines) - 1)
    cur_y = y - total_h / 2
    for line, h in zip(lines, heights):
        w, _ = text_size(draw, line, font)
        draw.text((x - w / 2, cur_y), line, font=font, fill=fill)
        cur_y += h + line_gap


def draw_actor_box(draw, x, y, label):
    left = x - BOX_W / 2
    top = y
    right = x + BOX_W / 2
    bottom = y + BOX_H
    draw.rounded_rectangle(
        (left, top, right, bottom),
        radius=10,
        fill=(248, 251, 255),
        outline=(35, 45, 58),
        width=3,
    )
    draw_center_text(draw, (x, y + BOX_H / 2), label, FONT_BOX)


def draw_lifeline(draw, x):
    y1 = TOP + BOX_H
    y2 = BOTTOM
    dash = 18
    gap = 13
    y = y1 + 8
    while y < y2 - 10:
        draw.line((x, y, x, min(y + dash, y2)), fill=(140, 152, 165), width=3)
        y += dash + gap


def draw_arrow(draw, src, dst, y, label, dashed=False, label_offset=0):
    x1 = actor_x[src]
    x2 = actor_x[dst]
    color = (24, 35, 50)
    width = 4
    if dashed:
        dash = 18
        gap = 10
        dx = 1 if x2 > x1 else -1
        cur = x1
        while (cur - x2) * dx < -dash:
            nxt = cur + dx * dash
            draw.line((cur, y, nxt, y), fill=color, width=width)
            cur = nxt + dx * gap
        draw.line((cur, y, x2, y), fill=color, width=width)
    else:
        draw.line((x1, y, x2, y), fill=color, width=width)

    # arrow head
    direction = 1 if x2 > x1 else -1
    ah = 18
    draw.polygon(
        [
            (x2, y),
            (x2 - direction * ah, y - ah / 2),
            (x2 - direction * ah, y + ah / 2),
        ],
        fill=color,
    )
    mid = (x1 + x2) / 2
    ty = y - 35 + label_offset
    draw_center_text(draw, (mid, ty), label, FONT_SMALL, fill=(62, 74, 89), line_gap=4)


def main():
    img = Image.new("RGB", (W, H), "white")
    draw = ImageDraw.Draw(img)

    # actor boxes and lifelines
    for actor in actors:
        x = actor_x[actor]
        draw_actor_box(draw, x, TOP, actor)
        draw_lifeline(draw, x)
        draw_actor_box(draw, x, BOTTOM, actor)

    # arrows, spaced with larger vertical intervals
    steps = [
        ("用户", "前端页面", 260, "访问推荐流\n或算法对比页", False, 0),
        ("前端页面", "推荐服务", 390, "请求个性化推荐列表", False, 0),
        ("推荐服务", "数据库", 520, "读取候选内容、行为、\n标签和负反馈", False, 0),
        ("推荐服务", "画像服务", 650, "根据用户行为构建画像", False, 0),
        ("画像服务", "数据库", 780, "查询近期行为、搜索记录\n和标签关联", False, 0),
        ("画像服务", "推荐服务", 910, "返回兴趣权重、作者偏好\n和近期兴趣序列", True, 0),
        ("推荐服务", "混合策略", 1040, "传入候选内容和画像特征", False, 0),
        ("混合策略", "推荐服务", 1170, "返回排序结果\n和评分拆分", True, 0),
        ("推荐服务", "前端页面", 1300, "返回推荐列表、多样性指标\n和对比指标", True, 0),
        ("前端页面", "可视化组件", 1420, "渲染信息流、评分明细\n和推荐对比图", False, 0),
        ("前端页面", "用户", 1510, "展示推荐结果与用户画像", True, -8),
    ]

    for step in steps:
        draw_arrow(draw, *step)

    img.save(OUT, quality=100)
    print(OUT)


if __name__ == "__main__":
    main()
