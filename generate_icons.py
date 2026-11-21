import os, re
from datetime import date
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

FA_WEEKDAYS = ["دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه", "یکشنبه"]
KU_WEEKDAYS = ["دووشەممە", "سێشەممە", "چوارشەممە", "پێنجشەممە", "هەینی", "شەممە", "یەکشەممە"]
EN_WEEKDAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]

def to_fa_digits(n):
    return str(n).translate(str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹"))

def jalali_1405_gdate(jm, jd):
    days_before = (jm - 1) * 31 if jm <= 6 else 186 + (jm - 7) * 30
    total_offset = days_before + (jd - 1)
    return date.fromordinal(date(2026, 3, 21).toordinal() + total_offset)

def greg_2026_weekday(gm, gd):
    try:
        return EN_WEEKDAYS[date(2026, gm, gd).weekday()]
    except ValueError:
        return EN_WEEKDAYS[date(2028, gm, gd).weekday()]

font_black_candidates = [
    "Vazirmatn-Black.ttf",
    "Vazirmatn-ExtraBold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Black.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
]
font_bold_candidates = [
    "Vazirmatn-Bold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
]
font_med_candidates = [
    "Vazirmatn-SemiBold.ttf",
    "Vazirmatn-Bold.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
]

f_black = next((p for p in font_black_candidates if os.path.exists(p)), None)
f_bold = next((p for p in font_bold_candidates if os.path.exists(p)), f_black)
f_med = next((p for p in font_med_candidates if os.path.exists(p)), f_bold)

font_wd = ImageFont.truetype(f_med, 26)
font_num_1d = ImageFont.truetype(f_black, 88)
font_num_2d = ImageFont.truetype(f_black, 80)
font_month_short = ImageFont.truetype(f_bold, 30)
font_month_long = ImageFont.truetype(f_bold, 24)

def make_icon(wd_str, day_str, month_str, out_path, dark_mode=False, is_rtl=True):
    S = 192
    bg_col = (15, 23, 42, 255) if dark_mode else (255, 255, 255, 255)
    wd_col = (148, 163, 184, 255) if dark_mode else (100, 116, 139, 255)
    num_col = (248, 250, 252, 255) if dark_mode else (15, 23, 42, 255)
    mon_col = (110, 168, 255, 255) if dark_mode else (27, 110, 243, 255)

    img = Image.new("RGBA", (S, S), bg_col)
    draw = ImageDraw.Draw(img)

    kw = {"direction": "rtl", "language": "fa"} if is_rtl else {}

    wb = draw.textbbox((0, 0), wd_str, font=font_wd, **kw)
    wx = (S - (wb[2] - wb[0])) / 2 - wb[0]
    wy = 34 - (wb[3] - wb[1]) / 2 - wb[1]
    draw.text((wx, wy), wd_str, font=font_wd, fill=wd_col, **kw)

    f_num = font_num_2d if len(day_str) >= 2 else font_num_1d
    nb = draw.textbbox((0, 0), day_str, font=f_num, **kw)
    nx = (S - (nb[2] - nb[0])) / 2 - nb[0]
    ny = 95 - (nb[3] - nb[1]) / 2 - nb[1]
    draw.text((nx, ny), day_str, font=f_num, fill=num_col, **kw)

    f_mon = font_month_long if len(month_str) >= 7 else font_month_short
    mb = draw.textbbox((0, 0), month_str, font=f_mon, **kw)
    mx = (S - (mb[2] - mb[0])) / 2 - mb[0]
    my = 155 - (mb[3] - mb[1]) / 2 - mb[1]
    draw.text((mx, my), month_str, font=f_mon, fill=mon_col, **kw)

    img.save(out_path, format="PNG", optimize=True)

for ref in mipmap_refs:
    light_p = os.path.join(res_light_dir, f"{ref}.png")
    night_p = os.path.join(res_night_dir, f"{ref}.png")

    m_fa = re.match(r"^ic_m(\d\d)_d(\d\d)$", ref)
    m_ku = re.match(r"^ic_km(\d\d)_d(\d\d)$", ref)
    m_en = re.match(r"^ic_gm(\d\d)_d(\d\d)$", ref)

    if m_fa:
        m_idx, d_idx = int(m_fa.group(1)), int(m_fa.group(2))
        gd = jalali_1405_gdate(m_idx, d_idx)
        wd = FA_WEEKDAYS[gd.weekday()]
        make_icon(wd, to_fa_digits(d_idx), FA_MONTHS[m_idx - 1], light_p, dark_mode=False, is_rtl=True)
        make_icon(wd, to_fa_digits(d_idx), FA_MONTHS[m_idx - 1], night_p, dark_mode=True, is_rtl=True)
    elif m_ku:
        m_idx, d_idx = int(m_ku.group(1)), int(m_ku.group(2))
        gd = jalali_1405_gdate(m_idx, d_idx)
        wd = KU_WEEKDAYS[gd.weekday()]
        make_icon(wd, to_fa_digits(d_idx), KU_MONTHS[m_idx - 1], light_p, dark_mode=False, is_rtl=True)
        make_icon(wd, to_fa_digits(d_idx), KU_MONTHS[m_idx - 1], night_p, dark_mode=True, is_rtl=True)
    elif m_en:
        m_idx, d_idx = int(m_en.group(1)), int(m_en.group(2))
        wd = greg_2026_weekday(m_idx, d_idx)
        make_icon(wd, str(d_idx), EN_MONTHS[m_idx - 1], light_p, dark_mode=False, is_rtl=False)
        make_icon(wd, str(d_idx), EN_MONTHS[m_idx - 1], night_p, dark_mode=True, is_rtl=False)
    else:
        gd = jalali_1405_gdate(8, 30)
        wd = FA_WEEKDAYS[gd.weekday()]
        make_icon(wd, to_fa_digits(30), "آبان", light_p, dark_mode=False, is_rtl=True)
        make_icon(wd, to_fa_digits(30), "آبان", night_p, dark_mode=True, is_rtl=True)

print(f"Generated light & night icons for {len(mipmap_refs)} refs (FA/KU/EN).")
