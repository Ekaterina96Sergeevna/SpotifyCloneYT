package com.plcoding.spotifycloneyt.data.entities

data class Song(
    // firebase require default values
    val mediaId: String = "",
    val title : String = "",
    val subtitle : String = "",
    val songUrl : String = "",
    val imageUrl : String = "",
)