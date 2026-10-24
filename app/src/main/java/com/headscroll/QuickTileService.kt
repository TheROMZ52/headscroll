package com.headscroll

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuickTileService : TileService() {

    override fun onStartListening() {
        refresh()
    }

    override fun onClick() {
        if (LiveState.running) {
            stopService(Intent(this, FaceService::class.java))
            qsTile?.let {
                it.state = Tile.STATE_INACTIVE
                it.updateTile()
            }
        } else {
            val i = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("autostart", true)
            launch(i)
        }
    }

    @Suppress("DEPRECATION")
    private fun launch(i: Intent) {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
        } else {
            startActivityAndCollapse(i)
        }
    }

    private fun refresh() {
        qsTile?.let {
            it.state = if (LiveState.running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.label = "HeadScroll"
            it.updateTile()
        }
    }
}
