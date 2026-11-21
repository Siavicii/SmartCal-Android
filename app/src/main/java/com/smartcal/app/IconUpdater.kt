package com.smartcal.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar

object IconUpdater {
    const val ACTION_MIDNIGHT_UPDATE = "com.smartcal.app.ACTION_MIDNIGHT_ICON_UPDATE"
    private const val PREFS = "smartcal_icon_prefs"
    private const val KEY_LAST_ALIAS = "last_enabled_alias"
    private const val KEY_CAL_TYPE = "cal_type"
    private const val KEY_LANG = "app_lang"
    private const val DEFAULT_ALIAS = ".M8D30"

    fun syncSystemDarkMode(context: Context) {
        val uiNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNight = uiNightMode == Configuration.UI_MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(
            if (isNight) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun updatePreferencesAndIcon(context: Context, calType: String?, lang: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (!calType.isNullOrBlank()) editor.putString(KEY_CAL_TYPE, calType)
        if (!lang.isNullOrBlank()) editor.putString(KEY_LANG, lang)
        editor.apply()
        updateTodayIcon(context)
    }

    fun updateTodayIcon(context: Context) {
        syncSystemDarkMode(context)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val calType = prefs.getString(KEY_CAL_TYPE, "jalali") ?: "jalali"
        val lang = prefs.getString(KEY_LANG, "fa") ?: "fa"

        val cal = Calendar.getInstance()
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)

        val jalali = JalaliHelper.gregorianToJalali(gy, gm, gd)

        // Map any selected calendar system (jalali / kurdish / gregorian) onto available month/day icon slots
        val (mSlot, dSlot) = when (calType) {
            "gregorian" -> Pair(gm.coerceIn(1, 12), gd.coerceIn(1, 31))
            "kurdish" -> Pair(jalali.month.coerceIn(1, 12), jalali.day.coerceIn(1, 31))
            else -> Pair(jalali.month.coerceIn(1, 12), jalali.day.coerceIn(1, 31))
        }

        val maxD = if (mSlot <= 6) 31 else 30
        val safeD = dSlot.coerceIn(1, maxD)
        val targetAlias = ".M${mSlot}D${safeD}"

        val lastAlias = prefs.getString(KEY_LAST_ALIAS, DEFAULT_ALIAS) ?: DEFAULT_ALIAS
        val modeMarker = "${calType}_${lang}"
        val lastModeMarker = prefs.getString("last_mode_marker", "") ?: ""

        if (lastAlias != targetAlias || modeMarker != lastModeMarker) {
            val pm = context.packageManager
            val pkg = context.packageName

            try {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, "$pkg$targetAlias"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {}

            if (lastAlias != targetAlias) {
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(pkg, "$pkg$lastAlias"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (_: Exception) {}
            }

            if (lastAlias != DEFAULT_ALIAS && targetAlias != DEFAULT_ALIAS) {
                try {
                    pm.setComponentEnabledSetting(
                        ComponentName(pkg, "$pkg$DEFAULT_ALIAS"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (_: Exception) {}
            }

            prefs.edit()
                .putString(KEY_LAST_ALIAS, targetAlias)
                .putString("last_mode_marker", modeMarker)
                .apply()
        }
        scheduleNextMidnight(context)
    }

    fun scheduleNextMidnight(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DateChangeReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_UPDATE
        }
        val pi = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }
        am.setInexactRepeating(
            AlarmManager.RTC,
            nextMidnight.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }
}
