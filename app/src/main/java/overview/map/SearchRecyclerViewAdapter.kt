package overview.map

import android.content.Context
import android.os.Build
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.RecyclerView
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import overview.map.db.Programmer

class SearchRecyclerViewAdapter(context: Context) : RecyclerView.Adapter<SearchRecyclerViewAdapter.ViewHolder>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var clickListener: SuggestionClickListener
    private var query: String = ""
    private val comparator: Comparator<Programmer>

    init {
        comparator = Comparator { p0, p1 ->
            p0.fio.compareTo(p1.fio)
        }
    }

    private val sortedList = SortedList<Programmer>(Programmer::class.java, object: SortedList.Callback<Programmer>() {

        override fun areItemsTheSame(p0: Programmer, p1: Programmer): Boolean = p0.id == p1.id
        override fun areContentsTheSame(p0: Programmer, p1: Programmer): Boolean = p0.id == p1.id
        override fun compare(p0: Programmer, p1: Programmer): Int = comparator.compare(p0, p1)

        override fun onMoved(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)
        override fun onChanged(position: Int, count: Int) = notifyItemRangeChanged(position, count)
        override fun onInserted(position: Int, count: Int) = notifyItemRangeInserted(position, count)
        override fun onRemoved(position: Int, count: Int) = notifyItemRangeRemoved(position, count)
    })

    private fun add(model: Programmer) = sortedList.add(model)
    private fun add(models:List<Programmer>) = sortedList.addAll(models)
    private fun remove(model: Programmer) = sortedList.remove(model)
    private fun remove(models:List<Programmer>) {
        sortedList.beginBatchedUpdates()
        for (model in models)
        {
            sortedList.remove(model)
        }
        sortedList.endBatchedUpdates()
    }
    private fun replaceAll(models:List<Programmer>) {
        sortedList.beginBatchedUpdates()
        for (i in sortedList.size() - 1 downTo 0)
        {
            val model = sortedList.get(i)
            if (!models.contains(model))
            {
                sortedList.remove(model)
            }
        }
        sortedList.addAll(models)
        sortedList.endBatchedUpdates()
    }

    fun setSearchResults(newData: List<Programmer>, newQuery: String) {
        query = newQuery

        // Видалити ті яких немає.
        sortedList.beginBatchedUpdates()
        var i = 0
        while (true) {
            if (i == sortedList.size()) {
                break
            }
            val result = newData.find { it.id == sortedList[i].id }
            if (result == null) {
                sortedList.removeItemAt(i)
            } else {
                i++
            }
        }
        // Оновити старі.
        for (j in 0 until sortedList.size()) {
            sortedList.updateItemAt(j, sortedList[j])
        }
        // Додати нові.
        for (proger in newData) {
            var contains = false
            for (j in 0 until sortedList.size()) {
                if (sortedList[j].id == proger.id) {
                    contains = true
                }
            }
            if (!contains) {
                sortedList.add(proger)
            }
        }
        sortedList.endBatchedUpdates()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.search_result_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = sortedList.size()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val programmer = sortedList[position]
        holder.fioTextView.text = highlightQuery(programmer.fio) ?: programmer.fio
        holder.addressTextView.text = programmer.address
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val fioTextView: TextView = itemView.findViewById(R.id.programmerFio)
        val addressTextView: TextView = itemView.findViewById(R.id.programmerAddress)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            clickListener.onSuggestionClick(view, sortedList[adapterPosition])
        }
    }

    @Suppress("DEPRECATION")
    private fun highlightQuery(fio: String): Spanned? {
        val index = fio.indexOf(query, 0, true)
        if (index != -1) {
            val textBefore = fio.substring(0, index)
            val querySubstring = fio.substring(index, index + query.length)
            val textAfter = fio.substring(index + query.length, fio.length)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml("$textBefore<b><font color=black>$querySubstring</font></b>$textAfter", Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml("$textBefore<font color=white>$querySubstring</font>$textAfter")
            }
        }
        return null
    }

    fun setClickListener(listener: SuggestionClickListener) {
        this.clickListener = listener
    }

    interface SuggestionClickListener {
        fun onSuggestionClick(view: View, programmer: Programmer)
    }
}
