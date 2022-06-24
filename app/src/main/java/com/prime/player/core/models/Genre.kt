package com.prime.player.core.models

data class Genre(val id: Long, val name: String) {
    val audios = ArrayList<Audio>()
}
