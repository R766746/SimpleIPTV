package com.simpleiptv.player.core.repository

import com.simpleiptv.player.core.model.Channel

object ChannelSessionStore {

    private var channels: List<Channel> = emptyList()

    var selectedChannel: Channel? = null
        private set

    fun setChannels(value: List<Channel>) {
        channels = value
    }

    fun getChannels(): List<Channel> {
        return channels
    }

    fun selectChannel(channel: Channel) {
        selectedChannel = channel
    }

    fun clearSelectedChannel() {
        selectedChannel = null
    }

    fun clearAll() {
        channels = emptyList()
        selectedChannel = null
    }
}