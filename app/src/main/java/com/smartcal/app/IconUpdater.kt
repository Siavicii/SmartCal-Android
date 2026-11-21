package com.smartcal.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.Calendar

object IconUpdater {
    const val ACTION_MIDNIGHT_UPDATE = "com.smartcal.app.ACTION_MIDNIGHT_ICON_UPDATE"
    private const val PREFS = "smartcal_icon_prefs"
    private const val KEY_LAST_ALIAS = "last_enabled_alias"
    private const val DEFAULT_ALIAS = ".M8D30"

    fun updateTodayIcon(context: Context) {
        val today = JalaliHelper.todayJalali()
        val m = today.month.coerceIn(1, 12)
        val maxD = if (m <= 6) 31 else 30
        val d = today.day.coerceIn(1, maxD)
        val targetAlias = ".M${m}D${d}"

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastAlias = prefs.getString(KEY_LAST_ALIAS, DEFAULT_ALIAS) ?: DEFAULT_ALIAS

        if (lastAlias != targetAlias) {
            val pm = context.packageManager
            val pkg = context.packageName

            // Enable today's month+day alias first
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg$targetAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Disable previous alias
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg$lastAlias"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

            // Ensure default alias is also disabled if different
            if (lastAlias != DEFAULT_ALIAS && targetAlias != DEFAULT_ALIAS) {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, "$pkg$DEFAULT_ALIAS"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            prefs.edit().putString(KEY_LAST_ALIAS, targetAlias).apply()
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
