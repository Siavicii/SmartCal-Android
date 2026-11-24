package com.smartcal.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONObject

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "یادآور Smart Cal"
        val body = intent.getStringExtra("body") ?: ""
        val reqCode = intent.getIntExtra("reqCode", 2001)

        removeSavedAlarm(context, reqCode)

        val channelId = "smartcal_note_alarms"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "یادآور یادداشت‌ها", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
            nm.createNotificationChannel(ch)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, reqCode, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pi)
            .build()

        nm.notify(reqCode, notif)
    }

    companion object {
        private const val PREFS_ALARMS = "smartcal_saved_alarms"
        private const val KEY_ALARMS_JSON = "alarms_map"

        fun scheduleAlarm(
            context: Context,
            triggerMs: Long,
            title: String,
            body: String,
            reqCode: Int,
            persist: Boolean = true
        ) {
            try {
                if (triggerMs <= System.currentTimeMillis()) return
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("title", title)
                    putExtra("body", body)
                    putExtra("reqCode", reqCode)
                }
                val pi = PendingIntent.getBroadcast(
                    context, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.canScheduleExactAlarms()
                } else {
                    true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (canExact) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                    }
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
                if (persist) {
                    saveAlarm(context, triggerMs, title, body, reqCode)
                }
            } catch (_: Exception) {}
        }

        private fun saveAlarm(context: Context, triggerMs: Long, title: String, body: String, reqCode: Int) {
            try {
                val prefs = context.getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
                val map = JSONObject(prefs.getString(KEY_ALARMS_JSON, "{}") ?: "{}")
                val entry = JSONObject().apply {
                    put("triggerMs", triggerMs)
                    put("title", title)
                    put("body", body)
                }
                map.put(reqCode.toString(), entry)
                prefs.edit().putString(KEY_ALARMS_JSON, map.toString()).apply()
            } catch (_: Exception) {}
        }

        fun removeSavedAlarm(context: Context, reqCode: Int) {
            try {
                val prefs = context.getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
                val map = JSONObject(prefs.getString(KEY_ALARMS_JSON, "{}") ?: "{}")
                if (map.has(reqCode.toString())) {
                    map.remove(reqCode.toString())
                    prefs.edit().putString(KEY_ALARMS_JSON, map.toString()).apply()
                }
            } catch (_: Exception) {}
        }

        fun rescheduleAllSavedAlarms(context: Context) {
            try {
                val prefs = context.getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
                val map = JSONObject(prefs.getString(KEY_ALARMS_JSON, "{}") ?: "{}")
                val now = System.currentTimeMillis()
                val kept = JSONObject()
                val keys = map.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val reqCode = key.toIntOrNull() ?: continue
                    val entry = map.optJSONObject(key) ?: continue
                    val triggerMs = entry.optLong("triggerMs", 0L)
                    if (triggerMs > now) {
                        val title = entry.optString("title", "یادآور Smart Cal")
                        val body = entry.optString("body", "")
                        scheduleAlarm(context, triggerMs, title, body, reqCode, persist = false)
                        kept.put(key, entry)
                    }
                }
                prefs.edit().putString(KEY_ALARMS_JSON, kept.toString()).apply()
            } catch (_: Exception) {}
        }
    }
}
