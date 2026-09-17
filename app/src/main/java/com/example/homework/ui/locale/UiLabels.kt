package com.example.homework.ui.locale

import androidx.annotation.StringRes
import com.example.homework.R
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.PlaceFilter
import com.example.homework.entity.map.TransportMode

val PlaceFilter.labelRes: Int
    @StringRes get() = when (this) {
        PlaceFilter.All -> R.string.filter_all
        PlaceFilter.History -> R.string.filter_history
        PlaceFilter.Culture -> R.string.filter_culture
        PlaceFilter.Food -> R.string.filter_food
        PlaceFilter.Museums -> R.string.filter_museums
        PlaceFilter.Nature -> R.string.filter_nature
        PlaceFilter.Viewpoints -> R.string.filter_viewpoints
    }

val PlaceCategory.labelRes: Int
    @StringRes get() = when (this) {
        PlaceCategory.Mosque -> R.string.category_mosque
        PlaceCategory.Temple -> R.string.category_temple
        PlaceCategory.Museum -> R.string.category_museum
        PlaceCategory.Gallery -> R.string.category_gallery
        PlaceCategory.ArtsCentre -> R.string.category_arts_centre
        PlaceCategory.Artwork -> R.string.category_artwork
        PlaceCategory.Theatre -> R.string.category_theatre
        PlaceCategory.Cafe -> R.string.category_cafe
        PlaceCategory.Restaurant -> R.string.category_restaurant
        PlaceCategory.Park -> R.string.category_park
        PlaceCategory.Garden -> R.string.category_garden
        PlaceCategory.Historic -> R.string.category_historic
        PlaceCategory.Attraction -> R.string.category_attraction
        PlaceCategory.Viewpoint -> R.string.category_viewpoint
        PlaceCategory.Other -> R.string.category_other
    }

val TransportMode.labelRes: Int
    @StringRes get() = when (this) {
        TransportMode.Walking -> R.string.transport_walking
        TransportMode.Cycling -> R.string.transport_cycling
        TransportMode.Driving -> R.string.transport_driving
        TransportMode.Transit -> R.string.transport_transit
    }
