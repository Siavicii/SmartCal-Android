#!/usr/bin/env python3
"""
SmartCal AI Emergency Holiday Scanner
Fetches official Iranian news RSS feeds, discovers available Gemini models
automatically via ListModels, analyzes confirmed closure announcements,
and updates holidays.json.
"""
import datetime
import json
import os
import re
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

RSS_FEEDS = [
    ("ایرنا", "https://www.irna.ir/rss"),
    ("ایسنا", "https://www.isna.ir/rss"),
    ("خبرگزاری مهر", "https://www.mehrnews.com/rss"),
    ("خبرآنلاین", "https://www.khabaronline.ir/rss"),
]

KEYWORDS = [
    "تعطیل", "تعطیلی", "غیرحضوری", "دورکاری", "آلودگی هوا",
    "برودت هوا", "مدیریت مصرف", "ادارات", "مدارس", "کمیته اضطرار"
]

# Minimal Jalali conversion (Gregorian -> Jalali)
def div(a, b): return int(a / b)
def mod(a, b): return a - int(a / b) * b

def jal_cal(jy):
    br = [-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178]
    gy = jy + 621
    leap_j = -14
    jp = br[0]
    for i in range(1, len(br)):
        jm = br[i]
        jump = jm - jp
        if jy < jm:
            break
        leap_j += div(jump, 33) * 8 + div(mod(jump, 33), 4)
        jp = jm
    n = jy - jp
    leap_j += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
    if mod(jump, 33) == 4 and jump - n == 4:
        leap_j += 1
    leap_g = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
    march = 20 + leap_j - leap_g
    if jump - n < 6:
        n = n - jump + div(jump + 4, 33) * 33
    leap = mod(mod(n + 1, 33) - 1, 4)
    if leap == -1:
        leap = 4
    return leap, gy, march

def g2d(gy, gm, gd):
    d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) + div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
    return d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752

def g2j(gy, gm, gd):
    jdn = g2d(gy, gm, gd)
    jy = gy - 621
    leap, _, march = jal_cal(jy)
    jdn1f = g2d(gy, 3, march)
    k = jdn - jdn1f
    if k >= 0:
        if k <= 185:
            return jy, 1 + div(k, 31), mod(k, 31) + 1
        k -= 186
        return jy, 7 + div(k, 30), mod(k, 30) + 1
    jy -= 1
    k += 179
    if leap == 1:
        k += 1
    return jy, 7 + div(k, 30), mod(k, 30) + 1

def fetch_relevant_news():
    items = []
    headers = {"User-Agent": "Mozilla/5.0 (SmartCal-AI-Bot/1.0)"}
    for source_name, url in RSS_FEEDS:
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=15) as resp:
                xml_bytes = resp.read()
            root = ET.fromstring(xml_bytes)
            for item in root.findall(".//item")[:40]:
                title = (item.findtext("title") or "").strip()
                desc = re.sub(r"<[^>]+>", " ", (item.findtext("description") or "")).strip()
                text = f"{title} — {desc[:220]}"
                if any(kw in text for kw in KEYWORDS):
                    items.append(f"[{source_name}] {text}")
        except Exception as e:
            print(f"Warning: failed to fetch {source_name}: {e}")
    return items[:30]

def discover_gemini_models(api_key):
    url = f"https://generativelanguage.googleapis.com/v1beta/models?key={api_key}"
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=20) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    candidates = []
    for m in data.get("models", []):
        name = m.get("name", "")
        methods = m.get("supportedGenerationMethods", [])
        if "generateContent" in methods and "gemini" in name.lower():
            short = name.replace("models/", "")
            candidates.append(short)
    # Prioritize flash models (fast & free-tier friendly)
    flash_models = [c for c in candidates if "flash" in c.lower() and "image" not in c.lower() and "audio" not in c.lower()]
    other_models = [c for c in candidates if c not in flash_models and "image" not in c.lower() and "audio" not in c.lower()]
    ordered = flash_models + other_models
    print("Available Gemini models:", ", ".join(ordered[:6]))
    return ordered

def call_gemini(api_key, news_lines, today_jalali, today_greg):
    models = discover_gemini_models(api_key)
    if not models:
        raise RuntimeError("No generateContent-capable Gemini models found for this API key.")

    prompt = f"""
امروز {today_jalali} شمسی (برابر با {today_greg} میلادی) است.
تیتر و خلاصه خبرهای زیر از خبرگزاری‌های رسمی ایران استخراج شده است:

{chr(10).join(news_lines)}

وظیفه تو:
فقط و فقط اطلاعیه‌های **قطعی و رسمی تعطیلی اضطراری** (ادارات، بانک‌ها، مدارس یا کل کشور) را که برای امروز یا روزهای آینده صادر شده‌اند استخراج کن.
- شایعات، پیشنهادها، «احتمال تعطیلی» یا اخبار تکذیب‌شده را کاملاً نادیده بگیر.
- اگر هیچ تعطیلی قطعی و رسمی در خبرها نیست، آرایه خالی `[]` برگردان.

خروجی را دقیقاً به صورت یک آرایه JSON معتبر (بدون هیچ متن اضافی یا Markdown) با این ساختار برگردان:
[
  {{
    "date": "1405-9-2",
    "title": "تعطیلی اضطراری ادارات و مدارس",
    "reason": "افزایش شدید آلودگی هوا",
    "scope": "all",
    "cities": ["تهران"],
    "isHoliday": true,
    "source": "ایرنا"
  }}
]
نکات:
- `date` تاریخ شمسی بدون صفر اضافی باشد (مثلاً 1405-9-2).
- `scope` یکی از مقادیر `"all"` (ادارات و مدارس)، `"offices"` (ادارات) یا `"schools"` (فقط مدارس/غیرحضوری) باشد.
- `isHoliday` برای `"all"` و `"offices"` برابر `true` و برای `"schools"` برابر `false` باشد.
- اگر تعطیلی سراسری است، `cities` برابر `["سراسری"]` باشد؛ در غیر این صورت نام شهرها یا استان‌های مشمول را بنویس.
"""
    payload = json.dumps({
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"temperature": 0.1, "responseMimeType": "application/json"}
    }).encode("utf-8")

    last_err = None
    for model in models[:5]:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
        req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"}, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                res = json.loads(resp.read().decode("utf-8"))
            print(f"Using Gemini model: {model}")
            raw_text = res["candidates"][0]["content"]["parts"][0]["text"].strip()
            raw_text = re.sub(r"^```(?:json)?\s*|\s*```$", "", raw_text)
            parsed = json.loads(raw_text)
            return parsed if isinstance(parsed, list) else []
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", errors="ignore")[:200]
            last_err = f"{model} -> HTTP {e.code}: {body}"
            continue
    raise RuntimeError(f"Failed on discovered models. Last: {last_err}")

def main():
    api_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not api_key:
        print("GEMINI_API_KEY secret is not set. Skipping AI scan.")
        return

    tehran_now = datetime.datetime.utcnow() + datetime.timedelta(hours=3, minutes=30)
    jy, jm, jd = g2j(tehran_now.year, tehran_now.month, tehran_now.day)
    today_j = f"{jy}-{jm}-{jd}"
    today_g = tehran_now.strftime("%Y-%m-%d")

    news = fetch_relevant_news()
    print(f"Fetched {len(news)} relevant headlines on {today_j}")
    if not news:
        return

    try:
        detected = call_gemini(api_key, news, today_j, today_g)
    except Exception as e:
        print(f"Gemini API error: {e}")
        sys.exit(0)

    print(f"AI detected {len(detected)} confirmed closures.")
    hol_path = "holidays.json"
    with open(hol_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    emg = data.setdefault("emergency", {})
    changed = False

    for item in detected:
        dkey = str(item.get("date", "")).strip()
        if not re.match(r"^\d{4}-\d{1,2}-\d{1,2}$", dkey):
            continue
        entry = {
            "title": str(item.get("title", "تعطیلی اضطراری")).strip(),
            "reason": str(item.get("reason", "")).strip(),
            "scope": item.get("scope", "all") if item.get("scope") in ("all", "offices", "schools") else "all",
            "cities": item.get("cities") if isinstance(item.get("cities"), list) and item.get("cities") else ["سراسری"],
            "isHoliday": bool(item.get("isHoliday", True)),
            "source": str(item.get("source", "خبرگزاری رسمی")).strip(),
        }
        cur_list = emg.setdefault(dkey, [])
        if not any(x.get("title") == entry["title"] and x.get("cities") == entry["cities"] for x in cur_list):
            cur_list.append(entry)
            changed = True

    if changed:
        for path in ("holidays.json", "app/src/main/assets/www/holidays.json"):
            if os.path.exists(os.path.dirname(path) or "."):
                with open(path, "w", encoding="utf-8") as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
        print("Updated holidays.json successfully.")
    else:
        print("No changes needed.")

if __name__ == "__main__":
    main()
