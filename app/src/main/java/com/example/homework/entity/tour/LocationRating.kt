package com.example.homework.entity.tour

data class LocationRating(
    val id: String,
    val userId: String,
    val placeId: String,
    val rating: Int,
    val review: String?,
    val createdAt: String,
    val updatedAt: String,
)
