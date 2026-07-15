package com.simpleiptv.player.core.repository

import com.simpleiptv.player.core.model.XtreamSeriesInfo

object SeriesSessionStore {
    var selectedSeries: XtreamSeriesInfo? = null
        private set

    fun selectSeries(series: XtreamSeriesInfo) {
        selectedSeries = series
    }

    fun clear() {
        selectedSeries = null
    }
}