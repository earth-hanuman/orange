package earth.hanuman.orange

import android.content.Context

/**
 * Orange's only persisted state. Plain SharedPreferences keeps us dependency-free.
 *
 * Model:
 *  - [active]      master on/off
 *  - warmth        amber overlay (0..100 intensity) — handled by OverlayService
 *  - dim           real Extra Dim (0..100 level) — handled by FilterController
 *  - grayscale     real desaturation on/off — handled by FilterController
 *  - schedule      auto on/off between [startMinutes] and [endMinutes]
 */
class Prefs(context: Context) {

    private val sp =
        context.applicationContext.getSharedPreferences("orange", Context.MODE_PRIVATE)

    var active: Boolean
        get() = sp.getBoolean(KEY_ACTIVE, false)
        set(value) = sp.edit().putBoolean(KEY_ACTIVE, value).apply()

    var warmthEnabled: Boolean
        get() = sp.getBoolean(KEY_WARMTH, true)
        set(value) = sp.edit().putBoolean(KEY_WARMTH, value).apply()

    /** Overlay tint strength, 0..100 (like iOS Color Tint "Intensity"). */
    var warmthIntensity: Int
        get() = sp.getInt(KEY_WARMTH_INTENSITY, 60)
        set(value) = sp.edit().putInt(KEY_WARMTH_INTENSITY, value).apply()

    /** Overlay tint hue in degrees, 0..360 (like iOS Color Tint "Hue"; 0 = pure red). */
    var warmthHue: Int
        get() = sp.getInt(KEY_WARMTH_HUE, 12)
        set(value) = sp.edit().putInt(KEY_WARMTH_HUE, value).apply()

    var dimEnabled: Boolean
        get() = sp.getBoolean(KEY_DIM, false)
        set(value) = sp.edit().putBoolean(KEY_DIM, value).apply()

    /** Extra Dim level, 0..100. */
    var dimLevel: Int
        get() = sp.getInt(KEY_DIM_LEVEL, 50)
        set(value) = sp.edit().putInt(KEY_DIM_LEVEL, value).apply()

    var grayscaleEnabled: Boolean
        get() = sp.getBoolean(KEY_GRAYSCALE, false)
        set(value) = sp.edit().putBoolean(KEY_GRAYSCALE, value).apply()

    var scheduleEnabled: Boolean
        get() = sp.getBoolean(KEY_SCHEDULE, false)
        set(value) = sp.edit().putBoolean(KEY_SCHEDULE, value).apply()

    /** Minutes past midnight for the schedule's "on" time (default 21:00). */
    var startMinutes: Int
        get() = sp.getInt(KEY_START, 21 * 60)
        set(value) = sp.edit().putInt(KEY_START, value).apply()

    /** Minutes past midnight for the schedule's "off" time (default 07:00). */
    var endMinutes: Int
        get() = sp.getInt(KEY_END, 7 * 60)
        set(value) = sp.edit().putInt(KEY_END, value).apply()

    /** One-tap bedtime configuration: deep red tint + strong dim, filter on. */
    fun applyBedtimePreset() {
        warmthEnabled = true
        warmthHue = 4
        warmthIntensity = 85
        dimEnabled = true
        dimLevel = 85
        grayscaleEnabled = false
        active = true
    }

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_WARMTH = "warmth_enabled"
        const val KEY_WARMTH_INTENSITY = "warmth_intensity"
        const val KEY_WARMTH_HUE = "warmth_hue"
        const val KEY_DIM = "dim_enabled"
        const val KEY_DIM_LEVEL = "dim_level"
        const val KEY_GRAYSCALE = "grayscale_enabled"
        const val KEY_SCHEDULE = "schedule_enabled"
        const val KEY_START = "start_minutes"
        const val KEY_END = "end_minutes"
    }
}
