package com.smartcal.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.widget.RemoteViews
import java.util.Calendar

class SmartCalMonthWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, id)
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val prefs = context.getSharedPreferences(SmartCalWidgetProvider.PREFS, Context.MODE_PRIVATE)
        val curOffset = prefs.getInt(KEY_MONTH_OFFSET, 0)
        when (action) {
            ACTION_PREV_MONTH -> {
                prefs.edit().putInt(KEY_MONTH_OFFSET, curOffset - 1).apply()
                updateAllWidgets(context)
            }
            ACTION_NEXT_MONTH -> {
                prefs.edit().putInt(KEY_MONTH_OFFSET, curOffset + 1).apply()
                updateAllWidgets(context)
            }
            ACTION_RESET_TODAY -> {
                prefs.edit().putInt(KEY_MONTH_OFFSET, 0).apply()
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        private const val ACTION_PREV_MONTH = "com.smartcal.app.WIDGET_PREV_MONTH"
        private const val ACTION_NEXT_MONTH = "com.smartcal.app.WIDGET_NEXT_MONTH"
        private const val ACTION_RESET_TODAY = "com.smartcal.app.WIDGET_RESET_TODAY"
        private const val KEY_MONTH_OFFSET = "w_month_offset"

        private val FA_WD_SHORT = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        private val KU_WD_SHORT = arrayOf("ش", "ی", "د", "س", "چ", "پ", "هـ")
        private val EN_WD_SHORT = arrayOf("S", "M", "T", "W", "T", "F", "S")

        fun resetAndUpdateAll(context: Context) {
            context.getSharedPreferences(SmartCalWidgetProvider.PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MONTH_OFFSET, 0).apply()
            updateAllWidgets(context)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, SmartCalMonthWidgetProvider::class.java)
                val ids = mgr.getAppWidgetIds(cn)
                for (id in ids) {
                    updateSingleWidget(context, mgr, id)
                }
            } catch (_: Exception) {}
        }

        private fun renderMonthGridBitmap(
            palette: SmartCalWidgetProvider.Companion.ThemePalette,
            headers: Array<String>,
            isRtl: Boolean,
            startCol: Int,
            daysInMonth: Int,
            todayDay: Int,
            weekendCol: Int,
            useFaDigits: Boolean
        ): Bitmap {
            val w = 980
            val h = 640
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            val cols = 7
            val colW = w.toFloat() / cols
            val headerH = 74f
            val totalCells = startCol + daysInMonth
            val rows = if (totalCells > 35) 6 else 5
            val rowH = (h - headerH - 12f) / rows

            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 34f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = if (rows == 6) 38f else 42f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val todayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = if (rows == 6) 38f else 42f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                color = Color.WHITE
            }
            val todayCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = palette.accent
            }
            val bounds = Rect()

            // Draw weekday headers
            for (c in 0 until cols) {
                val drawCol = if (isRtl) (cols - 1 - c) else c
                val cx = drawCol * colW + colW / 2f
                headPaint.color = if (c == weekendCol) palette.weekend else palette.txtSec
                val label = headers[c]
                headPaint.getTextBounds(label, 0, label.length, bounds)
                canvas.drawText(label, cx, headerH / 2f - bounds.exactCenterY(), headPaint)
            }

            // Draw subtle divider line
            val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.stroke
                strokeWidth = 2f
            }
            canvas.drawLine(18f, headerH, w - 18f, headerH, divPaint)

            // Draw month days
            val circleRadius = minOf(colW, rowH) * 0.39f
            for (d in 1..daysInMonth) {
                val idx = startCol + (d - 1)
                val r = idx / cols
                val c = idx % cols
                val drawCol = if (isRtl) (cols - 1 - c) else c
                val cx = drawCol * colW + colW / 2f
                val cy = headerH + 6f + r * rowH + rowH / 2f
                val numStr = if (useFaDigits) SmartCalWidgetProvider.toFaDigits(d) else d.toString()

                if (d == todayDay) {
                    canvas.drawCircle(cx, cy, circleRadius, todayCirclePaint)
                    todayTextPaint.getTextBounds(numStr, 0, numStr.length, bounds)
                    canvas.drawText(numStr, cx, cy - bounds.exactCenterY(), todayTextPaint)
                } else {
                    dayPaint.color = if (c == weekendCol) palette.weekend else palette.txtPri
                    dayPaint.getTextBounds(numStr, 0, numStr.length, bounds)
                    canvas.drawText(numStr, cx, cy - bounds.exactCenterY(), dayPaint)
                }
            }
            return bmp
        }

        private fun makeActionPi(context: Context, action: String, reqCode: Int): PendingIntent {
            val i = Intent(context, SmartCalMonthWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun updateSingleWidget(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(SmartCalWidgetProvider.PREFS, Context.MODE_PRIVATE)
            val calType = prefs.getString(SmartCalWidgetProvider.KEY_CAL, "jalali") ?: "jalali"
            val lang = prefs.getString(SmartCalWidgetProvider.KEY_LANG, "fa") ?: "fa"
            val monthOffset = prefs.getInt(KEY_MONTH_OFFSET, 0)
            val palette = SmartCalWidgetProvider.resolvePalette(context)

            val now = Calendar.getInstance()
            val curGy = now.get(Calendar.YEAR)
            val curGm = now.get(Calendar.MONTH) + 1
            val curGd = now.get(Calendar.DAY_OF_MONTH)
            val curJ = JalaliHelper.gregorianToJalali(curGy, curGm, curGd)

            val isGreg = (calType == "gregorian" || lang == "en")
            val isKu = (!isGreg && (calType == "kurdish" || lang == "ku"))

            val titleStr: String
            val startCol: Int
            val daysInMonth: Int
            val todayDay: Int
            val headers: Array<String>
            val isRtl: Boolean
            val weekendCol: Int
            val useFaDigits: Boolean

            if (isGreg) {
                val targetCal = Calendar.getInstance().apply {
                    set(curGy, curGm - 1, 1)
                    add(Calendar.MONTH, monthOffset)
                }
                val ty = targetCal.get(Calendar.YEAR)
                val tm = targetCal.get(Calendar.MONTH) + 1
                titleStr = "${SmartCalWidgetProvider.EN_MONTHS_FULL[(tm - 1).coerceIn(0, 11)]} $ty"
                startCol = (targetCal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6) // Sunday = 0
                daysInMonth = JalaliHelper.gregorianMonthLength(ty, tm)
                todayDay = if (ty == curGy && tm == curGm) curGd else -1
                headers = EN_WD_SHORT
                isRtl = false
                weekendCol = 0 // Sunday
                useFaDigits = false
            } else {
                // Jalali / Kurdish month offset calculation
                val totalMonths = (curJ.year * 12) + (curJ.month - 1) + monthOffset
                val ty = totalMonths / 12
                val tm = (totalMonths % 12) + 1
                val firstG = JalaliHelper.jalaliToGregorian(ty, tm, 1)
                val firstCal = Calendar.getInstance().apply { set(firstG.year, firstG.month - 1, firstG.day) }
                // Calendar.SATURDAY == 7 -> 0, SUNDAY == 1 -> 1, ..., FRIDAY == 6 -> 6
                val dow = firstCal.get(Calendar.DAY_OF_WEEK)
                startCol = if (dow == Calendar.SATURDAY) 0 else dow
                daysInMonth = JalaliHelper.jalaliMonthLength(ty, tm)
                todayDay = if (ty == curJ.year && tm == curJ.month) curJ.day else -1
                if (isKu) {
                    val kuY = ty + 1321
                    titleStr = "${SmartCalWidgetProvider.KU_MONTHS[(tm - 1).coerceIn(0, 11)]} ${SmartCalWidgetProvider.toFaDigits(kuY)}"
                    headers = KU_WD_SHORT
                } else {
                    titleStr = "${SmartCalWidgetProvider.FA_MONTHS[(tm - 1).coerceIn(0, 11)]} ${SmartCalWidgetProvider.toFaDigits(ty)}"
                    headers = FA_WD_SHORT
                }
                isRtl = true
                weekendCol = 6 // Friday
                useFaDigits = true
            }

            val views = RemoteViews(context.packageName, R.layout.widget_smartcal_month)
            views.setImageViewBitmap(R.id.monthWidgetBg, SmartCalWidgetProvider.renderThemeCardBitmap(palette, 840, 680))
            views.setTextViewText(R.id.monthTitleBtn, titleStr)
            views.setTextColor(R.id.monthTitleBtn, palette.txtPri)
            views.setTextColor(R.id.monthPrevBtn, palette.txtSec)
            views.setTextColor(R.id.monthNextBtn, palette.txtSec)

            val gridBmp = renderMonthGridBitmap(
                palette, headers, isRtl, startCol, daysInMonth, todayDay, weekendCol, useFaDigits
            )
            views.setImageViewBitmap(R.id.monthGridImage, gridBmp)

            // Wire prev/next/today buttons (RTL swaps visual left/right direction naturally)
            if (isRtl) {
                views.setOnClickPendingIntent(R.id.monthPrevBtn, makeActionPi(context, ACTION_NEXT_MONTH, 6471))
                views.setOnClickPendingIntent(R.id.monthNextBtn, makeActionPi(context, ACTION_PREV_MONTH, 6472))
            } else {
                views.setOnClickPendingIntent(R.id.monthPrevBtn, makeActionPi(context, ACTION_PREV_MONTH, 6471))
                views.setOnClickPendingIntent(R.id.monthNextBtn, makeActionPi(context, ACTION_NEXT_MONTH, 6472))
            }
            views.setOnClickPendingIntent(R.id.monthTitleBtn, makeActionPi(context, ACTION_RESET_TODAY, 6473))

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPi = PendingIntent.getActivity(
                context, 6474, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.monthGridImage, openPi)

            mgr.updateAppWidget(appWidgetId, views)
        }
    }
}
