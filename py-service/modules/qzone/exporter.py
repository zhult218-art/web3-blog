# -*- coding: utf-8 -*-
"""qzone 导出渲染：CSV 与离线 HTML。"""
from __future__ import annotations

import csv
import html

from .formatting import parse_emojis_export


def write_csv(path: str, moments: list[dict]):
    with open(path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["时间", "内容", "图片链接", "视频链接", "评论数", "转发数", "位置", "来源", "评论"])
        for m in moments:
            comments = " | ".join(
                f'{c["time"]} {c["name"]}：{c["content"]}'
                + (" 【回复】" + "；".join(f'{r["name"]}:{r["content"]}' for r in c["replies"])
                   if c["replies"] else "")
                for c in m["comments"])
            w.writerow([m["time"], m["content"],
                        " ".join(p.get("origin", "") for p in m["pics"]),
                        " ".join(v.get("origin", "") for v in m["videos"]),
                        m["cmtnum"], m["fwdnum"],
                        m["lbs"], m["source"], comments])


def render_html(name: str, uin: str, exported_at: str, moments: list[dict]) -> str:
    cards = []
    for m in moments:
        esc = lambda s: html.escape(str(s or ""))
        pics = "".join(
            f'<a href="{esc(p.get("origin", ""))}" target="_blank" rel="noopener">'
            f'<img loading="lazy" src="{esc(p.get("origin", ""))}" alt=""></a>'
            for p in m["pics"])
        vids = "".join(
            f'<video controls preload="none" src="{esc(v.get("origin", ""))}"></video>'
            for v in m["videos"])
        content_html = parse_emojis_export(m.get("content") or "")
        comments = ""
        if m["comments"]:
            lis = []
            for c in m["comments"]:
                inner = "".join(
                    f'<div class="reply">{esc(r["name"])}：{parse_emojis_export(r.get("content") or "")}</div>'
                    for r in c["replies"])
                lis.append(f'<li><span class="cname">{esc(c["name"])}</span>'
                           f'<span class="ctime">{esc(c["time"])}</span>'
                           f'<div class="ctext">{parse_emojis_export(c.get("content") or "")}</div>{inner}</li>')
            comments = f'<ul class="comments">{"".join(lis)}</ul>'
        cards.append(f"""
    <article class="card">
      <div class="meta">{esc(m["time"])}
        <span class="src">{esc(m["source"])}</span>
        <span class="lbs">{esc(m["lbs"])}</span>
      </div>
      <div class="content">{content_html}</div>
      <div class="pics">{pics}</div>
      <div class="vids">{vids}</div>
      {comments}
    </article>""")
    return f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(name)} 的QQ空间说说存档</title>
<style>
:root{{--bg:#060a12;--card:#0c1422;--line:rgba(120,170,220,.16);--amber:#fcd34d;--txt:#e2e8f0;--sub:#94a3b8;--cyan:#22d3ee}}
*{{box-sizing:border-box}}
body{{margin:0;background:radial-gradient(1200px 600px at 80% -10%,rgba(34,211,238,.08),transparent),var(--bg);color:var(--txt);font-family:"PingFang SC","Microsoft YaHei",serif;line-height:1.7}}
header{{max-width:820px;margin:0 auto;padding:48px 20px 20px}}
h1{{color:var(--amber);font-size:26px;margin:0 0 8px}}
.sub{{color:var(--sub);font-size:13px}}
main{{max-width:820px;margin:0 auto;padding:0 20px 60px}}
.card{{background:linear-gradient(150deg,#0c1422,#0a111d);border:1px solid var(--line);border-radius:12px;padding:18px 20px;margin:16px 0;box-shadow:0 6px 24px rgba(0,0,0,.35)}}
.meta{{color:var(--cyan);font-size:12px;margin-bottom:8px;font-family:monospace}}
.src,.lbs{{color:var(--sub);margin-left:10px}}
.content{{white-space:pre-wrap;word-break:break-word;font-size:15px}}
.content img.qz-emo{{display:inline-block;width:22px;height:22px;vertical-align:middle}}
.pics{{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}}
.pics img{{width:150px;height:150px;object-fit:cover;border-radius:8px;border:1px solid var(--line)}}
.vids video{{max-width:100%;border-radius:8px;margin-top:10px}}
.comments{{list-style:none;padding:12px 0 0;margin:12px 0 0;border-top:1px dashed var(--line)}}
.comments li{{font-size:13px;margin:8px 0;color:#cbd5e1}}
.cname{{color:var(--amber)}} .ctime{{color:var(--sub);font-size:11px;margin-left:8px;font-family:monospace}}
.ctext{{margin:2px 0}} .reply{{color:var(--sub);padding-left:14px;font-size:12px}}
</style></head>
<body>
<header><h1>时光回溯 · {html.escape(name)} 的说说存档</h1>
<div class="sub">QQ：{html.escape(uin)} ｜ 共 {len(moments)} 条 ｜ 导出时间：{html.escape(exported_at)}</div></header>
<main>{''.join(cards)}</main></body></html>"""
