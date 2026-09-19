package com.example.homework.entity.tour

import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory

fun OsmPlace.withMockSightDescription(): OsmPlace {
    if (description.isNotBlank()) return this
    return copy(description = mockSightDescription(name, category))
}

private fun mockSightDescription(name: String, category: PlaceCategory): String {
    val title = name.ifBlank { "Это место" }
    return when (category) {
        PlaceCategory.Mosque ->
            "$title — мечеть в историческом центре Казани. Стоит зайти во двор и посмотреть на купола."
        PlaceCategory.Temple ->
            "$title — храм с богатой историей у кремлёвских стен. Короткой остановки хватит, чтобы осмотреть фасад."
        PlaceCategory.Attraction ->
            "$title — заметная точка на прогулке. Несколько минут на фото и вид на улицу."
        else ->
            "$title — историческое место Казани. Короткий рассказ о прошлом города и архитектуре вокруг."
    }
}
