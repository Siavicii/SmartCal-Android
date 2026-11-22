import os, re, json, urllib.request
from PIL import Image, ImageDraw, ImageFont

manifest_path = "app/src/main/AndroidManifest.xml"
with open(manifest_path, "r", encoding="utf-8") as f:
    manifest_xml = f.read()

mipmap_refs = sorted(set(re.findall(r"@mipmap/([a-zA-Z0-9_]+)", manifest_xml)))
res_light_dir = "app/src/main/res/mipmap-xxhdpi"
res_night_dir = "app/src/main/res/mipmap-night-xxhdpi"
os.makedirs(res_light_dir, exist_ok=True)
os.makedirs(res_night_dir, exist_ok=True)

FA_MONTHS = [
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
]
KU_MONTHS = [
    "خاکەلێوە", "گوڵان", "جۆزەردان", "پووشپەڕ", "گەلاوێژ", "خەرمانان",
    "ڕەزبەر", "گەڵاڕێزان", "سەرماوەز", "بەفرانبار", "ڕێبەندان", "ڕەشەمە"
]
EN_MONTHS = [
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
]

def to_fa_digits(n):
    return str(n).translate(str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹"))

def _download_bytes(url, timeout=15):
    req = urllib.request.Request(url, headers={"User-Agent": "SmartCal-IconGen/64.8"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read()

def _test_pillow_font(path):
    try:
        ImageFont.truetype(path, 48)
        return True
    except Exception:
        return False

def ensure_dastkhat_font():
    local_ttf = "Dastkhat-Icon.ttf"
    if os.path.exists(local_ttf) and os.path.getsize(local_ttf) > 10000 and _test_pillow_font(local_ttf):
        return local_ttf

    # 1) Discover exact font files from dastyar-io/dastkhat-font repo tree via GitHub API
    discovered_ttf = []
    discovered_woff2 = []
    try:
        tree_raw = _download_bytes("https://api.github.com/repos/dastyar-io/dastkhat-font/git/trees/main?recursive=1")
        tree = json.loads(tree_raw.decode("utf-8")).get("tree", [])
        for item in tree:
            p = item.get("path", "")
            pl = p.lower()
            if pl.endswith(".ttf") or pl.endswith(".otf"):
                discovered_ttf.append(f"https://raw.githubusercontent.com/dastyar-io/dastkhat-font/main/{p}")
            elif pl.endswith(".woff2"):
                discovered_woff2.append(f"https://raw.githubusercontent.com/dastyar-io/dastkhat-font/main/{p}")
        # Prefer Bold over Regular
        discovered_ttf.sort(key=lambda u: (0 if "bold" in u.lower() else 1, len(u)))
        discovered_woff2.sort(key=lambda u: (0 if "bold" in u.lower() else 1, len(u)))
    except Exception as e:
        print("GitHub tree lookup warning:", e)

    # Fallback direct URLs (including well-known Persian handwritten TTF repos)
    fallback_ttf = [
        "https://raw.githubusercontent.com/dastyar-io/dastkhat-font/main/dist/Dastkhat-Bold.ttf",
        "https://raw.githubusercontent.com/dastyar-io/dastkhat-font/main/dist/Dastkhat-Regular.ttf",
        "https://raw.githubusercontent.com/rastikerdar/vazirmatn/master/fonts/ttf/Vazirmatn-Bold.ttf"
    ]

    for url in discovered_ttf + fallback_ttf:
        try:
            data = _download_bytes(url)
            if len(data) > 10000:
                with open(local_ttf, "wb") as out_f:
                    out_f.write(data)
                if _test_pillow_font(local_ttf):
                    print("Loaded Dastkhat TTF from:", url)
                    return local_ttf
        except Exception:
            continue

    # 2) If repo only ships .woff2, download and convert woff2 -> ttf via fontTools
    for url in discovered_woff2:
        try:
            data = _download_bytes(url)
            if len(data) > 5000:
                w2_path = "Dastkhat-Icon.woff2"
                with open(w2_path, "wb") as out_f:
                    out_f.write(data)
                try:
                    from fontTools.ttLib import TTFont
                    tt = TTFont(w2_path)
                    tt.flavor = None
                    tt.save(local_ttf)
                    if _test_pillow_font(local_ttf):
                        print("Converted Dastkhat WOFF2 -> TTF from:", url)
                        return local_ttf
                except Exception as conv_e:
                    print("WOFF2 convert note:", conv_e)
        except Exception:
            continue

    return None

dastkhat_path = ensure_dastkhat_font()

font_black_candidates = [
    dastkhat_path,
    "Dastkhat-Bold.ttf",
    "Dastkhat-Regular.ttf",
    "Vazirmatn-Black.ttf",
    "Vazirmatn-ExtraBold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Black.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
]
font_bold_candidates = [
    dastkhat_path,
    "Dastkhat-Bold.ttf",
    "Dastkhat-Regular.ttf",
    "Vazirmatn-Bold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
]

f_black = next((p for p in font_black_candidates if p and os.path.exists(p)), None)
f_bold = next((p for p in font_bold_candidates if p and os.path.exists(p)), f_black)
using_dastkhat = bool(f_black and "dastkhat" in f_black.lower())
print(f"Using icon font: {f_black} (handwritten Dastkhat={using_dastkhat})")

font_num_1d = ImageFont.truetype(f_black, 114 if using_dastkhat else 102)
font_num_2d = ImageFont.truetype(f_black, 102 if using_dastkhat else 92)
font_month_short = ImageFont.truetype(f_bold, 40 if using_dastkhat else 34)
font_month_long = ImageFont.truetype(f_bold, 32 if using_dastkhat else 27)

def make_icon(day_str, month_str, out_path, dark_mode=False, is_rtl=True):
    S = 192
    bg_col = (15, 23, 42, 255) if dark_mode else (255, 255, 255, 255)
    num_col = (248, 250, 252, 255) if dark_mode else (15, 23, 42, 255)
    mon_col = (110, 168, 255, 255) if dark_mode else (27, 110, 243, 255)
    bar_col = (110, 168, 255, 190) if dark_mode else (27, 110, 243, 210)

    img = Image.new("RGBA", (S, S), bg_col)
    draw = ImageDraw.Draw(img)

    # Subtle minimalist top accent pill
    draw.rounded_rectangle([68, 18, 124, 24], radius=3, fill=bar_col)

    kw = {"direction": "rtl", "language": "fa"} if is_rtl else {}

    f_num = font_num_2d if len(day_str) >= 2 else font_num_1d
    nb = draw.textbbox((0, 0), day_str, font=f_num, **kw)
    nx = (S - (nb[2] - nb[0])) / 2 - nb[0]
    ny = 78 - (nb[3] - nb[1]) / 2 - nb[1]
    draw.text((nx, ny), day_str, font=f_num, fill=num_col, **kw)

    f_mon = font_month_long if len(month_str) >= 7 else font_month_short
    mb = draw.textbbox((0, 0), month_str, font=f_mon, **kw)
    mx = (S - (mb[2] - mb[0])) / 2 - mb[0]
    my = 148 - (mb[3] - mb[1]) / 2 - mb[1]
    draw.text((mx, my), month_str, font=f_mon, fill=mon_col, **kw)

    img.save(out_path, format="PNG", optimize=True)

def make_timeless_launcher_icon(out_path, dark_mode=False):
    S = 192
    bg_col = (31, 43, 59, 255)
    img = Image.new("RGBA", (S, S), bg_col)
    draw = ImageDraw.Draw(img)

    white = (255, 255, 255, 255)
    coral = (248, 113, 113, 255)
    blue = (96, 165, 250, 255)

    draw.rounded_rectangle([34, 46, 158, 158], radius=16, outline=white, width=9)
    draw.line([(34, 74), (158, 74)], fill=white, width=8)
    draw.rounded_rectangle([60, 28, 70, 56], radius=5, fill=white)
    draw.rounded_rectangle([122, 28, 132, 56], radius=5, fill=white)

    draw.rounded_rectangle([62, 92, 102, 132], radius=7, outline=coral, width=9)
    draw.rounded_rectangle([90, 104, 130, 144], radius=7, outline=blue, width=9)
    draw.line([(90, 92), (102, 92)], fill=coral, width=9)
    draw.line([(102, 92), (102, 118)], fill=coral, width=9)

    img.save(out_path, format="PNG", optimize=True)

for ref in mipmap_refs:
    light_p = os.path.join(res_light_dir, f"{ref}.png")
    night_p = os.path.join(res_night_dir, f"{ref}.png")

    m_fa = re.match(r"^ic_m(\d\d)_d(\d\d)$", ref)
    m_ku = re.match(r"^ic_km(\d\d)_d(\d\d)$", ref)
    m_en = re.match(r"^ic_gm(\d\d)_d(\d\d)$", ref)

    if m_fa:
        m_idx, d_idx = int(m_fa.group(1)), int(m_fa.group(2))
        make_icon(to_fa_digits(d_idx), FA_MONTHS[m_idx - 1], light_p, dark_mode=False, is_rtl=True)
        make_icon(to_fa_digits(d_idx), FA_MONTHS[m_idx - 1], night_p, dark_mode=True, is_rtl=True)
    elif m_ku:
        m_idx, d_idx = int(m_ku.group(1)), int(m_ku.group(2))
        make_icon(to_fa_digits(d_idx), KU_MONTHS[m_idx - 1], light_p, dark_mode=False, is_rtl=True)
        make_icon(to_fa_digits(d_idx), KU_MONTHS[m_idx - 1], night_p, dark_mode=True, is_rtl=True)
    elif m_en:
        m_idx, d_idx = int(m_en.group(1)), int(m_en.group(2))
        make_icon(str(d_idx), EN_MONTHS[m_idx - 1], light_p, dark_mode=False, is_rtl=False)
        make_icon(str(d_idx), EN_MONTHS[m_idx - 1], night_p, dark_mode=True, is_rtl=False)
    else:
        make_timeless_launcher_icon(light_p, dark_mode=False)
        make_timeless_launcher_icon(night_p, dark_mode=True)

print(f"Generated handwritten Dastkhat icons for {len(mipmap_refs)} refs (FA/KU/EN).")
