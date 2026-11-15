package com.universalshot.app

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ScreenshotTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let {
            it.state = Tile.STATE_INACTIVE
            it.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        ShotHelper.capture(applicationContext, true)
    }
}
