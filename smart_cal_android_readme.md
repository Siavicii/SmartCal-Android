<p align="center">
  <h1>📅 Smart Cal — Android</h1>
  <p><strong>A Smart, Lightweight, Multi-Calendar Android App (Persian, Kurdish & Gregorian)</strong></p>
  <p>
    <a href="#english">🇬🇧 English</a> •
    <a href="#persian">🇮🇷 فارسی</a>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Version-v64.2-blue?style=for-the-badge" alt="Version" />
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
    <img src="https://img.shields.io/badge/Stack-Kotlin%20%2B%20WebView-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Stack" />
    <img src="https://img.shields.io/badge/Languages-FA%20%7C%20KU%20%7C%20EN-orange?style=for-the-badge" alt="Languages" />
  </p>
</p>

---

<a id="english"></a>

## 🇬🇧 Overview

**Smart Cal** is a fast, modern, all-in-one Android calendar application supporting **Persian (Jalali)**, **Kurdish (Sorani)**, and **Gregorian** calendar systems. It combines dynamic home-screen app icons, live weather forecasts, real-time financial & gold/currency rates, background native alarms, and optional two-way Google Calendar synchronization inside a fluid, customizable interface.

---

## 📸 Screenshots

> 💡 *Tip: Create a `screenshots/` folder in your repository root and upload your app screenshots with the filenames below (`home.png`, `styles.png`, `calendar.png`, `settings.png`) so they appear automatically here.*

| Home & Calendar | Themes & UI Styles | Weather & Markets | Settings & Sync |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/home.png" width="200" alt="Home Screen" /> | <img src="screenshots/styles.png" width="200" alt="UI Styles" /> | <img src="screenshots/markets.png" width="200" alt="Weather & Financial Rates" /> | <img src="screenshots/settings.png" width="200" alt="Settings & Google Sync" /> |

---

## ✨ Key Features

- **🌐 Tri-Calendar & Tri-Language Support:** Seamlessly switch between **Persian (Jalali)**, **Kurdish (Sorani)**, and **English (Gregorian)** with consistent localization across the entire UI.
- **🔄 Dynamic App Icon (Future-Proof):** Automatically updates the launcher icon at midnight (using `AlarmManager.RTC_WAKEUP`) to match the current day, month, selected calendar/language, and system Dark/Light mode—designed without year-dependent weekday labels so it never expires.
- **🔔 Persistent Status Bar Date:** Displays the current day number persistently in the Android status bar and automatically refreshes at midnight even when the app is closed.
- **📆 Optional Google Calendar Sync:** Native two-way sync via Android's `CalendarContract` to read and create events directly in your device's Google Calendar (no extra login required, disabled by default until enabled by the user).
- **⏰ Native Background Alarms:** Reliable, exact-time reminder notifications powered by `AlarmManager` and `AlarmReceiver`, working even when the app is completely closed.
- **🎨 8 Modern UI Styles & Smooth Launch:** Includes *iOS 26 Liquid Glass*, *Android 17 Expressive*, *Classic*, *Glass*, *Bento*, *Swiss Minimal*, and more—paired with a hardware-accelerated 60fps launch animation and automatic Dark/Light theme support.
- **🌦️ Live Weather & Financial Tracking:** Real-time weather conditions, air-quality alerts, and live gold, coin, and currency rates (TGJU & global markets) fetched natively without CORS restrictions.
- **🚀 Smart GitHub Auto-Updater:** Numeric semantic version comparison against GitHub Releases to notify you only when a newer version is available, plus a manual check button in Settings.

---

## 📥 Download & Installation

1. Go to the [**Releases**](https://github.com/Siavicii/SmartCal-Android/releases/latest) page.
2. Download the latest `.apk` file and install it on your Android device.
3. Future updates will be detected automatically inside the app.

---

## 🏗️ Project Structure

```text
SmartCal-Android/
├── generate_icons.py                        # Generates Day & Night dynamic icons for all 3 calendar systems
├── app/
│   ├── build.gradle.kts                     # Build config & app version (v64.2 / versionCode 73)
│   └── src/main/
│       ├── AndroidManifest.xml              # Permissions, receivers, and dynamic icon activity-aliases
│       ├── assets/www/                      # Web UI core (index.html, manifest.json, sw.js, version.json)
│       └── java/com/smartcal/app/
│           ├── MainActivity.kt              # WebView lifecycle, JS bridge, CalendarContract & GitHub updater
│           ├── IconUpdater.kt               # Dynamic icon switching, midnight scheduler & status bar notification
│           ├── DateChangeReceiver.kt        # Midnight broadcast receiver for icon & status bar updates
│           ├── AlarmReceiver.kt             # Native background alarm receiver for timed reminders
│           └── JalaliHelper.kt              # Jalali, Kurdish, and Gregorian date conversion utilities
```

---

## 🛠️ Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/Siavicii/SmartCal-Android.git
   cd SmartCal-Android
   ```
2. Generate the dynamic icon resources (handled automatically if building via GitHub Actions):
   ```bash
   python3 generate_icons.py
   ```
3. Open in **Android Studio** or build via Gradle:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 🔐 Permissions

| Permission | Purpose |
| :--- | :--- |
| `INTERNET` | Fetching weather, live financial rates, and checking GitHub Releases |
| `POST_NOTIFICATIONS` | Displaying the persistent status bar date and reminder alerts |
| `READ_CALENDAR` / `WRITE_CALENDAR` | Optional — only requested if you enable Google/Device Calendar sync |
| `SCHEDULE_EXACT_ALARM` / `RECEIVE_BOOT_COMPLETED` | Triggering timed reminders and updating the app icon at midnight |

---

<br />

<a id="persian"></a>
<div dir="rtl">

## 🇮🇷 معرفی برنامه (فارسی)

**Smart Cal** یک اپلیکیشن تقویم هوشمند، سبک و همه‌کاره برای اندروید است که تقویم‌های **شمسی، کوردی و میلادی** را در کنار آب‌وهوای زنده، نرخ لحظه‌ای طلا و ارز، یادآور بومی و همگام‌سازی با تقویم گوگل در یک رابط کاربری مدرن و روان گرد هم آورده است.

---

## 📸 تصاویر برنامه

> 💡 *راهنما: کافی است یک پوشه به نام `screenshots` در ریشه مخزن گیت‌هاب بسازید و تصاویر اسکرین‌شات برنامه را با نام‌های `home.png`، `styles.png`، `markets.png` و `settings.png` در آن آپلود کنید تا به‌صورت خودکار در جدول زیر نمایش داده شوند.*

| صفحه اصلی و تقویم | سبک‌های طراحی | آب‌وهوا و بازار مالی | تنظیمات و همگام‌سازی |
| :---: | :---: | :---: | :---: |
| <img src="screenshots/home.png" width="200" alt="صفحه اصلی" /> | <img src="screenshots/styles.png" width="200" alt="سبک‌های طراحی" /> | <img src="screenshots/markets.png" width="200" alt="آب‌وهوا و نرخ ارز" /> | <img src="screenshots/settings.png" width="200" alt="تنظیمات" /> |

---

## ✨ امکانات کلیدی

- **🌐 پشتیبانی کامل از ۳ تقویم و ۳ زبان:** جابه‌جایی سریع میان تقویم‌های **شمسی (فارسی)**، **کوردی (سورانی)** و **میلادی (انگلیسی)** با ترجمه یکدست تمام بخش‌ها.
- **🔄 آیکون پویای هوشمند (بدون تاریخ انقضا):** تغییر خودکار آیکون برنامه روی صفحه اصلی گوشی در نیمه‌شب متناسب با روز، ماه، زبان/تقویم انتخابی و حالت شب/روز سیستم (بدون وابستگی به نام روز هفته تا برای همیشه معتبر بماند).
- **🔔 اعلان همیشگی تاریخ در نوار وضعیت (Status Bar):** نمایش دائمی عدد روز در استاتوس‌بار با به‌روزرسانی خودکار در ساعت ۱۲ شب بدون نیاز به باز کردن برنامه.
- **📆 همگام‌سازی اختیاری با تقویم گوگل و گوشی:** اتصال بومی و دوطرفه از طریق `CalendarContract` برای مشاهده و ثبت رویدادها در تقویم شخصی کاربر (بدون نیاز به لاگین مجدد).
- **⏰ یادآور و آلارم بومی پس‌زمینه:** اجرای دقیق اعلان یادداشت‌های ساعت‌دار از طریق `AlarmManager` حتی در صورت بسته بودن کامل برنامه.
- **🎨 ۸ سبک طراحی مدرن و شروع بدون لگ:** شامل *iOS 26 Liquid Glass*، *Android 17 Expressive*، *Bento*، *Swiss Minimal* و... به همراه انیمیشن شروع ۶۰ فریم و هماهنگی خودکار با حالت شب و روز اندروید.
- **🌦️ آب‌وهوا و بازار مالی زنده:** نمایش وضعیت آب‌وهوا، هشدارهای جوی و نرخ لحظه‌ای طلا، سکه و ارز با ارتباط مستقیم بومی (بدون محدودیت CORS).
- **🚀 بروزرسانی خودکار از گیت‌هاب:** مقایسه دقیق عددی نسخه‌ها در GitHub Releases به همراه دکمه بررسی دستی در تنظیمات.

---

## 📥 نصب نسخه نهایی (APK)

1. وارد بخش [**Releases**](https://github.com/Siavicii/SmartCal-Android/releases/latest) مخزن شوید.
2. جدیدترین فایل `.apk` را دانلود و روی گوشی اندرویدی خود نصب کنید.
3. برای دریافت بروزرسانی‌های بعدی، برنامه به‌صورت خودکار نسخه جدید را به شما اطلاع می‌دهد.

---

## 🛠️ راهنمای ساخت (Build)

1. مخزن را کلون کنید:
   ```bash
   git clone https://github.com/Siavicii/SmartCal-Android.git
   cd SmartCal-Android
   ```
2. پیش از بیلد، آیکون‌های پویا را با پایتون تولید کنید (در صورت استفاده از GitHub Actions، این مرحله به‌صورت خودکار انجام می‌شود):
   ```bash
   python3 generate_icons.py
   ```
3. پروژه را با **Android Studio** باز کنید یا با Gradle خروجی بگیرید:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 🔐 مجوزها (Permissions)

| مجوز | کاربرد |
| :--- | :--- |
| `INTERNET` | دریافت آب‌وهوا، نرخ طلا و ارز و بررسی بروزرسانی |
| `POST_NOTIFICATIONS` | نمایش اعلان همیشگی تاریخ در نوار وضعیت و یادآورها |
| `READ_CALENDAR` / `WRITE_CALENDAR` | اختیاری — صرفاً در صورت فعال‌سازی همگام‌سازی تقویم گوگل توسط کاربر |
| `SCHEDULE_EXACT_ALARM` / `RECEIVE_BOOT_COMPLETED` | اجرای دقیق یادآورها و تغییر خودکار آیکون در نیمه‌شب |

---

<p align="center">
  ساخته‌شده با ❤️ برای تجربه‌ای سریع، زیبا و هوشمند از تقویم
</p>

</div>