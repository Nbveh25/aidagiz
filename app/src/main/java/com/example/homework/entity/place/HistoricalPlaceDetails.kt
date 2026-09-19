package com.example.homework.entity.place

data class HistoricalPlaceDetails(
    val placeId: String,
    val imageUrl: String?,
    val nameRu: String,
    val nameTt: String?,
    val story: String,
    val firstMentionYear: Int?,
)
