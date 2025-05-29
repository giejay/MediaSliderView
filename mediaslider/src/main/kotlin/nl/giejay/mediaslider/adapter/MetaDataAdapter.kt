package nl.giejay.mediaslider.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import nl.giejay.mediaslider.model.MetaDataType
import com.zeuskartik.mediaslider.R
import kotlinx.serialization.Serializable
import nl.giejay.mediaslider.model.SliderItem

enum class AlignOption {
    LEFT, RIGHT
}

@Serializable
sealed class MetaDataItem{
    abstract val align: AlignOption
    abstract fun setAlignOption(align: AlignOption): MetaDataItem
}

@Serializable
data class MetaDataClock(override val align: AlignOption): MetaDataItem() {
    override fun setAlignOption(align: AlignOption): MetaDataItem {
        return copy(align = align)
    }
}

@Serializable
data class MetaDataMediaCount(override val align: AlignOption): MetaDataItem() {
    override fun setAlignOption(align: AlignOption): MetaDataItem {
        return copy(align = align)
    }
}

@Serializable
data class MetaDataSliderItem(val metaDataType: MetaDataType, override val align: AlignOption):MetaDataItem() {
    fun setView(view: TextView, item: SliderItem) {
        view.text = item.get(metaDataType)
        // todo add font size, padding?
    }

    override fun setAlignOption(align: AlignOption): MetaDataItem {
        return copy(align = align)
    }
}

class MetaDataAdapter(val context: Context, val items: List<MetaDataItem>, private val portraitViewItems: List<MetaDataItem>) : BaseAdapter() {
    private var portraitMode: Boolean = false
    private var currentMediaCount: String? = null
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var currentItem: SliderItem? = null

    override fun getCount(): Int {
        if(currentItem == null){
            return 0
        }
        return getFilteredMetaData().size
    }

    private fun getFilteredMetaData() = (if (portraitMode) portraitViewItems else items).filter {
        it !is MetaDataSliderItem || currentItem!!.get(it.metaDataType)?.isNotBlank() == true
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
        val view = if (item is MetaDataClock) {
            layoutInflater.inflate(R.layout.metadata_item_clock, null)
        } else layoutInflater.inflate(R.layout.metadata_item, null)

        currentItem?.let {
            val textView = view.findViewById<TextView>(R.id.textView)

            if (item.align == AlignOption.RIGHT) {
                val params = textView.layoutParams as RelativeLayout.LayoutParams
                textView.gravity = Gravity.RIGHT
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                view.layoutParams = params
            }

            if (item is MetaDataClock) {
                return view
            }

            if (item is MetaDataMediaCount) {
                textView.text = currentMediaCount ?: ""
            } else if(item is MetaDataSliderItem) {
                item.setView(textView, it)
            }
            return view
        }
        return view
    }

    fun setItem(mediaSliderItem: SliderItem, portraitMode: Boolean = false) {
        this.currentItem = mediaSliderItem
        this.portraitMode = portraitMode
        notifyDataSetChanged()
    }

    fun clearItem(){
        if(portraitMode){
            this.currentItem = null
            this.portraitMode = false
            notifyDataSetChanged()
        }
    }

    fun setMediaCount(s: String) {
        this.currentMediaCount = s
        notifyDataSetChanged()
    }
}