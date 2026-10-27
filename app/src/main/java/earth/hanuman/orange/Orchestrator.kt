package earth.hanuman.orange

import android.content.Context
import android.provider.Settings

/**
 * Single place that turns the persisted [Prefs] into actual effects:
 * secure-settings transforms (grayscale, dim) + the warmth overlay. Everything
 * that changes state (UI, tile, schedule, boot) funnels through here.
 */
object Orchestrator {

    fun apply(context: Context) {
        val prefs = Prefs(context)
        val active = prefs.active

        FilterController.apply(
            context,
            grayscale = active && prefs.grayscaleEnabled,
            dim = active && prefs.dimEnabled,
            dimLevel = prefs.dimLevel,
        )

        val wantOverlay = active && prefs.warmthEnabled && Settings.canDrawOverlays(context)
        if (wantOverlay) {
            OverlayService.show(context)
        } else {
            OverlayService.hide(context)
        }

        FilterTileService.requestUpdate(context)
    }
}
