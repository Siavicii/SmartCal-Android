package com.smartcal.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import org.json.JSONObject
import java.util.Calendar

class SmartCalMarketWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_MKT = "smartcal_market_widget_prefs"
        private const val KEY_JSON = "mkt_widget_json"

        fun saveMarketData(context: Context, jsonStr: String) {
            try {
                context.getSharedPreferences(PREFS_MKT, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_JSON, jsonStr)
                    .apply()
                updateAllWidgets(context)
            } catch (_: Exception) {}
        }

        fun updateAllWidgets(context: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, SmartCalMarketWidgetProvider::class.java))
                for (id in ids) {
                    updateSingleWidget(context, mgr, id)
                }
            } catch (_: Exception) {}
        }

        private fun updateSingleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_smartcal_market)

            val themePrefs = context.getSharedPreferences(SmartCalWidgetProvider.PREFS, Context.MODE_PRIVATE)
            val themeMode = themePrefs.getString(SmartCalWidgetProvider.KEY_THEME, "auto") ?: "auto"
            val lang = themePrefs.getString(SmartCalWidgetProvider.KEY_LANG, "fa") ?: "fa"
            val calType = themePrefs.getString(SmartCalWidgetProvider.KEY_CAL, "jalali") ?: "jalali"
            val sysDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val isDark = themeMode == "dark" || (themeMode == "auto" && sysDark)

            val bgColor = if (isDark) Color.parseColor("#141923") else Color.parseColor("#FFFFFF")
            val titleColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
            val bodyColor = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#334155")
            val accentColor = if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")
            val taskColor = if (isDark) Color.parseColor("#4ADE80") else Color.parseColor("#15803D")

            views.setInt(R.id.mktWidgetRoot, "setBackgroundColor", bgColor)
            views.setTextColor(R.id.mktWidgetTitle, titleColor)
            views.setTextColor(R.id.mktWidgetDate, accentColor)
            views.setTextColor(R.id.mktWidgetRates, bodyColor)
            views.setTextColor(R.id.mktWidgetTasks, taskColor)

            val headerTitle = when (lang) {
                "ku" -> "Smart Cal • بازاڕ و ئەمڕۆ"
                "en" -> "Smart Cal • Market & Today"
                else -> "Smart Cal • بازار و امروز"
            }
            views.setTextViewText(R.id.mktWidgetTitle, headerTitle)

            val dateLabel = if (calType == "gregorian" || lang == "en") {
                val c = Calendar.getInstance()
                val m = SmartCalWidgetProvider.EN_MONTHS_SHORT.getOrElse(c.get(Calendar.MONTH)) { "" }
                "${c.get(Calendar.DAY_OF_MONTH)} $m"
            } else {
                val today = JalaliHelper.todayJalali()
                val mArr = if (lang == "ku" || calType == "kurdish") SmartCalWidgetProvider.KU_MONTHS else SmartCalWidgetProvider.FA_MONTHS
                val mName = mArr.getOrElse(today.month - 1) { "" }
                "${SmartCalWidgetProvider.toFaDigits(today.day)} $mName"
            }
            views.setTextViewText(R.id.mktWidgetDate, dateLabel)

            val defaultRatesMsg = when (lang) {
                "ku" -> "بۆ هاوکاتکردنی نرخی ڕاستەوخۆ، Smart Cal بکەرەوە"
                "en" -> "Open Smart Cal to sync live market rates"
                else -> "برای همگام‌سازی نرخ لحظه‌ای، Smart Cal را باز کنید"
            }
            val emptyTaskMsg = when (lang) {
                "ku" -> "📋 ئەمڕۆ هیچ یادخەرەوەیەکی بەپەلە نییە"
                "en" -> "📋 No urgent tasks for today"
                else -> "📋 امروز یادآور فوری ثبت نشده"
            }
            views.setTextViewText(R.id.mktWidgetRates, defaultRatesMsg)
            views.setTextViewText(R.id.mktWidgetTasks, emptyTaskMsg)

            val raw = context.getSharedPreferences(PREFS_MKT, Context.MODE_PRIVATE).getString(KEY_JSON, "") ?: ""
            if (raw.isNotBlank()) {
                try {
                    val obj = JSONObject(raw)
                    val ratesArr = obj.optJSONArray("rates")
                    val lines = StringBuilder()
                    if (ratesArr != null) {
                        for (i in 0 until minOf(ratesArr.length(), 4)) {
                            val r = ratesArr.getJSONObject(i)
                            val n = r.optString("n", "")
                            val p = r.optString("p", "")
                            val c = r.optString("c", "")
                            if (i > 0) lines.append("\n")
                            lines.append("• ").append(n).append(": ").append(p).append(" (").append(c).append(")")
                        }
                    }
                    if (lines.isNotEmpty()) {
                        views.setTextViewText(R.id.mktWidgetRates, lines.toString())
                    }
                    val taskCount = obj.optInt("taskCount", 0)
                    val firstTask = obj.optString("firstTask", "")
                    val cntStr = if (lang == "en") taskCount.toString() else SmartCalWidgetProvider.toFaDigits(taskCount)
                    val taskLine = if (taskCount > 0 && firstTask.isNotBlank()) {
                        when (lang) {
                            "ku" -> "📋 ئەمڕۆ ($cntStr): $firstTask"
                            "en" -> "📋 Today ($cntStr): $firstTask"
                            else -> "📋 امروز ($cntStr): $firstTask"
                        }
                    } else if (taskCount > 0) {
                        when (lang) {
                            "ku" -> "📋 کارەکانی ئەمڕۆ: $cntStr دانە"
                            "en" -> "📋 Today's tasks: $cntStr"
                            else -> "📋 کارهای امروز: $cntStr مورد"
                        }
                    } else {
                        emptyTaskMsg
                    }
                    views.setTextViewText(R.id.mktWidgetTasks, taskLine)
                } catch (_: Exception) {}
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context, 7401, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.mktWidgetRoot, pi)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
