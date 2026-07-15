package com.simpleiptv.player.core.util

import android.content.Context
import android.content.res.Configuration

object TvDeviceDetector {
    fun isAndroidTv(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    }
}