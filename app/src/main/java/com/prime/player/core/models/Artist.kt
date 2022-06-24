package com.prime.player.core.models

data class Artist(val id: Long, val name: String) {
    var desc: String? = null
    val albumList = ArrayList<Album>()
    val audioList = ArrayList<Audio>()
}
