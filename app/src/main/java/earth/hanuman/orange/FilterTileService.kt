package earth.hanuman.orange

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile that toggles the whole filter from the notification shade.
 */
class FilterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = Prefs(this)
        prefs.active = !prefs.active
        Orchestrator.apply(this)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val prefs = Prefs(this)
        val usable = FilterController.hasPermission(this) || Settings.canDrawOverlays(this)
        tile.state = when {
            !usable -> Tile.STATE_UNAVAILABLE
            prefs.active -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.updateTile()
    }

    companion object {
        /** Ask the system to refresh the tile after state changes elsewhere. */
        fun requestUpdate(context: Context) {
            runCatching {
                requestListeningState(
                    context,
                    ComponentName(context, FilterTileService::class.java),
                )
            }
        }
    }
}
