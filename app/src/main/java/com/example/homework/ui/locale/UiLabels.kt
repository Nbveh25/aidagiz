package com.example.homework.ui.locale

import androidx.annotation.StringRes
import com.example.homework.R
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.PlaceFilter
import com.example.homework.entity.map.TransportMode
import com.example.homework.entity.tour.CulturalInterest
import com.example.homework.entity.tour.WalkPace

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

val CulturalInterest.labelRes: Int
    @StringRes get() = when (this) {
        CulturalInterest.History -> R.string.interest_history
        CulturalInterest.TatarCulture -> R.string.interest_tatar
        CulturalInterest.Architecture -> R.string.interest_architecture
        CulturalInterest.Museums -> R.string.interest_museums
        CulturalInterest.Mosques -> R.string.interest_mosques
        CulturalInterest.Parks -> R.string.interest_parks
    }

val WalkPace.labelRes: Int
    @StringRes get() = when (this) {
        WalkPace.Fast -> R.string.pace_fast
        WalkPace.Normal -> R.string.pace_normal
        WalkPace.Leisurely -> R.string.pace_leisurely
    }

val TransportMode.labelRes: Int
    @StringRes get() = when (this) {
        TransportMode.Walking -> R.string.transport_walking
        TransportMode.Cycling -> R.string.transport_cycling
        TransportMode.Driving -> R.string.transport_driving
        TransportMode.Transit -> R.string.transport_transit
    }
