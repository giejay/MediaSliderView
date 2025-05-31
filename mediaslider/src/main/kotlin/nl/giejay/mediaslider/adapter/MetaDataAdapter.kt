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
    abstract fun createView(layoutInflater: LayoutInflater): View
    abstract fun updateView(view: TextView, item: SliderItem, index: Int, totalCount: Int)
    abstract fun hasData(sliderItem: SliderItem): Boolean
    fun withAlign(align: AlignOption): MetaDataItem {
        return create(type, align)
    }

    companion object{
        fun create(type: MetaDataType, align: AlignOption): MetaDataItem{
            // being able to change from a metadataclock to a mediacount or slideritem in metadata customizer
            val obj = JsonObject()
            obj.addProperty("type", type.toString())
            obj.addProperty("align", align.toString())
            return MetaDataConverter.metaDataFromJsonObject(obj)
        }
    }
}

data class MetaDataClock(override val align: AlignOption) : MetaDataItem(MetaDataType.CLOCK) {
    override fun createView(layoutInflater: LayoutInflater): View {
        return layoutInflater.inflate(R.layout.metadata_item_clock, null)
    }

    override fun updateView(view: TextView, item: SliderItem, index: Int, totalCount: Int) {
        // no-op
    }

    override fun hasData(sliderItem: SliderItem): Boolean {
        return true
    }

}

data class MetaDataMediaCount(override val align: AlignOption) : MetaDataItem(MetaDataType.MEDIA_COUNT) {
    override fun createView(layoutInflater: LayoutInflater): View {
        return layoutInflater.inflate(R.layout.metadata_item, null)
    }

    override fun updateView(view: TextView, item: SliderItem, index: Int, totalCount: Int) {
        view.text = "${index + 1}/$totalCount"
    }

    override fun hasData(sliderItem: SliderItem): Boolean {
        return true
    }
}

data class MetaDataSliderItem(val metaDataType: MetaDataType, override val align: AlignOption) : MetaDataItem(metaDataType) {

    override fun createView(layoutInflater: LayoutInflater): View {
        return layoutInflater.inflate(R.layout.metadata_item, null)
    }

    override fun updateView(view: TextView, item: SliderItem, index: Int, totalCount: Int) {
        view.text = item.get(metaDataType)
    }

    override fun hasData(sliderItem: SliderItem): Boolean {
        return sliderItem.get(metaDataType)?.isNotBlank() == true
    }
}

class MetaDataAdapter(val context: Context,
                      val items: List<MetaDataItem>,
                      private val portraitViewItems: List<MetaDataItem>,
                      private val updateItem: (MetaDataItem, SliderItem, TextView) -> Unit,
                      private val getCurrentItem: () -> SliderItem,
                      private val portraitMode: () -> Boolean) : BaseAdapter() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return getFilteredMetaData().size
    }

    private fun getFilteredMetaData() = (if (portraitMode()) portraitViewItems else items).filter {
        it.hasData(getCurrentItem())
    }

    override fun getItem(p0: Int): Any {
        return getFilteredMetaData()[p0]
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun isEnabled(position: Int): Boolean {
        return false
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val item = getItem(p0) as MetaDataItem
        // todo use p1
        val view = item.createView(layoutInflater)
        val textView = view.findViewById<TextView>(item.textViewResourceId)
        if (item.align == AlignOption.RIGHT) {
            val params = textView.layoutParams as RelativeLayout.LayoutParams
            textView.gravity = Gravity.RIGHT
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            view.layoutParams = params
        }
        updateItem.invoke(item, getCurrentItem(), textView)
        return view
    }
}