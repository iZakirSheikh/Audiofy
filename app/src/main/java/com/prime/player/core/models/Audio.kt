package com.prime.player.core.models

data class Audio(val id: Long, val title: String, val mineType: String, val duration: Int) {
    var dateModified: Long = 0
    var year = 0
    var path: String? = null
    var trackNumber = 0

    var artist: Artist? = null
    var album: Album? = null
    var genre: Genre? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Audio

        if (id != other.id) return false
        if (title != other.title) return false
        if (mineType != other.mineType) return false
        if (duration != other.duration) return false
        if (dateModified != other.dateModified) return false
        if (year != other.year) return false
        if (path != other.path) return false
        if (trackNumber != other.trackNumber) return false
        if (artist?.id != other.artist?.id) return false
        if (album?.id != other.album?.id) return false
        if (genre?.id != other.genre?.id) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + mineType.hashCode()
        result = 31 * result + duration
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + year
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + trackNumber
        result = 31 * result + artist?.id.hashCode()
        result = 31 * result + album?.id.hashCode()
        result = 31 * result + genre?.id.hashCode()
        return result
    }

}
