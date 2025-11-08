package nl.giejay.mediaslider.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.gson.JsonObject
import com.zeuskartik.mediaslider.R
import nl.giejay.mediaslider.model.MetaDataType
import nl.giejay.mediaslider.model.SliderItem
import nl.giejay.mediaslider.util.MetaDataConverter

enum class AlignOption {
    LEFT, RIGHT
}

sealed class MetaDataItem(val type: MetaDataType) {
    val textViewResourceId: Int = R.id.textView
    abstract val align: AlignOption
    abstract val fontSize: Int
    abstract val padding: Int
    abstract fun createView(layoutInflater: LayoutInflater): View
    abstract suspend fun getValue(item: SliderItem, index: Int, totalCount: Int): String?
    abstract fun updateView(view: TextView, value: String?)
    fun withAlign(align: AlignOption): MetaDataItem {
        return create(type, align, padding, fontSize)
    }

    abstract fun getTitle(): String

    companion object {
        const val DEFAULT_PADDING = 0
        fun create(type: MetaDataType, align: AlignOption, padding: Int, fontSize: Int): MetaDataItem {
            // being able to change from a metadataclock to a mediacount or slideritem in metadata customizer
            val obj = JsonObject()
            obj.addProperty("type", type.toString())
            obj.addProperty("align", align.toString())
            obj.addProperty("padding", padding)
            obj.addProperty("fontSize", fontSize)
            return MetaDataConverter.metaDataFromJsonObject(obj)
        }
    }
}

data class MetaDataClock(override val align: AlignOption,
                         override val fontSize: Int = MetaDataType.CLOCK.defaultFontSize,
                         override val padding: Int = DEFAULT_PADDING) : MetaDataItem(MetaDataType.CLOCK) {

    override fun createView(layoutInflater: LayoutInflater): View {
        return layoutInflater.inflate(R.layout.metadata_item_clock, null)
    }

    override suspend fun getValue(item: SliderItem, index: Int, totalCount: Int): String? {
        return null
    }

    override fun updateView(view: TextView, value: String?) {
        // no-op
    }

    override fun getTitle(): String {
        return "Clock"
    }
}

data class MetaDataMediaCount(override val align: AlignOption,
                              override val fontSize: Int = MetaDataType.MEDIA_COUNT.defaultFontSize,
                              override val padding: Int = DEFAULT_PADDING) : MetaDataItem(MetaDataType.MEDIA_COUNT) {
    override fun createView(layoutInflater: LayoutInflater): View {
        return layoutInflater.inflate(R.layout.metadata_item, null)
    }

    override suspend fun getValue(item: SliderItem, index: Int, totalCount: Int): String {
        return "${index + 1}/$totalCount"
    }

    override fun updateView(view: TextView, value: String?) {
        view.text = value
    }

    override fun getTitle(): String {
        return "Media Count"
    }
}

data class MetaDataSliderItem(val metaDataType: MetaDataType, override val align: AlignOption,
                              override val fontSize: Int = metaDataType.defaultFontSize,
                              override val padding: Int = DEFAULT_PADDING) : MetaDataItem(metaDataType) {

    override fun createView(layoutInflater: LayoutInflater): View {
        return layoutInflater.inflate(R.layout.metadata_item, null)
    }

    override suspend fun getValue(item: SliderItem, index: Int, totalCount: Int): String? {
        return item.get(metaDataType)
    }

    override fun updateView(view: TextView, value: String?) {
        if(value?.isBlank() == true){
            view.width = 0
            view.height = 0
            view.setPadding(0, 0, 0, 0)
            view.text = ""
        } else {
            view.text = value
        }
    }

    override fun getTitle(): String {
        return metaDataType.title
    }
}

class MetaDataAdapter(val context: Context,
                      val items: List<MetaDataItem>,
                      private val portraitViewItems: List<MetaDataItem>,
                      private val updateItem: (MetaDataItem, SliderItem, TextView) -> Unit,
                      private val getCurrentItem: () -> SliderItem,
                      private val portraitMode: () -> Boolean) : BaseAdapter() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private val viewsPerType: MutableMap<MetaDataType, View> = mutableMapOf()

    override fun getCount(): Int {
        return getItemsToShow().size
    }

    private fun getItemsToShow(): List<MetaDataItem> = (if (portraitMode()) portraitViewItems else items)

    override fun getItem(p0: Int): Any {
        return getItemsToShow()[p0]
    }

    override fun getItemId(p0: Int): Long {
        return getItemsToShow()[p0].type.ordinal.toLong()
    }

    override fun isEnabled(position: Int): Boolean {
        return false
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val item = getItem(p0) as MetaDataItem
        // can't use p1 because the list might differ for every photo/adapter
        val view = viewsPerType.getOrPut(item.type) { item.createView(layoutInflater) }
        val textView = view.findViewById<TextView>(item.textViewResourceId)
        if (item.align == AlignOption.RIGHT) {
            val params = textView.layoutParams as RelativeLayout.LayoutParams
            textView.gravity = Gravity.RIGHT
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            view.layoutParams = params
        }
        textView.textSize = item.fontSize.toFloat()
        textView.setPadding(textView.paddingLeft, item.padding, textView.paddingRight, item.padding)
        updateItem.invoke(item, getCurrentItem(), textView)
        return view
    }
}