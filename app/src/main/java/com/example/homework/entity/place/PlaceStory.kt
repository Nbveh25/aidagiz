package com.example.homework.entity.place

data class PlaceStory(
    val placeId: String,
    val name: String?,
    val story: String,
    val sources: List<String>,
)
