package nl.giejay.mediaslider.model

import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Objects

class SliderItem : Parcelable {
    var id: String
    val url: String?
    val type: SliderItemType
    private val metaData: Map<MetaDataType, String?>
    val thumbnailUrl: String?

    constructor(id: String, url: String?, type: SliderItemType, metaData: Map<MetaDataType, String?>, thumbnailUrl: String?) {
        this.id = id
        this.url = url
        this.type = type
        this.metaData = metaData
        this.thumbnailUrl = thumbnailUrl
    }

    private constructor(`in`: Parcel) {
        id = `in`.readString()!!
        url = `in`.readString()!!
        type = SliderItemType.valueOf(`in`.readString()!!)
        metaData = `in`.readHashMap(ClassLoader.getSystemClassLoader())!!.toMap() as Map<MetaDataType, String>
        thumbnailUrl = `in`.readString()!!
    }

    fun get(metaData: MetaDataType): String? {
        return this.metaData[metaData]
    }



    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(url)
        dest.writeString(type.toString())
        dest.writeMap(this.metaData)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as SliderItem
        return url == that.url
    }

    override fun hashCode(): Int {
        return Objects.hash(url)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SliderItem> = object : Parcelable.Creator<SliderItem> {
            override fun createFromParcel(`in`: Parcel): SliderItem {
                return SliderItem(`in`)
            }

            override fun newArray(size: Int): Array<SliderItem?> {
                return arrayOfNulls(size)
            }
        }
    }
}
