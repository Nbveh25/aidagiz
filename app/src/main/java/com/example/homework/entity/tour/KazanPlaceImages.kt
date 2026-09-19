package com.example.homework.entity.tour

fun kazanPlaceImageUrl(name: String): String? {
    val normalized = name.lowercase().replace('ё', 'е').replace(Regex("[«»\"']"), "").trim()
    if (normalized.isEmpty() || normalized in GENERIC_NAMES) return null
    return PLACE_IMAGES
        .filter { (key, _) -> normalized.contains(key) }
        .maxByOrNull { (key, _) -> key.length }
        ?.second
}

private val GENERIC_NAMES = setOf(
    "историческое место",
    "место",
    "attraction",
    "historic",
)

private fun commons(file: String): String {
    val encoded = java.net.URLEncoder.encode(file.replace(' ', '_'), Charsets.UTF_8.name())
        .replace("+", "%20")
    return "https://commons.wikimedia.org/wiki/Special:FilePath/$encoded?width=1280"
}

private val PLACE_IMAGES = listOf(
    "пирамида" to commons("Piramida_Kazan_2012.jpg"),
    "иоанно-предтечен" to commons("Kazan_Ioanno-Predtechensky_Monastery_08-2016_img2.jpg"),
    "ивановск" to commons("Kazan_Ioanno-Predtechensky_Monastery_08-2016_img2.jpg"),
    "петуш" to commons("Часы_на_Баумана..JPG"),
    "часы на бауман" to commons("Часы_на_Баумана..JPG"),
    "баумана" to commons("Baumana_Street_Kazan_Russia_2009_sept_06.jpg"),
    "кул-шариф" to commons("Kazan_Kremlin_Qolsharif_Mosque_08-2016_img2.jpg"),
    "кул шариф" to commons("Kazan_Kremlin_Qolsharif_Mosque_08-2016_img2.jpg"),
    "сююмбике" to commons("Kazan_Kremlin_Soyembika_Tower_08-2016_img1.jpg"),
    "союмбике" to commons("Kazan_Kremlin_Soyembika_Tower_08-2016_img1.jpg"),
    "спасск" to commons("Kazan_Kremlin_Spasskaya_Tower_08-2016_img2.jpg"),
    "тайницк" to commons("Kazan Kremlin Taynitskaya Tower 08-2016.jpg"),
    "благовещенск" to commons("Благовещенский_собор_-_panoramio_(2).jpg"),
    "национальн" to commons("Национальный_музей_Республики_Татарстан.JPG"),
    "гостиный двор" to commons("Национальный_музей_Республики_Татарстан.JPG"),
    "гостинодворск" to commons("Бывшая_Гостинодворская_церковь_(г._Казань)_(2015_год)_-_1.JPG"),
    "богоявленск" to commons("Church_Epiphany,_Kazan_(2023-04-12)_14.jpg"),
    "черное озеро" to commons("Black_Lake,_Kazan.JPG"),
    "черн озеро" to commons("Black_Lake,_Kazan.JPG"),
    "университетская" to commons("Kazan_20180805_110925.jpg"),
    "университет" to commons("Kazan_state_university.jpg"),
    "александровск" to commons("Kazan_Kremlevskaya_Street_Alexandrovsky_Passage_08-2016.jpg"),
    "пассаж" to commons("Kazan_Kremlevskaya_Street_Alexandrovsky_Passage_08-2016.jpg"),
    "ушков" to commons("Дом_Ушковой_З._Н._04.JPG"),
    "петропавловск" to commons("Петропавловский_собор_в_Казани_00.jpg"),
    "марджани" to commons("Kazan_Marjani_Mosque_08-2016_img1.jpg"),
    "апанаев" to commons("Kazan_Apanayev_Mosque_08-2016_img1.jpg"),
    "азимов" to commons("Азимовская_мечеть2.jpg"),
    "нурулла" to commons("Kazan_Nurulla_Mosque_08-2016.jpg"),
    "закабанн" to commons("Kazan_Zakabannaya_Mosque_08-2016.jpg"),
    "султанов" to commons("Солтан_мечеть.jpg"),
    "бурнаев" to commons("Kazan_Burnay_Mosque_08-2016.jpg"),
    "голубая мечет" to commons("Голубая_мечеть_Казань.jpg"),
    "качалов" to commons("Театр_имени_В.И.Качалова..JPG"),
    "камала" to commons("Театр_Камала,_центральный_вход.jpg"),
    "земледельц" to commons("Дворец_Земледельцев_(Казань).JPG"),
    "кремлевская набережн" to commons("Кремлёвская_набережная_с_холма.jpg"),
    "набережн" to commons("Кремлёвская_набережная_с_холма.jpg"),
    "площадь тысяч" to commons("Вид_от_кремлёвских_стен.jpg"),
    "тысячелет" to commons("Вид_от_кремлёвских_стен.jpg"),
    "николо-гостино" to commons("Бывшая_Гостинодворская_церковь_(г._Казань)_(2015_год)_-_1.JPG"),
    "никольск" to commons("Kazan_Baumana_Street_StNicholas_Cathedral_08-2016.jpg"),
    "варвар" to commons("Варваринская_церковь.JPG"),
    "адмиралтей" to commons("Адмиралтейская_слобода.jpg"),
    "пушечн" to commons("Vakhitovskiy_rayon,_Kazan,_Respublika_Tatarstan,_Russia_-_panoramio_(331).jpg"),
    "губернатор" to commons("Дворец_губернаторский._Кремь,_Казань.JPG"),
    "юнкер" to commons("4preobraznensky cadet school.jpg"),
    "богородиц" to commons("Kazansky_Bogoroditsky_Monastery_(2020-09-02)_11.jpg"),
    "дом печати" to commons("Дом_печати_-_Казань.jpg"),
    "кабан" to commons("Qaban_from_Kamal.JPG"),
    "вахитов" to commons("Казань._Памятник_Муллануру_Вахитову_(3).JPG"),
    "алабрыс" to commons("Памятник_Казанскому_коту_Алабрысу._Казань._Январь_2014_-_panoramio.jpg"),
    "кот казанск" to commons("Памятник_Казанскому_коту_Алабрысу._Казань._Январь_2014_-_panoramio.jpg"),
    "зилант" to commons("Zilant monument (2023-08-19) 05.jpg"),
    "цирк" to commons("Kazan_circus_(2022-07-05)_03.jpg"),
    "храм всех" to commons("Kazan_church_edit1.jpg"),
    "вселенск" to commons("Kazan_church_edit1.jpg"),
    "зоо" to commons("Kazan_zoo-botanical_garden_(entrance).jpg"),
    "кремлевская улиц" to commons("Streets_in_Kazan_(April_2025)_-_0_3.jpg"),
    "площадь свобод" to
        "https://upload.wikimedia.org/wikipedia/ru/b/b0/%D0%9F%D0%BB%D0%BE%D1%89%D0%B0%D0%B4%D1%8C_%D0%A1%D0%B2%D0%BE%D0%B1%D0%BE%D0%B4%D1%8B_-_%D0%9A%D0%B0%D0%B7%D0%B0%D0%BD%D1%8C_-_2017.jpg",
    "шамил" to
        "https://upload.wikimedia.org/wikipedia/ru/b/b2/%D0%94%D0%BE%D0%BC_%D0%A8%D0%B0%D0%BC%D0%B8%D0%BB%D1%8F_%D0%9A%D0%B0%D0%B7%D0%B0%D0%BD%D1%8C.jpg",
    "кремл" to commons("Kazan_Kremlin_-_panoramio_(6).jpg"),
)
