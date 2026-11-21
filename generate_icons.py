import os, re
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

def to_fa_digits(n):
    return str(n).translate(str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹"))

font_candidates = [
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Black.ttf",
    "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
]
font_black = next((p for p in font_candidates if os.path.exists(p)), None)
font_bold = "/usr/share/fonts/truetype/noto/NotoSansArabic-Bold.ttf"
if not os.path.exists(font_bold):
    font_bold = font_black

font_num_1d = ImageFont.truetype(font_black, 96)
font_num_2d = ImageFont.truetype(font_black, 86)
font_month_short = ImageFont.truetype(font_bold, 34)
font_month_long = ImageFont.truetype(font_bold, 28)

def make_icon(day_str, month_str, out_path):
    S = 192
    img = Image.new("RGBA", (S, S), (244, 246, 250, 255))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([8, 8, S - 8, S - 8], radius=40, fill=(255, 255, 255, 255), outline=(27, 110, 243, 55), width=3)

    f_num = font_num_2d if len(day_str) >= 2 else font_num_1d
    nb = draw.textbbox((0, 0), day_str, font=f_num, direction="rtl", language="fa")
    draw.text(((S - (nb[2] - nb[0])) / 2 - nb[0], 76 - (nb[3] - nb[1]) / 2 - nb[1]), day_str, font=f_num, fill=(15, 23, 42, 255), direction="rtl", language="fa")

    f_mon = font_month_long if len(month_str) >= 7 else font_month_short
    mb = draw.textbbox((0, 0), month_str, font=f_mon, direction="rtl", language="fa")
    draw.text(((S - (mb[2] - mb[0])) / 2 - mb[0], 148 - (mb[3] - mb[1]) / 2 - mb[1]), month_str, font=f_mon, fill=(27, 110, 243, 255), direction="rtl", language="fa")

    img.save(out_path, format="PNG", optimize=True)

for ref in mipmap_refs:
    m = re.match(r"^ic_m(\d\d)_d(\d\d)$", ref)
    out_p = os.path.join(res_mipmap_dir, f"{ref}.png")
    if m:
        make_icon(to_fa_digits(int(m.group(2))), FA_MONTHS[int(m.group(1)) - 1], out_p)
    else:
        make_icon(to_fa_digits(30), "آبان", out_p)

print("Generated", len(mipmap_refs), "icons successfully.")
