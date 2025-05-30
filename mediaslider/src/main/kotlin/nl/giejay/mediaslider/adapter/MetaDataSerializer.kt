package nl.giejay.mediaslider.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import nl.giejay.mediaslider.model.MetaDataType
import java.lang.reflect.Type

class MetaDataSerializer : JsonSerializer<MetaDataItem>, JsonDeserializer<MetaDataItem> {
    override fun serialize(src: MetaDataItem?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val jsonElement = context!!.serialize(src)
        jsonElement.asJsonObject.addProperty("type", src!!.type.toString())
        return jsonElement
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MetaDataItem {
        val asJsonObject = json!!.asJsonObject
        val align = AlignOption.valueOf(asJsonObject.get("align").asString)
        return when (val type = asJsonObject.get("type").asString) {
            MetaDataType.MEDIA_COUNT.toString() -> MetaDataMediaCount(align)
            MetaDataType.CLOCK.toString() -> MetaDataClock(align)
            else -> MetaDataSliderItem(MetaDataType.valueOf(type), align)
        }
    }
}