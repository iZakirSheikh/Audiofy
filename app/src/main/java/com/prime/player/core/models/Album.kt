package com.prime.player.core.models

data class Album(val id: Long, val title: String) {
    var audioList = ArrayList<Audio>()
    var desc: String? = null
    var artist: Artist? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Album

        if (id != other.id) return false
        if (title != other.title) return false
        if (audioList != other.audioList) return false
        if (desc != other.desc) return false
        if (artist?.id != other.artist?.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + audioList.hashCode()
        result = 31 * result + (desc?.hashCode() ?: 0)
        result = 31 * result + (artist?.id.hashCode() ?: 0)
        return result
    }
}