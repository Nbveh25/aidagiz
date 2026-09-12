package com.example.homework.ui.feature.map

import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

object AppTileSource {
    private const val CARTO_API_KEY = "cb1_3hmf_1_20f0184f1372f043b8d7779b"

    val cartoVoyager = object : XYTileSource(
        "CartoVoyagerKeyed",
        1,
        20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/",
        ),
        "© OpenStreetMap contributors © CARTO",
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "${getBaseUrl()}$zoom/$x/$y.png?key=$CARTO_API_KEY"
        }
    }
}
