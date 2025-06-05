package nl.giejay.mediaslider.model

enum class MetaDataType(val title: String, val defaultFontSize: Int) {
    ALBUM_NAME("Album Name", 18),
    CAMERA("Camera Model", 18),
    CITY("City", 18),
    CLOCK("Clock", 48),
    COUNTRY("Country", 18),
    DATE("Date", 18),
    DESCRIPTION("Description", 18),
    FILENAME("Filename", 18),
    FILEPATH("Filepath", 18),
    MEDIA_COUNT("Media Count", 18)
}
