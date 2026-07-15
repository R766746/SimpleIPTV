package com.simpleiptv.player.core.util

import android.content.Context
import com.simpleiptv.player.core.model.CatchupInfo
import com.simpleiptv.player.core.model.Channel
import com.simpleiptv.player.core.model.EpgProgram
import com.simpleiptv.player.core.model.PlaylistSourceType
import com.simpleiptv.player.core.repository.PlaylistSourcePreviewStore
import com.simpleiptv.player.core.repository.XtreamCredentialsStore

object CatchupResolver {

    fun resolve(
        context: Context,
        channel: Channel,
        program: EpgProgram
    ): CatchupInfo? {
        val sourceId = channel.playlistSourceId ?: return null

        val playlistStore = PlaylistSourcePreviewStore(context)
        val source = playlistStore.load().firstOrNull { it.id == sourceId } ?: return null

        if (source.type != PlaylistSourceType.XTREAM_CODES) {
            return null
        }

        val credentialsStore = XtreamCredentialsStore(context)
        val credentials = credentialsStore.getBySourceId(sourceId) ?: return null

        val streamId = extractStreamId(channel.streamUrl) ?: return null

        val durationSeconds = (program.endTimeMillis - program.startTimeMillis) / 1000

        val catchupUrl = credentials.catchupUrl(
            streamId = streamId,
            startTimeMillis = program.startTimeMillis,
            durationSeconds = durationSeconds
        )

        return CatchupInfo(
            program = program,
            catchupUrl = catchupUrl,
            channelName = channel.name
        )
    }

    fun isCatchupAvailable(
        context: Context,
        channel: Channel
    ): Boolean {
        val sourceId = channel.playlistSourceId ?: return false

        val playlistStore = PlaylistSourcePreviewStore(context)
        val source = playlistStore.load().firstOrNull { it.id == sourceId } ?: return false

        if (source.type != PlaylistSourceType.XTREAM_CODES) {
            return false
        }

        val credentialsStore = XtreamCredentialsStore(context)
        return credentialsStore.getBySourceId(sourceId) != null
    }

    private fun extractStreamId(streamUrl: String): String? {
        val regex = Regex("""/(\d+)\.\w+$""")
        val match = regex.find(streamUrl)
        return match?.groupValues?.getOrNull(1)
    }
}