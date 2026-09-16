package com.example.homework.entity.place

import com.example.homework.entity.map.PlaceCategory

data class PlaceDetails(
    val placeId: String,
    val name: String,
    val shortDescription: String,
    val fullDescription: String,
    val address: String,
    val tags: List<String>,
    val photoRes: Int,
    val imageUrl: String?,
    val openingHours: String?,
    val category: PlaceCategory,
)
