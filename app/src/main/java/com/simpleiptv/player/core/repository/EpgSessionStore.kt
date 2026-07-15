package com.simpleiptv.player.core.repository

import com.simpleiptv.player.core.model.EpgProgram

object EpgSessionStore {

    private var programs: List<EpgProgram> = emptyList()
    private var channelNames: Map<String, String> = emptyMap()

    fun setEpgData(
        programs: List<EpgProgram>,
        channelNames: Map<String, String>
    ) {
        this.programs = programs
        this.channelNames = channelNames
    }

    fun getAllPrograms(): List<EpgProgram> = programs

    fun getChannelNames(): Map<String, String> = channelNames

    fun getProgramsForChannel(channelId: String): List<EpgProgram> {
        return programs.filter { program ->
            program.channelId.equals(channelId, ignoreCase = true)
        }.sortedBy { it.startTimeMillis }
    }

    fun getCurrentProgram(channelId: String): EpgProgram? {
        val now = System.currentTimeMillis()

        return programs.firstOrNull { program ->
            program.channelId.equals(channelId, ignoreCase = true) &&
                    program.isCurrentlyAiring(now)
        }
    }

    fun getNextProgram(channelId: String): EpgProgram? {
        val now = System.currentTimeMillis()

        return programs
            .filter { program ->
                program.channelId.equals(channelId, ignoreCase = true) &&
                        program.startTimeMillis > now
            }
            .minByOrNull { it.startTimeMillis }
    }

    fun getNowNext(channelId: String): Pair<EpgProgram?, EpgProgram?> {
        return Pair(
            getCurrentProgram(channelId),
            getNextProgram(channelId)
        )
    }

    fun hasEpgData(): Boolean = programs.isNotEmpty()

    fun clear() {
        programs = emptyList()
        channelNames = emptyMap()
    }
}