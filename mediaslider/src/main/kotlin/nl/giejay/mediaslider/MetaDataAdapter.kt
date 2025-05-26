package nl.giejay.mediaslider

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.zeuskartik.mediaslider.R


data class MetaDataItem(val text: String, val resource: Int)

class MetaDataAdapter(val context: Context, val items: List<MetaDataItem>) : BaseAdapter() {
    val layoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(p0: Int): Any {
        return items[0]
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
        val item = items.get(p0)
        val view = layoutInflater.inflate(item.resource, null)
        view.findViewById<TextView>(R.id.textView).text = item.text
//        view.findViewById(R.id.icon).setImageResource(flags.get(i))
        return view
    }
}