package com.prime.player.core.models

data class Folder(val name: String, val path: String) {
    val audios = ArrayList<Audio>()
}
