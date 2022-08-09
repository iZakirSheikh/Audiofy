package com.prime.player.audio.tracks

import javax.annotation.meta.When

enum class GroupBy {

    // no group is used
    NONE,

    // the title[first letter] of the audio
    NAME,

    // The name of the artist of the audio file
    ARTIST,

    // the album to which the audio file belongs
    ALBUM,

    // The duration of the audio
    // 1 min, 3 mins, 5 mins, 10 mins etc.
    DURATION;
}
