package com.example.homework.entity.map

data class YearRange(
    val from: Int,
    val to: Int,
) {
    init {
        require(from <= to) { "yearFrom must be <= yearTo" }
    }

    companion object {
        const val MinYear = 1000
        const val MaxYear = 2026
        val Default = YearRange(1500, 1900)
        val Options = listOf(
            YearRange(1500, 1700),
            YearRange(1700, 1900),
            YearRange(1900, 2000),
        )
    }
}
