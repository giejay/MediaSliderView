package nl.giejay.mediaslider.model

enum class MetaDataType(val title: String, val defaultFontSize: Int) {
    CLOCK("Clock", 48),
    MEDIA_COUNT("Media Count", 18),
    DESCRIPTION("Description", 18),
    COUNTRY("Country", 18),
    CITY("City", 18),
    ALBUM_NAME("Album Name", 18),
    DATE("Date", 18)
}
