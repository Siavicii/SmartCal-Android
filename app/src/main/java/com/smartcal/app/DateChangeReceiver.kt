package com.smartcal.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // 1. Always re-arm next midnight alarm first
        try { IconUpdater.scheduleNextMidnight(context) } catch (_: Exception) {}

        // 2. Refresh persistent status bar date notification BEFORE icon alias switch
        //    (switching activity-alias can restart the app process and abort later lines)
        try { IconUpdater.refreshStatusDateNotificationIfEnabled(context) } catch (_: Exception) {}

        // 3. Update all home-screen widgets
        try { SmartCalMonthWidgetProvider.resetAndUpdateAll(context) } catch (_: Exception) {}
        try { SmartCalWidgetProvider.updateAllWidgets(context) } catch (_: Exception) {}
        try { SmartCalMarketWidgetProvider.updateAllWidgets(context) } catch (_: Exception) {}

        // 4. Re-register saved user note/event alarms
        try { AlarmReceiver.rescheduleAllSavedAlarms(context) } catch (_: Exception) {}

        // 5. Switch launcher icon activity-alias LAST
        try { IconUpdater.updateTodayIcon(context) } catch (_: Exception) {}
    }
}
