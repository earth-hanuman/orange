package earth.hanuman.orange

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * Draws the warmth as a deep-amber overlay, since the real night_display
 * transform is unreachable to a non-system app on this device.
 *
 * An amber layer absorbs the blue the panel emits, and can go far warmer than
 * the OS Night Light allows — which is exactly what helps at night. It runs as
 * a foreground service (a persistent view needs a living process); the only cost
 * is a low-priority ongoing notification.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlay: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOrUpdate()
        return START_STICKY
    }

    private fun showOrUpdate() {
        // If the permission was revoked, don't try to draw.
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        val prefs = Prefs(this)
        val color = warmthColor(prefs.warmthHue, prefs.warmthIntensity)
        val existing = overlay
        if (existing == null) {
            val view = View(this).apply { setBackgroundColor(color) }
            // Explicit full-display size: MATCH_PARENT on an overlay window resolves
            // to the fit frame (stops at the nav bar), leaving a gap at the bottom.
            val (fullW, fullH) = fullDisplaySize()
            val params = WindowManager.LayoutParams(
                fullW,
                fullH,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            )
            params.gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            windowManager.addView(view, params)
            overlay = view
        } else {
            existing.setBackgroundColor(color)
            resizeToFullDisplay(existing)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotation / display size change: the window keeps its old dimensions,
        // so re-measure and resize to the new full display bounds.
        overlay?.let { resizeToFullDisplay(it) }
    }

    /** Update an already-added overlay view to the current full display size. */
    private fun resizeToFullDisplay(view: View) {
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val (fullW, fullH) = fullDisplaySize()
        if (params.width == fullW && params.height == fullH) return
        params.width = fullW
        params.height = fullH
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /**
     * A square the size of the display's largest edge, so one fixed size covers
     * the screen in *any* orientation. This means we never resize on rotation —
     * resizing forces a relayout that briefly blanks the overlay. The excess
     * spills off-screen harmlessly (FLAG_LAYOUT_NO_LIMITS). Includes the status-
     * and nav-bar regions.
     */
    private fun fullDisplaySize(): Pair<Int, Int> {
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
            metrics.widthPixels to metrics.heightPixels
        }
        val side = maxOf(w, h)
        return side to side
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Filter running",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Orange filter active")
            .setContentText("Warmth overlay is on")
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(tap)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        overlay?.let { view -> runCatching { windowManager.removeView(view) } }
        overlay = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "orange_filter"
        private const val ACTION_HIDE = "earth.hanuman.orange.OVERLAY_HIDE"

        /** Maximum overlay opacity at intensity 100 (strong, near-monochrome like iOS). */
        private const val MAX_ALPHA = 0.90f

        /**
         * Overlay color from an iOS-style hue (0..360°, 0 = pure red) + intensity
         * (0..100 → opacity). The tint is fully saturated so it reads as a true
         * color wash, like iOS Color Tint.
         */
        fun warmthColor(hue: Int, intensity: Int): Int {
            val base = Color.HSVToColor(floatArrayOf(hue.coerceIn(0, 360).toFloat(), 1f, 1f))
            val alpha = (intensity.coerceIn(0, 100) / 100f * MAX_ALPHA * 255f)
                .roundToInt().coerceIn(0, 255)
            return Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
        }

        /** Start or refresh the overlay (reads current intensity from Prefs). */
        fun show(context: Context) {
            context.startForegroundService(Intent(context, OverlayService::class.java))
        }

        /** Remove the overlay and stop the service. */
        fun hide(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
