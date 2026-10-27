package earth.hanuman.orange

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Drives the two display transforms we *can* reach on a non-system app via the
 * WRITE_SECURE_SETTINGS grant: grayscale (daltonizer) and Extra Dim
 * (reduce_bright_colors). Both are read directly from secure settings by the
 * accessibility pipeline, so our writes take effect immediately.
 *
 * Warmth is NOT here: night_display is managed by a privileged service that
 * ignores external writes, so warmth is handled as an overlay in [OverlayService].
 */
object FilterController {

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE = "accessibility_display_daltonizer"

    /** Daltonizer mode value for full grayscale (monochromacy). */
    private const val DALTONIZER_MONOCHROMACY = 0

    private const val REDUCE_BRIGHT_COLORS_ACTIVATED = "reduce_bright_colors_activated"
    private const val REDUCE_BRIGHT_COLORS_LEVEL = "reduce_bright_colors_level"

    /** Extra Dim level range the system accepts (0 = none, 100 = darkest). */
    val DIM_RANGE = 0..100

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Apply the grayscale and dim transforms. Returns false only if the
     * permission is missing, so callers can show the setup card.
     */
    fun apply(context: Context, grayscale: Boolean, dim: Boolean, dimLevel: Int): Boolean = try {
        val cr = context.contentResolver

        if (grayscale) {
            Settings.Secure.putInt(cr, DALTONIZER_MODE, DALTONIZER_MONOCHROMACY)
            Settings.Secure.putInt(cr, DALTONIZER_ENABLED, 1)
        } else {
            Settings.Secure.putInt(cr, DALTONIZER_ENABLED, 0)
        }

        if (dim) {
            Settings.Secure.putInt(cr, REDUCE_BRIGHT_COLORS_LEVEL, dimLevel.coerceIn(DIM_RANGE))
            Settings.Secure.putInt(cr, REDUCE_BRIGHT_COLORS_ACTIVATED, 1)
        } else {
            Settings.Secure.putInt(cr, REDUCE_BRIGHT_COLORS_ACTIVATED, 0)
        }
        true
    } catch (_: SecurityException) {
        false
    }

    /** Turn both secure transforms off. */
    fun clear(context: Context) {
        try {
            val cr = context.contentResolver
            Settings.Secure.putInt(cr, DALTONIZER_ENABLED, 0)
            Settings.Secure.putInt(cr, REDUCE_BRIGHT_COLORS_ACTIVATED, 0)
        } catch (_: SecurityException) {
            // Nothing granted, nothing to clear.
        }
    }
}
