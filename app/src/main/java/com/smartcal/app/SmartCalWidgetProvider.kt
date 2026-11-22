package com.smartcal.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar

class SmartCalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        private const val PREFS = "smartcal_widget_prefs"
        private const val KEY_UI = "w_ui"
        private const val KEY_THEME = "w_theme"
        private const val KEY_ACCENT = "w_accent"
        private const val KEY_CAL = "w_cal"
        private const val KEY_LANG = "w_lang"
        private const val KEY_CITY = "w_city"
        private const val KEY_WEATHER = "w_weather"
        private const val KEY_SUMMARY = "w_summary"

        private val FA_MONTHS = arrayOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )
        private val KU_MONTHS = arrayOf(
            "خاکەلێوە", "گوڵان", "جۆزەردان", "پووشپەڕ", "گەلاوێژ", "خەرمانان",
            "ڕەزبەر", "گەڵاڕێزان", "سەرماوەز", "بەفرانبار", "ڕێبەندان", "ڕەشەمە"
        )
        private val EN_MONTHS = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        private val FA_DAYS = arrayOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه")
        private val KU_DAYS = arrayOf("یەکشەممە", "دووشەممە", "سێشەممە", "چوارشەممە", "پێنجشەممە", "هەینی", "شەممە")
        private val EN_DAYS = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        private fun toFaDigits(n: Int): String {
            val map = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
            return n.toString().map { if (it in '0'..'9') map[it - '0'] else it }.joinToString("")
        }

        fun saveTheme(context: Context, ui: String?, theme: String?, accent: String?, calType: String?, lang: String?) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            if (!ui.isNullOrBlank()) prefs.putString(KEY_UI, ui)
            if (!theme.isNullOrBlank()) prefs.putString(KEY_THEME, theme)
            if (!accent.isNullOrBlank()) prefs.putString(KEY_ACCENT, accent)
            if (!calType.isNullOrBlank()) prefs.putString(KEY_CAL, calType)
            if (!lang.isNullOrBlank()) prefs.putString(KEY_LANG, lang)
            prefs.apply()
            updateAllWidgets(context)
        }

        fun saveContent(context: Context, city: String?, weather: String?, summary: String?) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            if (!city.isNullOrBlank()) prefs.putString(KEY_CITY, city)
            if (!weather.isNullOrBlank()) prefs.putString(KEY_WEATHER, weather)
            if (!summary.isNullOrBlank()) prefs.putString(KEY_SUMMARY, summary)
            prefs.apply()
            updateAllWidgets(context)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, SmartCalWidgetProvider::class.java)
                val ids = mgr.getAppWidgetIds(cn)
                for (id in ids) {
                    updateSingleWidget(context, mgr, id)
                }
            } catch (_: Exception) {}
        }

        private data class ThemePalette(
            val bgStart: Int,
            val bgEnd: Int,
            val stroke: Int,
            val strokeWidth: Float,
            val radiusDp: Float,
            val txtPri: Int,
            val txtSec: Int,
            val accent: Int
        )

        private fun resolvePalette(ui: String, isDark: Boolean, accentHex: String): ThemePalette {
            val acc = try { Color.parseColor(accentHex) } catch (_: Exception) {
                if (isDark) Color.parseColor("#6EA8FF") else Color.parseColor("#1B6EF3")
            }
            return when (ui) {
                "ios26" -> if (isDark) {
                    ThemePalette(Color.parseColor("#D8181E30"), Color.parseColor("#D81E2942"), Color.parseColor("#44FFFFFF"), 2.2f, 28f, Color.parseColor("#F8FAFC"), Color.parseColor("#CBD5E1"), acc)
                } else {
                    ThemePalette(Color.parseColor("#E6EFF6FF"), Color.parseColor("#E6F5F3FF"), Color.parseColor("#CCFFFFFF"), 2.4f, 28f, Color.parseColor("#0F172A"), Color.parseColor("#475569"), acc)
                }
                "android17" -> if (isDark) {
                    ThemePalette(Color.parseColor("#FF171C24"), Color.parseColor("#FF1E2530"), Color.parseColor("#2A94A3B8"), 1.6f, 28f, Color.parseColor("#F8FAFC"), Color.parseColor("#94A3B8"), acc)
                } else {
                    ThemePalette(Color.parseColor("#FFFFFFFF"), Color.parseColor("#FFF4F7FC"), Color.parseColor("#221B6EF3"), 1.6f, 28f, Color.parseColor("#0F172A"), Color.parseColor("#64748B"), acc)
                }
                "glass" -> if (isDark) {
                    ThemePalette(Color.parseColor("#CC141B2D"), Color.parseColor("#CC1E1B4B"), Color.parseColor("#40FFFFFF"), 2.0f, 26f, Color.parseColor("#F8FAFC"), Color.parseColor("#CBD5E1"), acc)
                } else {
                    ThemePalette(Color.parseColor("#D9E8F0FE"), Color.parseColor("#D9F3E8FF"), Color.parseColor("#BFFFFFFF"), 2.0f, 26f, Color.parseColor("#0F172A"), Color.parseColor("#475569"), acc)
                }
                "cyber" -> ThemePalette(
                    Color.parseColor("#EE13092D"), Color.parseColor("#EE071A26"), Color.parseColor("#AA8B5CF6"), 2.4f, 18f, Color.parseColor("#F8FAFC"), Color.parseColor("#A5B4FC"), acc
                )
                "minimal" -> if (isDark) {
                    ThemePalette(Color.parseColor("#FF111113"), Color.parseColor("#FF111113"), Color.parseColor("#33FFFFFF"), 1.8f, 12f, Color.parseColor("#FAFAFA"), Color.parseColor("#A1A1AA"), acc)
                } else {
                    ThemePalette(Color.parseColor("#FFFFFFFF"), Color.parseColor("#FFFFFFFF"), Color.parseColor("#26000000"), 1.8f, 12f, Color.parseColor("#09090B"), Color.parseColor("#52525B"), acc)
                }
                "bento" -> if (isDark) {
                    ThemePalette(Color.parseColor("#FF161B24"), Color.parseColor("#FF161B24"), Color.parseColor("#22FFFFFF"), 1.5f, 30f, Color.parseColor("#F8FAFC"), Color.parseColor("#94A3B8"), acc)
                } else {
                    ThemePalette(Color.parseColor("#FFFFFFFF"), Color.parseColor("#FFF8FAFC"), Color.parseColor("#1A0F172A"), 1.5f, 30f, Color.parseColor("#0F172A"), Color.parseColor("#64748B"), acc)
                }
                "neumorph" -> if (isDark) {
                    ThemePalette(Color.parseColor("#FF1A1E26"), Color.parseColor("#FF14171E"), Color.parseColor("#18FFFFFF"), 1.8f, 24f, Color.parseColor("#F1F5F9"), Color.parseColor("#94A3B8"), acc)
                } else {
                    ThemePalette(Color.parseColor("#FFE8ECF2"), Color.parseColor("#FFDFE4EC"), Color.parseColor("#CCFFFFFF"), 2.0f, 24f, Color.parseColor("#1E293B"), Color.parseColor("#64748B"), acc)
                }
                "persian" -> if (isDark) {
                    ThemePalette(Color.parseColor("#FF1C1712"), Color.parseColor("#FF261E16"), Color.parseColor("#55B4823C"), 2.0f, 22f, Color.parseColor("#FDF8F0"), Color.parseColor("#D6C3A5"), acc)
                } else {
                    ThemePalette(Color.parseColor("#FFFCF9F2"), Color.parseColor("#FFF3EBD9"), Color.parseColor("#55B4823C"), 2.0f, 22f, Color.parseColor("#2C2217"), Color.parseColor("#78634B"), acc)
                }
                "nordic" -> if (isDark) {
                    ThemePalette(Color.parseColor("#FF1E293B"), Color.parseColor("#FF0F172A"), Color.parseColor("#3394A3B8"), 1.6f, 20f, Color.parseColor("#F8FAFC"), Color.parseColor("#94A3B8"), acc)
                } else {
                    ThemePalette(Color.parseColor("#FFF8FAFC"), Color.parseColor("#FFE8EEF2"), Color.parseColor("#220F172A"), 1.6f, 20f, Color.parseColor("#0F172A"), Color.parseColor("#475569"), acc)
                }
                else -> if (isDark) {
                    ThemePalette(Color.parseColor("#FF161B26"), Color.parseColor("#FF111620"), Color.parseColor("#24FFFFFF"), 1.5f, 24f, Color.parseColor("#F8FAFC"), Color.parseColor("#94A3B8"), acc)
                } else {
                    ThemePalette(Color.parseColor("#FFFFFFFF"), Color.parseColor("#FFF4F7FB"), Color.parseColor("#180F172A"), 1.5f, 24f, Color.parseColor("#0F172A"), Color.parseColor("#64748B"), acc)
                }
            }
        }

        private fun renderThemeCardBitmap(palette: ThemePalette): Bitmap {
            val w = 720
            val h = 340
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val pad = palette.strokeWidth + 2f
            val rect = RectF(pad, pad, w - pad, h - pad)
            val rad = palette.radiusDp * 2.2f

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), palette.bgStart, palette.bgEnd, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(rect, rad, rad, fillPaint)

            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = palette.strokeWidth * 1.4f
                color = palette.stroke
            }
            canvas.drawRoundRect(rect, rad, rad, strokePaint)
            return bmp
        }

        private fun updateSingleWidget(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val ui = prefs.getString(KEY_UI, "android17") ?: "android17"
            val themeMode = prefs.getString(KEY_THEME, "auto") ?: "auto"
            val accentHex = prefs.getString(KEY_ACCENT, "#1B6EF3") ?: "#1B6EF3"
            val calType = prefs.getString(KEY_CAL, "jalali") ?: "jalali"
            val lang = prefs.getString(KEY_LANG, "fa") ?: "fa"
            val city = prefs.getString(KEY_CITY, "تهران") ?: "تهران"
            val weather = prefs.getString(KEY_WEATHER, "") ?: ""
            val summary = prefs.getString(KEY_SUMMARY, "") ?: ""

            val sysDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val isDark = themeMode == "dark" || (themeMode == "auto" && sysDark)
            val palette = resolvePalette(ui, isDark, accentHex)

            val cal = Calendar.getInstance()
            val gy = cal.get(Calendar.YEAR)
            val gm = cal.get(Calendar.MONTH) + 1
            val gd = cal.get(Calendar.DAY_OF_MONTH)
            val dow = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            val jalali = JalaliHelper.gregorianToJalali(gy, gm, gd)

            val dayNumStr: String
            val monthYearStr: String
            val subDateStr: String

            when {
                calType == "gregorian" || lang == "en" -> {
                    dayNumStr = gd.toString()
                    monthYearStr = "${EN_MONTHS[(gm - 1).coerceIn(0, 11)]} $gy"
                    subDateStr = "${EN_DAYS[dow]} • ${jalali.day} ${FA_MONTHS[(jalali.month - 1).coerceIn(0, 11)]}"
                }
                calType == "kurdish" || lang == "ku" -> {
                    val kuYear = jalali.year + 1321
                    dayNumStr = toFaDigits(jalali.day)
                    monthYearStr = "${KU_MONTHS[(jalali.month - 1).coerceIn(0, 11)]} ${toFaDigits(kuYear)}"
                    subDateStr = "${KU_DAYS[dow]} • $gd ${EN_MONTHS[(gm - 1).coerceIn(0, 11)]} $gy"
                }
                else -> {
                    dayNumStr = toFaDigits(jalali.day)
                    monthYearStr = "${FA_MONTHS[(jalali.month - 1).coerceIn(0, 11)]} ${toFaDigits(jalali.year)}"
                    subDateStr = "${FA_DAYS[dow]} • $gd ${EN_MONTHS[(gm - 1).coerceIn(0, 11)]} $gy"
                }
            }

            // Adaptive sizing for One UI 9 & other launchers (compact 4x1 vs full 4x2)
            val opts = mgr.getAppWidgetOptions(appWidgetId)
            val minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            val isCompactHeight = minH in 1..92
            val isCompactWidth = minW in 1..185

            val views = RemoteViews(context.packageName, R.layout.widget_smartcal)
            views.setImageViewBitmap(R.id.widgetBgImage, renderThemeCardBitmap(palette))

            views.setTextViewText(R.id.widgetDayNum, dayNumStr)
            views.setTextColor(R.id.widgetDayNum, palette.accent)

            views.setTextViewText(R.id.widgetMonthYear, monthYearStr)
            views.setTextColor(R.id.widgetMonthYear, palette.txtPri)

            views.setTextViewText(R.id.widgetSubDate, subDateStr)
            views.setTextColor(R.id.widgetSubDate, palette.txtSec)

            if (weather.isNotBlank() && !isCompactWidth) {
                views.setViewVisibility(R.id.widgetWeatherWrap, View.VISIBLE)
                views.setTextViewText(R.id.widgetWeather, weather)
                views.setTextColor(R.id.widgetWeather, palette.txtPri)
                views.setTextViewText(R.id.widgetCity, city)
                views.setTextColor(R.id.widgetCity, palette.txtSec)
            } else {
                views.setViewVisibility(R.id.widgetWeatherWrap, View.GONE)
            }

            if (isCompactHeight) {
                views.setViewVisibility(R.id.widgetSummary, View.GONE)
            } else {
                views.setViewVisibility(R.id.widgetSummary, View.VISIBLE)
                val fallbackSummary = when {
                    lang == "en" -> "Smart Cal • Tap to open"
                    lang == "ku" -> "ڕۆژژمێری زیرەکی گشتی Smart Cal"
                    else -> "تقویم هوشمند جامع Smart Cal"
                }
                views.setTextViewText(R.id.widgetSummary, if (summary.isNotBlank()) summary else fallbackSummary)
                views.setTextColor(R.id.widgetSummary, palette.txtSec)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 6460, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pi)

            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
