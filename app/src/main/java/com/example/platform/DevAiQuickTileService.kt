package com.example.platform

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity

class DevAiQuickTileService : TileService() {

    private var isActiveState = false

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        isActiveState = !isActiveState

        // Perform action: launch assistant or toggle mode
        if (isActiveState) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "DEV AI: Active"
            tile.subtitle = "Tap to open"
            tile.updateTile()

            // Launch app directly into voice assistant
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("EXTRA_START_VOICE", true)
            }
            startActivityAndCollapse(intent)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "DEV AI Assistant"
            tile.subtitle = "Standby"
            tile.updateTile()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (isActiveState) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "DEV AI Assistant"
        tile.subtitle = if (isActiveState) "Active" else "Tap to trigger"
        tile.updateTile()
    }
}
