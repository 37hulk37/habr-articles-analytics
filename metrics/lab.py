import argparse
import ast
import os

import pandas as pd

TZ = "Europe/Moscow"
TOP_TAGS = 15
DEFAULT_WINDOW = 60

EVENTS = {
    "Go 1.27": dict(files=["Go 1.27.json"], event_date="2026-08-19", window=30,
                    relevant=r"go\s?1\.27",
                    keywords=[r"go 1\.27", r"generic", r"simd", r"\bjson\b", r"горутин", r"аллокатор"]),
    "Rust 1.95": dict(files=["Rust 1.95.json"], event_date="2026-04-16", window=30,
                      relevant=r"rust",
                      keywords=[r"rust 1\.95", r"\brust\b", r"cargo", r"релиз"]),
    "TypeScript 7.0": dict(files=["TypeScript 7.0.json"], event_date="2026-07-08", window=90,
                           relevant=r"typescript\s?7",
                           keywords=[r"typescript 7", r"\bgo\b|golang", r"компилятор", r"\brc\b|бета"]),
    "Python 3.14": dict(files=["Python 3.14.json"], event_date="2025-10-07", window=120,
                        relevant=r"python\s?3\.14|3\.14\.\d",
                        keywords=[r"python 3\.14", r"\bgil\b", r"free-thread", r"\bjit\b", r"t-строк|template",
                                  r"субинтерпретатор|subinterpreter"]),
    "Java 26": dict(files=["Java 26.json"], event_date="2026-03-17", window=60,
                    relevant=r"java\s?26|jdk\s?26",
                    keywords=[r"java 26", r"jdk", r"\bjep\b", r"релиз|вышла"]),
    "Let's Encrypt: отзыв сертификатов": dict(files=["Отзыв сертификатов Let's Encrypt.json"],
                                              event_date="2020-03-04", window=30,
                                              relevant=r"let'?s encrypt",
                                              keywords=[r"let'?s encrypt", r"отзыв", r"сертификат", r"\bcaa\b"]),
    "Сертификаты Минцифры": dict(files=["сертификатов безопасности Минцифры.json"], event_date=None, window=90,
                                 keywords=[r"минцифры", r"нуц", r"сертификат", r"tls"]),
    "Цифровой рубль": dict(files=["цифровой рубль.json", "цифрового рубля.json"], event_date="2026-09-01", window=120,
                           keywords=[r"цифров\w+ рубл", r"\bцб\b|банк россии|центробанк", r"платформ", r"банк"]),
    "Роскомнадзор: блокировка VPN": dict(files=["Роскомнадзор VPN блокировка.json"], event_date=None, window=365,
                                         keywords=[r"роскомнадзор|ркн", r"vpn", r"блокировк", r"wireguard|openvpn"]),
    "VPN": dict(files=["VPN.json"], event_date=None, window=90,
                keywords=[r"vpn", r"роскомнадзор|ркн", r"блокировк", r"wireguard|openvpn", r"youtube",
                          r"замедлен", r"\bdpi\b", r"прокси"]),
    "DPI": dict(files=["DPI.json"], event_date=None, window=90,
                keywords=[r"\bdpi\b", r"vpn", r"блокировк", r"роскомнадзор|ркн", r"тспу", r"обход"]),
}


def load(csv_path: str) -> pd.DataFrame:
    df = pd.read_csv(csv_path)
    ts = pd.to_datetime(df["date"], utc=True).dt.tz_convert(TZ)
    df["published"] = ts.dt.strftime("%Y-%m-%d %H:%M:%S")
    df["day"] = ts.dt.tz_localize(None).dt.normalize()
    df["week_start"] = df["day"] - pd.to_timedelta(df["day"].dt.weekday, unit="D")
    iso = df["day"].dt.isocalendar()
    df["iso_week"] = iso["year"].astype(str) + "-W" + iso["week"].astype(str).str.zfill(2)
    df["tag_list"] = df["tags"].apply(lambda s: [t.strip().lower() for t in ast.literal_eval(s)])
    df["text"] = (df["title"].fillna("") + " " + df["tag_list"].apply(" ".join)).str.lower()
    return df


def pick_event_date(sub: pd.DataFrame) -> pd.Timestamp:
    wk = sub.groupby("week_start").size()
    return wk.idxmax() + pd.Timedelta(days=3)


def event_slice(df, cfg):
    sub = df[df["source_file"].isin(cfg["files"])].drop_duplicates("id").copy()
    if cfg.get("relevant"):
        sub = sub[sub["text"].str.contains(cfg["relevant"], regex=True)]
    if sub.empty:
        return sub, None, None, None
    ev = pd.Timestamp(cfg["event_date"]) if cfg.get("event_date") else pick_event_date(sub)
    w = pd.Timedelta(days=cfg.get("window", DEFAULT_WINDOW))
    d0, d1 = ev - w, min(ev + w, df["day"].max())
    sub = sub[(sub["day"] >= d0) & (sub["day"] <= d1)].copy()
    ev_week = ev - pd.Timedelta(days=ev.weekday())
    sub["rel_day"] = (sub["day"] - ev).dt.days
    sub["rel_week"] = (sub["week_start"] - ev_week).dt.days // 7
    return sub, ev, d0, d1


def axis(d0, d1, level):
    if level == "day":
        return pd.date_range(d0, d1, freq="D")
    s = d0 - pd.Timedelta(days=d0.weekday())
    e = d1 - pd.Timedelta(days=d1.weekday())
    return pd.date_range(s, e, freq="7D")


def add_rel(t, col, ev, level):
    if level == "day":
        t["rel_day"] = (t[col] - ev).dt.days
    else:
        ev_week = ev - pd.Timedelta(days=ev.weekday())
        t["rel_week"] = (t[col] - ev_week).dt.days // 7
    return t


def total_series(sub, ev, d0, d1, level):
    col = "day" if level == "day" else "week_start"
    s = sub.groupby(col).size().reindex(axis(d0, d1, level), fill_value=0)
    t = s.rename_axis(col).rename("articles").reset_index()
    return add_rel(t, col, ev, level)


def grouped_series(pairs, sub_idx, ev, d0, d1, level, name):
    col = "day" if level == "day" else "week_start"
    p = pairs.merge(sub_idx[["id", col]], on="id")
    if p.empty:
        return pd.DataFrame(columns=[name, col, "articles"])
    c = p.groupby([name, col]).size().unstack(name, fill_value=0)
    c = c.reindex(axis(d0, d1, level), fill_value=0)
    t = c.rename_axis(col).reset_index().melt(id_vars=col, var_name=name, value_name="articles")
    t = add_rel(t, col, ev, level)
    return t[[name, col, "articles"] + [x for x in t.columns if x.startswith("rel_")]]


def tag_pairs(sub, top_n):
    t = sub[["id", "tag_list"]].explode("tag_list").dropna().rename(columns={"tag_list": "tag"})
    t = t.drop_duplicates(["id", "tag"])
    top = t["tag"].value_counts().head(top_n).index
    return t[t["tag"].isin(top)]


def keyword_pairs(sub, keywords):
    label = lambda kw: kw.replace("\\b", "").replace("\\", "")
    rows = [(i, label(kw)) for kw in keywords for i in sub.loc[sub["text"].str.contains(kw, regex=True), "id"]]
    return pd.DataFrame(rows, columns=["id", "keyword"])


def summary_row(name, sub, ev, d0, d1):
    before, after = sub[sub["day"] < ev], sub[sub["day"] >= ev]
    nb, na = (ev - d0).days, (d1 - ev).days + 1
    return dict(event=name, event_date=ev.date(), window_from=d0.date(), window_to=d1.date(),
                articles_total=len(sub), articles_before=len(before), articles_on_after=len(after),
                per_day_before=round(len(before) / nb, 3) if nb else None,
                per_day_after=round(len(after) / na, 3) if na else None,
                articles_on_event_day=int((sub["day"] == ev).sum()),
                peak_day=sub["day"].value_counts().idxmax().date(),
                peak_day_articles=int(sub["day"].value_counts().max()))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    here = os.path.dirname(os.path.abspath(__file__))
    ap.add_argument("--csv", default=os.path.join(here, "dataset.csv"))
    ap.add_argument("--out", default=os.path.join(here, "result"))
    ap.add_argument("--events", default="", help="через запятую; по умолчанию все из EVENTS")
    ap.add_argument("--top-tags", type=int, default=TOP_TAGS)
    a = ap.parse_args()

    df = load(a.csv)
    names = [n.strip() for n in a.events.split(",") if n.strip()] or list(EVENTS)
    unknown = [n for n in names if n not in EVENTS]
    if unknown:
        raise SystemExit(f"Нет в EVENTS: {unknown}. Доступны: {list(EVENTS)}")

    out = {k: [] for k in ["articles", "daily_total", "weekly_total", "tag_daily", "tag_weekly",
                           "keyword_daily", "keyword_weekly", "summary"]}
    for name in names:
        cfg = EVENTS[name]
        sub, ev, d0, d1 = event_slice(df, cfg)
        if sub is None or sub.empty:
            print(f"[{name}] нет статей в окне — пропуск")
            continue
        tag = lambda t: t.assign(event=name)[["event"] + list(t.columns)]
        art = sub[["id", "url", "title", "published", "day", "week_start", "iso_week", "rel_day", "rel_week"]].copy()
        art["tags"] = sub["tag_list"].apply("; ".join)
        out["articles"].append(tag(art))
        for lvl in ("day", "week"):
            out[f"{'daily' if lvl == 'day' else 'weekly'}_total"].append(tag(total_series(sub, ev, d0, d1, lvl)))
            sfx = "daily" if lvl == "day" else "weekly"
            out[f"tag_{sfx}"].append(tag(grouped_series(tag_pairs(sub, a.top_tags), sub, ev, d0, d1, lvl, "tag")))
            out[f"keyword_{sfx}"].append(tag(grouped_series(keyword_pairs(sub, cfg["keywords"]), sub, ev, d0, d1, lvl, "keyword")))
        out["summary"].append(pd.DataFrame([summary_row(name, sub, ev, d0, d1)]))
        flag = "" if cfg.get("event_date") else "  (дата события выбрана автоматически: пик публикаций)"
        print(f"[{name}] событие {ev.date()}, окно ±{cfg.get('window', DEFAULT_WINDOW)} дн., статей {len(sub)}{flag}")

    os.makedirs(a.out, exist_ok=True)
    for k, parts in out.items():
        if parts:
            pd.concat(parts, ignore_index=True).to_csv(os.path.join(a.out, f"{k}.csv"), index=False, encoding="utf-8-sig")
    print("Готово →", os.path.abspath(a.out))


if __name__ == "__main__":
    main()