from pathlib import Path
from xml.sax.saxutils import escape


OUT = Path("拆分ER图")
OUT.mkdir(exist_ok=True)


CSS = """
  .bg { fill: #ffffff; }
  .entity { fill: #ffffff; stroke: #1f2937; stroke-width: 2.4; rx: 12; ry: 12; }
  .entity-head { fill: #f1f5f9; stroke: #1f2937; stroke-width: 2.4; rx: 12; ry: 12; }
  .logical { fill: #ffffff; stroke: #64748b; stroke-width: 2.2; stroke-dasharray: 8 6; rx: 12; ry: 12; }
  .logical-head { fill: #f8fafc; stroke: #64748b; stroke-width: 2.2; stroke-dasharray: 8 6; rx: 12; ry: 12; }
  .title { font-family: "Microsoft YaHei", "SimHei", sans-serif; font-size: 30px; font-weight: 700; fill: #111827; }
  .field { font-family: "Microsoft YaHei", "SimHei", sans-serif; font-size: 22px; fill: #1f2937; }
  .label { font-family: "Microsoft YaHei", "SimHei", sans-serif; font-size: 20px; fill: #374151; font-weight: 600; }
  .card { font-family: "Microsoft YaHei", "SimHei", sans-serif; font-size: 22px; fill: #111827; font-weight: 700; }
  .rel { stroke: #111827; stroke-width: 2.6; fill: none; marker-end: url(#arrow); }
  .rel-dash { stroke: #64748b; stroke-width: 2.4; fill: none; stroke-dasharray: 8 6; marker-end: url(#arrowGray); }
  .divider { stroke: #1f2937; stroke-width: 2.2; }
  .note { fill: #f8fafc; stroke: #cbd5e1; stroke-width: 1.6; rx: 8; ry: 8; }
"""


def t(x, y, text, cls="field", anchor="start"):
    return f'<text x="{x}" y="{y}" text-anchor="{anchor}" class="{cls}">{escape(text)}</text>'


def mt(x, y, lines, cls="label", anchor="middle", gap=26):
    parts = [f'<text x="{x}" y="{y}" text-anchor="{anchor}" class="{cls}">']
    for i, line in enumerate(lines):
        dy = 0 if i == 0 else gap
        parts.append(f'<tspan x="{x}" dy="{dy}">{escape(line)}</tspan>')
    parts.append("</text>")
    return "".join(parts)


def entity(x, y, w, h, name, fields, logical=False):
    box_cls = "logical" if logical else "entity"
    head_cls = "logical-head" if logical else "entity-head"
    out = [
        f'<rect x="{x}" y="{y}" width="{w}" height="{h}" class="{box_cls}"/>',
        f'<rect x="{x}" y="{y}" width="{w}" height="62" class="{head_cls}"/>',
        f'<line x1="{x}" y1="{y+62}" x2="{x+w}" y2="{y+62}" class="divider"/>',
        mt(x + w / 2, y + 40, [name], cls="title"),
    ]
    yy = y + 100
    for field in fields:
        out.append(t(x + 28, yy, field, cls="field"))
        yy += 34
    return "\n".join(out)


def line(x1, y1, x2, y2, label="", c1="", c2="", dashed=False, label_dx=0, label_dy=-12):
    cls = "rel-dash" if dashed else "rel"
    out = [f'<path d="M {x1} {y1} L {x2} {y2}" class="{cls}"/>']
    if label:
        out.append(mt((x1 + x2) / 2 + label_dx, (y1 + y2) / 2 + label_dy, [label], cls="label"))
    if c1:
        out.append(t(x1 + 10, y1 - 10, c1, cls="card"))
    if c2:
        out.append(t(x2 - 28, y2 - 10, c2, cls="card"))
    return "\n".join(out)


def poly(points, label="", c1="", c2="", dashed=False, label_pos=None):
    cls = "rel-dash" if dashed else "rel"
    d = "M " + " L ".join(f"{x} {y}" for x, y in points)
    out = [f'<path d="{d}" class="{cls}"/>']
    if label:
        lx, ly = label_pos if label_pos else points[len(points) // 2]
        out.append(mt(lx, ly, [label], cls="label"))
    if c1:
        x, y = points[0]
        out.append(t(x + 8, y - 10, c1, cls="card"))
    if c2:
        x, y = points[-1]
        out.append(t(x - 28, y - 10, c2, cls="card"))
    return "\n".join(out)


def svg(body, width=1800, height=1050):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
<defs>
<style>{CSS}</style>
<marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
  <path d="M 0 0 L 10 5 L 0 10 z" fill="#111827"/>
</marker>
<marker id="arrowGray" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
  <path d="M 0 0 L 10 5 L 0 10 z" fill="#64748b"/>
</marker>
</defs>
<rect x="0" y="0" width="{width}" height="{height}" class="bg"/>
<rect x="28" y="28" width="{width-56}" height="{height-56}" fill="none" stroke="#e2e8f0" stroke-width="2"/>
{body}
</svg>'''


diagrams = {}

diagrams["图3-3a_用户与内容局部ER图.svg"] = svg("\n".join([
    entity(105, 190, 330, 330, "用户 User", [
        "PK id",
        "username",
        "handle",
        "role",
        "banned",
        "created_at",
    ]),
    entity(725, 150, 420, 410, "内容 Content", [
        "PK id",
        "FK author_id",
        "parent_id",
        "repost_of_id",
        "quote_of_id",
        "content",
        "image_url",
        "category",
        "created_at",
    ]),
    entity(1360, 215, 320, 280, "内容统计字段", [
        "view_count",
        "like_count",
        "comment_count",
        "dislike_count",
        "repost_count",
    ], logical=True),
    line(435, 330, 725, 330, "发布", "1", "N"),
    line(1145, 330, 1360, 330, "冗余统计", "1", "1", dashed=True),
    poly([(935, 560), (935, 720), (725, 720), (725, 500)], "评论/回复", "1", "N", label_pos=(820, 700)),
    poly([(1015, 560), (1015, 790), (1145, 790), (1145, 480)], "转发/引用来源", "1", "N", label_pos=(1100, 770)),
]))

diagrams["图3-3b_行为画像与标签局部ER图.svg"] = svg("\n".join([
    entity(80, 170, 310, 280, "用户 User", [
        "PK id",
        "username",
        "custom_weights",
        "created_at",
    ]),
    entity(520, 135, 350, 350, "行为 Behavior", [
        "PK id",
        "FK user_id",
        "FK content_id",
        "type",
        "duration",
        "created_at",
    ]),
    entity(1010, 155, 340, 320, "内容 Content", [
        "PK id",
        "FK author_id",
        "content",
        "category",
        "created_at",
    ]),
    entity(1505, 205, 230, 230, "标签 Tag", [
        "PK id",
        "name",
    ]),
    entity(1060, 620, 300, 230, "content_tags", [
        "FK content_id",
        "FK tag_id",
        "联合唯一约束",
    ], logical=True),
    entity(515, 620, 360, 280, "负反馈 NegativeSignal", [
        "PK id",
        "FK user_id",
        "target_type",
        "target_id",
        "signal_type",
        "created_at",
    ]),
    line(390, 300, 520, 300, "", "1", "N"),
    mt(455, 258, ["产生行为"], cls="label"),
    line(870, 300, 1010, 300, "", "N", "1"),
    mt(940, 258, ["作用于内容"], cls="label"),
    poly([(1180, 475), (1180, 620)], "绑定", "1", "N", label_pos=(1225, 560)),
    poly([(1360, 735), (1505, 320)], "关联标签", "N", "1", label_pos=(1480, 625)),
    poly([(235, 450), (235, 760), (515, 760)], "发起负反馈", "1", "N", label_pos=(330, 742)),
    poly([(875, 760), (1010, 410)], "过滤候选", "N", "1", dashed=True, label_pos=(965, 620)),
]), width=1800, height=1050)

diagrams["图3-3c_社交关系与消息通知局部ER图.svg"] = svg("\n".join([
    entity(90, 210, 310, 300, "用户 User", [
        "PK id",
        "username",
        "handle",
        "role",
        "banned",
    ]),
    entity(620, 100, 350, 260, "关注 Follow", [
        "PK id",
        "follower_id",
        "followee_id",
        "created_at",
    ]),
    entity(620, 435, 350, 300, "私信 Message", [
        "PK id",
        "sender_id",
        "recipient_id",
        "content",
        "is_read",
        "created_at",
    ]),
    entity(1210, 245, 390, 330, "通知 Notification", [
        "PK id",
        "recipient_id",
        "actor_id",
        "type",
        "entity_id",
        "is_read",
        "created_at",
    ]),
    entity(1215, 675, 380, 230, "内容 Content", [
        "PK id",
        "author_id",
        "content",
    ], logical=True),
    line(400, 275, 620, 230, "关注/被关注", "1", "N"),
    line(400, 410, 620, 570, "发送/接收私信", "1", "N"),
    line(970, 230, 1210, 350, "触发通知", "N", "N"),
    line(970, 570, 1210, 455, "消息提醒", "N", "N"),
    poly([(1400, 575), (1400, 675)], "关联内容", "N", "1", dashed=True, label_pos=(1460, 640)),
]))

diagrams["图3-3d_广告投放局部ER图.svg"] = svg("\n".join([
    entity(90, 210, 330, 300, "用户画像", [
        "user_id",
        "interest_tags",
        "behavior_stats",
        "match_rate",
        "dynamic_weights",
    ], logical=True),
    entity(610, 145, 420, 450, "广告 Ad", [
        "PK id",
        "title",
        "description",
        "image_url",
        "target_url",
        "target_tags",
        "bid_price",
        "impression_count",
        "click_count",
        "active",
    ]),
    entity(1240, 175, 360, 300, "广告配置 AdConfig", [
        "PK id",
        "enabled",
        "frequency",
        "max_ads_per_page",
    ]),
    entity(615, 690, 415, 240, "信息流广告位", [
        "user_id",
        "ad_id",
        "ad_score",
        "insert_index",
    ], logical=True),
    entity(1240, 650, 360, 250, "广告统计", [
        "impression_count",
        "click_count",
        "CTR",
        "quality_score",
    ], logical=True),
    line(420, 330, 610, 330, "标签匹配", "1", "N"),
    line(1030, 330, 1240, 330, "投放控制", "N", "1"),
    poly([(820, 595), (820, 690)], "生成广告位", "N", "N", label_pos=(895, 650)),
    line(1030, 775, 1240, 775, "统计反馈", "N", "1", dashed=True),
]))


for name, content in diagrams.items():
    (OUT / name).write_text(content, encoding="utf-8")
    print(OUT / name)
