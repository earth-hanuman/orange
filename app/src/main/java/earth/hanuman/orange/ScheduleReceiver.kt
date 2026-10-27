package earth.hanuman.orange

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the schedule's start/end alarms, and re-arms everything after a reboot.
 */
class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Alarms don't survive reboots; set them up again and restore state.
                ScheduleManager.reschedule(context)
                Orchestrator.apply(context)
            }

            ScheduleManager.ACTION_START -> {
                prefs.active = true
                Orchestrator.apply(context)
            }

            ScheduleManager.ACTION_END -> {
                prefs.active = false
                Orchestrator.apply(context)
            }
        }
    }
}
