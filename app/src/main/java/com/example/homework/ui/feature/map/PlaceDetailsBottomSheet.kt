package com.example.homework.ui.feature.map

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework.R
import com.example.homework.entity.map.OsmPlace
import com.example.homework.entity.map.PlaceCategory
import com.example.homework.entity.map.formatDistance
import com.example.homework.ui.uikit.theme.ForestGreenDeep
import com.example.homework.ui.uikit.theme.SheetWhite
import com.example.homework.ui.uikit.theme.TextOnForest
import com.example.homework.ui.uikit.theme.TextPrimary
import com.example.homework.ui.uikit.theme.TextSecondary

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailsBottomSheet(
    place: OsmPlace,
    distanceMeters: Int?,
    onDismiss: () -> Unit,
) {
    var sheetSize by remember { mutableStateOf(PlaceSheetSize.Peek) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val targetHeight = when (sheetSize) {
        PlaceSheetSize.Peek -> screenHeight / 3
        PlaceSheetSize.Details -> screenHeight * 2 / 3
        PlaceSheetSize.Guide -> screenHeight
    }

    val sheetHeight by animateDpAsState(targetValue = targetHeight, label = "placeSheetHeight")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val details = buildList {
        add(place.category.label)
        if (distanceMeters != null) add(formatDistance(distanceMeters))
        place.openingHours?.let { add(it) }
    }.joinToString(" · ")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (sheetSize == PlaceSheetSize.Guide) Color.Transparent else SheetWhite,
        shape = if (sheetSize == PlaceSheetSize.Guide) {
            RectangleShape
        } else {
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        },
        dragHandle = null,
    ) {
        if (sheetSize == PlaceSheetSize.Guide) {
            AudioGuideContent(
                place = place,
                onBack = { sheetSize = PlaceSheetSize.Details },
                onClose = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight),
            )
        } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .navigationBarsPadding()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 16.dp,
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD5D3CE)),
            )
            if (sheetSize == PlaceSheetSize.Details) {
                ExpandedPlaceContent(
                    place = place,
                    details = details,
                    onCollapse = { sheetSize = PlaceSheetSize.Peek },
                    onListen = { sheetSize = PlaceSheetSize.Guide },
                    modifier = Modifier.weight(1f),
                )
            } else {
                CollapsedPlaceContent(
                    place = place,
                    details = details,
                    onExpand = { sheetSize = PlaceSheetSize.Details },
                )
            }
        }
        }
    }
}

private enum class PlaceSheetSize {
    Peek,
    Details,
    Guide,
}

@Composable
private fun CollapsedPlaceContent(
    place: OsmPlace,
    details: String,
    onExpand: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(mockPhotoRes(place)),
            contentDescription = place.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = place.name,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mockDescription(place),
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Подробнее",
            tint = TextSecondary,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onExpand),
        )
    }
    Spacer(Modifier.height(14.dp))
    Text(
        text = details,
        color = TextSecondary,
        fontSize = 13.sp,
    )
}

@Composable
private fun ExpandedPlaceContent(
    place: OsmPlace,
    details: String,
    onCollapse: () -> Unit,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Image(
                painter = painterResource(mockPhotoRes(place)),
                contentDescription = place.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = details,
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Свернуть",
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onCollapse),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "AI-гид",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = mockGuideText(place),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlaceSheetButton(
                label = "Слушать AI-гида",
                filled = true,
                onClick = onListen,
            )
            PlaceSheetButton(
                label = "Я посмотрел, дальше",
                filled = false,
                onClick = {},
            )
        }
    }
}

@Composable
private fun PlaceSheetButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (filled) TextOnForest else ForestGreenDeep,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) ForestGreenDeep else Color(0x14163833))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        textAlign = TextAlign.Center,
    )
}

@DrawableRes
private fun mockPhotoRes(place: OsmPlace): Int = when (place.category) {
    PlaceCategory.Mosque -> R.drawable.photo_kul_sharif
    PlaceCategory.Park -> R.drawable.photo_sloboda
    PlaceCategory.Cafe, PlaceCategory.Restaurant -> R.drawable.photo_bauman
    PlaceCategory.Historic, PlaceCategory.Attraction, PlaceCategory.Museum -> R.drawable.photo_kremlin
    else -> when ((place.id % 4).toInt()) {
        0 -> R.drawable.photo_kremlin
        1 -> R.drawable.photo_kul_sharif
        2 -> R.drawable.photo_bauman
        else -> R.drawable.photo_sloboda
    }
}

private fun mockDescription(place: OsmPlace): String = when (place.category) {
    PlaceCategory.Mosque -> "Историческая мечеть Казани. Архитектура и тихая атмосфера двора."
    PlaceCategory.Temple -> "Храм с богатой историей. Стоит зайти и осмотреть интерьер."
    PlaceCategory.Museum -> "Музей о городе и его культуре. Короткий визит займёт около часа."
    PlaceCategory.Theatre -> "Театр с красивым фасадом. Удобная точка на прогулке по центру."
    PlaceCategory.Cafe -> "Уютное кафе рядом. Хорошее место передохнуть и перекусить."
    PlaceCategory.Restaurant -> "Ресторан с локальной кухней. Можно запланировать обед в маршруте."
    PlaceCategory.Park -> "Зелёная зона для короткой прогулки и паузы между точками."
    PlaceCategory.Historic -> "Историческое место Казани. Короткий рассказ о прошлом города."
    PlaceCategory.Attraction -> "Популярная точка маршрута. Несколько минут на осмотр и фото."
    PlaceCategory.Other -> "Интересная точка поблизости. Можно добавить в маршрут."
}

private fun mockGuideText(place: OsmPlace): String {
    val intro = mockDescription(place)
    val story = when (place.category) {
        PlaceCategory.Mosque ->
            "Купола и минареты видны издалека. Во дворе тише, чем на улице: слышен ветер и шаги. Обратите внимание на орнамент портала и на то, как свет падает на стены ближе к закату."

        PlaceCategory.Temple ->
            "Фасад держит ритм старого города. Внутри обычно прохладнее, голоса приглушены. Стоит остановиться у входа и сравнить декор с соседними зданиями той же эпохи."

        PlaceCategory.Museum ->
            "Коллекция собрана так, чтобы за час сложилась картина города: быт, ремёсла, лица. Не гонитесь за всеми залами — выберите два сюжета и рассмотрите их спокойно."

        PlaceCategory.Theatre ->
            "Театр здесь не только сцена, но и площадь перед ним. Вечером фасад подсвечивают, днём можно обойти здание и посмотреть на детали карниза."

        PlaceCategory.Cafe ->
            "Место для паузы: чай, эчпочмак, короткая остановка между точками маршрута. Если шумно у окна, сядьте глубже в зал — там обычно тише."

        PlaceCategory.Restaurant ->
            "Локальная кухня — часть прогулки, не отдельная программа. Закажите одно татарское блюдо и не затягивайте обед, если впереди ещё есть точки."

        PlaceCategory.Park ->
            "Дорожки расходятся от центральной аллеи. Сверните с главного пути на минуту: скамейки в тени и вид на воду часто лучше, чем у входа."

        PlaceCategory.Historic ->
            "Камень и таблички хранят слой за слоем: ханская Казань, губернский город, советские годы. Прочитайте одну табличку вслух — маршрут сразу становится рассказом."

        PlaceCategory.Attraction ->
            "Сюда приходят за кадром, но место живёт и без фото. Обойдите точку кругом: задний ракурс часто спокойнее и ближе к настоящему масштабу."

        PlaceCategory.Other ->
            "Точка не из парадного списка, зато рядом с маршрутом. Задержитесь на пару минут: такие места связывают известные остановки в цельный путь."
    }
    return "$intro\n\n$story\n\nТак AI-гид помогает не торопиться: один взгляд, одна деталь, и можно идти дальше."
}
