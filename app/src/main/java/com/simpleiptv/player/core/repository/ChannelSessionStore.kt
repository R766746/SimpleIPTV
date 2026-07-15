package com.simpleiptv.player.core.repository

import com.simpleiptv.player.core.model.Channel

object ChannelSessionStore {

    private var channels: List<Channel> = emptyList()

    var selectedChannel: Channel? = null
        private set

    fun setChannels(value: List<Channel>) {
        channels = value

        val selected = selectedChannel
        if (selected != null && channels.none { it.id == selected.id }) {
            selectedChannel = null
        }
    }

    fun getChannels(): List<Channel> {
        return channels
    }

    fun getChannelCount(): Int {
        return channels.size
    }

    fun hasMultipleChannels(): Boolean {
        return channels.size > 1
    }

    fun selectChannel(channel: Channel) {
        selectedChannel = channel
    }

    fun selectNextChannel(): Channel? {
        if (channels.isEmpty()) {
            return null
        }

        val currentIndex = selectedChannelIndex()
        val nextIndex = if (currentIndex < 0) {
            0
        } else {
            (currentIndex + 1) % channels.size
        }

        selectedChannel = channels[nextIndex]
        return selectedChannel
    }

    fun selectPreviousChannel(): Channel? {
        if (channels.isEmpty()) {
            return null
        }

        val currentIndex = selectedChannelIndex()
        val previousIndex = if (currentIndex < 0) {
            0
        } else {
            (currentIndex - 1 + channels.size) % channels.size
        }

        selectedChannel = channels[previousIndex]
        return selectedChannel
    }

    fun selectedChannelPositionText(): String? {
        val index = selectedChannelIndex()

        if (index < 0 || channels.isEmpty()) {
            return null
        }

        return "${index + 1} / ${channels.size}"
    }

    fun clearSelectedChannel() {
        selectedChannel = null
    }

    fun clearAll() {
        channels = emptyList()
        selectedChannel = null
    }

    private fun selectedChannelIndex(): Int {
        val selected = selectedChannel ?: return -1

        val byId = channels.indexOfFirst { channel ->
            channel.id == selected.id
        }

        if (byId >= 0) {
            return byId
        }

        return channels.indexOfFirst { channel ->
            channel.streamUrl == selected.streamUrl
        }
    }
}