package earth.hanuman.orange

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Turns the filter on/off at the user's chosen times using [AlarmManager].
 *
 * We use inexact repeating alarms on purpose: a blue-light schedule doesn't need
 * second-level precision, and inexact alarms avoid the SCHEDULE_EXACT_ALARM
 * permission entirely — fewer permissions, better for battery and privacy.
 */
object ScheduleManager {

    const val ACTION_START = "earth.hanuman.orange.ACTION_START"
    const val ACTION_END = "earth.hanuman.orange.ACTION_END"

    fun reschedule(context: Context) {
        val prefs = Prefs(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val start = pendingIntent(context, ACTION_START)
        val end = pendingIntent(context, ACTION_END)

        am.cancel(start)
        am.cancel(end)
        if (!prefs.scheduleEnabled) return

        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTrigger(prefs.startMinutes),
            AlarmManager.INTERVAL_DAY,
            start,
        )
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTrigger(prefs.endMinutes),
            AlarmManager.INTERVAL_DAY,
            end,
        )
    }

    /** True if the current wall-clock time is inside the scheduled on-window. */
    fun inWindowNow(prefs: Prefs): Boolean {
        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = prefs.startMinutes
        val end = prefs.endMinutes
        return if (start <= end) {
            minutes in start until end
        } else {
            // Overnight window, e.g. 21:00 -> 07:00.
            minutes >= start || minutes < end
        }
    }

    private fun pendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, ScheduleReceiver::class.java).setAction(action)
        val requestCode = if (action == ACTION_START) 1 else 2
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTrigger(minutesOfDay: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= now.timeInMillis) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }
}
