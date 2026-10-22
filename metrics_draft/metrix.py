import json
import sys
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path

# ---------- загрузка ----------

def load_articles(path):
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    dates = []
    for item in data:
        raw = item.get("timePublished")
        if not raw:
            continue
        try:
            dt = datetime.fromisoformat(raw.replace("Z", "+00:00"))
        except ValueError:
            continue
        dates.append(dt)
    return dates


# ---------- полный диапазон ключей ----------

def full_range_keys(dates, level):
    start = min(dates).date()
    end   = max(dates).date()
    keys = []
    if level == "day":
        cur = start
        while cur <= end:
            keys.append(cur.strftime("%Y-%m-%d"))
            cur += timedelta(days=1)
    elif level == "month":
        cur = start.replace(day=1)
        while cur <= end:
            keys.append(cur.strftime("%Y-%m"))
            if cur.month == 12:
                cur = cur.replace(year=cur.year + 1, month=1)
            else:
                cur = cur.replace(month=cur.month + 1)
    elif level == "year":
        for y in range(start.year, end.year + 1):
            keys.append(str(y))
    return keys


# ---------- пик и фон ----------

def peak_stats(counter, total_units):
    """counter уже содержит нули за весь период."""
    peak_key, peak_val = max(counter.items(), key=lambda x: x[1])
    total_articles = sum(counter.values())
    other_sum = total_articles - peak_val
    other_cnt = total_units - 1
    baseline = other_sum / other_cnt if other_cnt > 0 else 0
    height = (peak_val / baseline) if baseline > 0 else float("inf")
    return {
        "peak_key": peak_key,
        "peak_value": peak_val,
        "baseline": round(baseline, 4),
        "height": round(height, 2) if height != float("inf") else "∞",
    }


# ---------- период полураспада ----------

def half_life_simple(counter, peak_key):
    """Для месяцев/лет: без сглаживания."""
    peak_val = counter.get(peak_key, 0)
    if peak_val == 0:
        return None
    target = peak_val / 2
    keys = sorted(counter.keys())
    start_idx = keys.index(peak_key)
    for i in range(start_idx, len(keys)):
        if counter[keys[i]] <= target:
            return i - start_idx if i > start_idx else None
    return None


def half_life_day(counter, peak_key, window=7):
    """Для дней: скользящее среднее по window дней вперёд."""
    peak_val = counter.get(peak_key, 0)
    if peak_val == 0:
        return None
    target = peak_val / 2
    keys = sorted(counter.keys())
    start_idx = keys.index(peak_key)
    for i in range(start_idx, len(keys)):
        chunk = keys[i:i + window]
        if not chunk:
            break
        avg = sum(counter[k] for k in chunk) / len(chunk)
        if avg <= target:
            return i - start_idx if i > start_idx else None
    return None


# ---------- анализ ----------

def find_peaks(dates):
    if not dates:
        return None

    by_day   = Counter(d.strftime("%Y-%m-%d") for d in dates)
    by_month = Counter(d.strftime("%Y-%m")    for d in dates)
    by_year  = Counter(d.strftime("%Y")       for d in dates)

    for lvl, ctr in (("day", by_day), ("month", by_month), ("year", by_year)):
        for k in full_range_keys(dates, lvl):
            ctr.setdefault(k, 0)

    day_stats   = peak_stats(by_day,   len(by_day))
    month_stats = peak_stats(by_month, len(by_month))
    year_stats  = peak_stats(by_year,  len(by_year))

    return {
        "total_articles": len(dates),
        "first_date": min(dates).date().isoformat(),
        "last_date":  max(dates).date().isoformat(),
        "by_day":   day_stats,
        "by_month": month_stats,
        "by_year":  year_stats,
        "half_life_day":   half_life_day(by_day,   day_stats["peak_key"]),
        "half_life_month": half_life_simple(by_month, month_stats["peak_key"]),
        "half_life_year":  half_life_simple(by_year,  year_stats["peak_key"]),
        "dist_day":   by_day,
        "dist_month": by_month,
        "dist_year":  by_year,
    }


# ---------- вывод ----------

def print_report(name, res):
    print(f"\n{'='*64}")
    print(f"  {name}")
    print(f"{'='*64}")
    if not res:
        print("  Нет данных")
        return
    print(f"  Всего статей: {res['total_articles']}")
    print(f"  Период: {res['first_date']} … {res['last_date']}")

    for level in ("by_day", "by_month", "by_year"):
        s = res[level]
        print(f"\n  [{level}]")
        print(f"    Пик:      {s['peak_key']}  ({s['peak_value']} статей)")
        print(f"    Фон:      {s['baseline']} статей")
        print(f"    Высота:   {s['height']}x")

    print(f"\n  Период полураспада:")
    hl_day   = res["half_life_day"]
    hl_month = res["half_life_month"]
    hl_year  = res["half_life_year"]
    print(f"    дни:    {hl_day   if hl_day   is not None else 'не достигнут'}")
    print(f"    месяцы: {hl_month if hl_month is not None else 'не достигнут'}")
    print(f"    годы:   {hl_year  if hl_year  is not None else 'не достигнут'}")


# ---------- main ----------

def main(folder):
    folder = Path(folder)
    if not folder.is_dir():
        print(f"Папка не найдена: {folder}")
        return
    json_files = sorted(folder.glob("*.json"))
    if not json_files:
        print(f"В папке {folder} нет JSON-файлов")
        return
    for path in json_files:
        try:
            dates = load_articles(path)
        except Exception as e:
            print(f"Ошибка при чтении {path.name}: {e}")
            continue
        res = find_peaks(dates)
        print_report(path.name, res)


if __name__ == "__main__":
    folder = sys.argv[1] if len(sys.argv) > 1 else "."
    main(folder)