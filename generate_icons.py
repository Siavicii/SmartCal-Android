import os, re
from datetime import date
from PIL import Image, ImageDraw, ImageFont

manifest_path = "app/src/main/AndroidManifest.xml"
with open(manifest_path, "r", encoding="utf-8") as f:
    manifest_xml = f.read()

mipmap_refs = sorted(set(re.findall(r"@mipmap/([a-zA-Z0-9_]+)", manifest_xml)))
res_mipmap_dir = "app/src/main/res/mipmap-xxhdpi"
os.makedirs(res_mipmap_dir, exist_ok=True)

FA_MONTHS = [
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
]
# Python weekday(): Mon=0..Sun=6 -> Persian weekday names
FA_WEEKDAYS = ["دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه", "یکشنبه"]

def to_fa_digits(n):
    return str(n).translate(str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹"))

# Exact Jalali to Gregorian for year 1405 (1 Farvardin 1405 = 2026-03-21, Saturday)
def jalali_1405_weekday(jm, jd):
    days_before = (jm - 1) * 31 if jm <= 6 else 186 + (jm - 7) * 30
    total_offset = days_before + (jd - 1)
    # 2026-03-21 is Saturday (weekday() == 5)
    g_date = date.fromordinal(date(2026, 3, 21).toordinal() + total_offset)
    return FA_WEEKDAYS[g_date.weekday()]

font_black_candidates = [
    "Vazirmatn-Black.ttf",
    "Vazirmatn-ExtraBold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Black.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf"
]
font_bold_candidates = [
    "Vazirmatn-Bold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf"
]
font_med_candidates = [
    "Vazirmatn-SemiBold.ttf",
    "Vazirmatn-Bold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf"
]

f_black = next((p for p in font_black_candidates if os.path.exists(p)), None)
f_bold = next((p for p in font_bold_candidates if os.path.exists(p)), f_black)
f_med = next((p for p in font_med_candidates if os.path.exists(p)), f_bold)

font_wd = ImageFont.truetype(f_med, 28)
font_num_1d = ImageFont.truetype(f_black, 88)
font_num_2d = ImageFont.truetype(f_black, 80)
font_month_short = ImageFont.truetype(f_bold, 30)
font_month_long = ImageFont.truetype(f_bold, 25)

def make_icon(wd_str, day_str, month_str, out_path):
    S = 192
    # Full-bleed pure white background with NO outer border or margin
    img = Image.new("RGBA", (S, S), (255, 255, 255, 255))
    draw = ImageDraw.Draw(img)

    # 1. Top: Weekday (روز هفته)
    wb = draw.textbbox((0, 0), wd_str, font=font_wd, direction="rtl", language="fa")
    wx = (S - (wb[2] - wb[0])) / 2 - wb[0]
    wy = 34 - (wb[3] - wb[1]) / 2 - wb[1]
    draw.text((wx, wy), wd_str, font=font_wd, fill=(100, 116, 139, 255), direction="rtl", language="fa")

    # 2. Center: Giant crisp Day Number (تاریخ عددی)
    f_num = font_num_2d if len(day_str) >= 2 else font_num_1d
    nb = draw.textbbox((0, 0), day_str, font=f_num, direction="rtl", language="fa")
    nx = (S - (nb[2] - nb[0])) / 2 - nb[0]
    ny = 95 - (nb[3] - nb[1]) / 2 - nb[1]
    draw.text((nx, ny), day_str, font=f_num, fill=(15, 23, 42, 255), direction="rtl", language="fa")

    # 3. Bottom: Month Name (ماه)
    f_mon = font_month_long if len(month_str) >= 7 else font_month_short
    mb = draw.textbbox((0, 0), month_str, font=f_mon, direction="rtl", language="fa")
    mx = (S - (mb[2] - mb[0])) / 2 - mb[0]
    my = 155 - (mb[3] - mb[1]) / 2 - mb[1]
    draw.text((mx, my), month_str, font=f_mon, fill=(27, 110, 243, 255), direction="rtl", language="fa")

    img.save(out_path, format="PNG", optimize=True)

for ref in mipmap_refs:
    m = re.match(r"^ic_m(\d\d)_d(\d\d)$", ref)
    out_p = os.path.join(res_mipmap_dir, f"{ref}.png")
    if m:
        m_idx, d_idx = int(m.group(1)), int(m.group(2))
        make_icon(jalali_1405_weekday(m_idx, d_idx), to_fa_digits(d_idx), FA_MONTHS[m_idx - 1], out_p)
    else:
        make_icon(jalali_1405_weekday(8, 30), to_fa_digits(30), "آبان", out_p)

print("Generated", len(mipmap_refs), "minimalist full-bleed icons with weekday + day + month.")
