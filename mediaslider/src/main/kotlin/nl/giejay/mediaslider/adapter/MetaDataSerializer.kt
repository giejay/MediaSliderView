package nl.giejay.mediaslider.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import nl.giejay.mediaslider.model.MetaDataType
import java.lang.reflect.Type

class MetaDataSerializer: JsonSerializer<MetaDataItem>, JsonDeserializer<MetaDataItem> {
    override fun serialize(src: MetaDataItem?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val jsonElement = context!!.serialize(src)
        jsonElement.asJsonObject.addProperty("type", src!!::class.simpleName)
        return jsonElement
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MetaDataItem {
        val asJsonObject = json!!.asJsonObject
        val align = AlignOption.valueOf(asJsonObject.get("align").asString)
        return when(asJsonObject.get("type").asString){
            MetaDataMediaCount::class.simpleName -> MetaDataMediaCount(align)
            MetaDataSliderItem::class.simpleName -> {
                MetaDataSliderItem(MetaDataType.valueOf(asJsonObject.get("metaDataType").asString), align)
            }
            else -> MetaDataClock(align)
        }
    }
}