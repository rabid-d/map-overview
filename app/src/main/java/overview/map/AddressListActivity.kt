package overview.map

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import com.google.android.material.snackbar.Snackbar
import overview.map.db.Point
import android.widget.SimpleAdapter
import overview.map.db.AppDatabase
import overview.map.db.Programmer
import kotlinx.android.synthetic.main.activity_address_list.*
import kotlinx.coroutines.*
import java.text.Collator
import java.util.*
import kotlin.coroutines.CoroutineContext


class AddressListActivity : AppCompatActivity(), CoroutineScope {
    private val ids: MutableList<Int> = mutableListOf()
    private val db: AppDatabase = AppDatabase.getInstance(this)
    private var show500Message = false
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_list)

        title = getString(R.string.address_list_activity_title)
        val args = intent.extras!!
        val parcels = args.getParcelableArray("overview.map.ITEMS")!!
        for (parcel in parcels) {
            if (parcel is Point) {
                ids.add(parcel.id)
            }
        }
        val clusterSize = args.getInt("overview.map.CLUSTER_SIZE", -1)
        if (clusterSize != -1) show500Message = true

        launch {
            var programmers = withContext(Dispatchers.IO) { db.programmerDao().getBunchOfProgrammers(ids) }

            val ukrainianCollator = Collator.getInstance(Locale("uk_UA"))
            val comparator = compareBy(ukrainianCollator) { p: Programmer -> p.fio }
            programmers = programmers.sortedWith(comparator)
            val list = mutableListOf<HashMap<String, String>>()
            var map: HashMap<String, String>
            programmers.forEach {
                map = HashMap()
                map["Name"] = it.fio
                map["Address"] = it.address
                list.add(map)
            }
            val adapter = SimpleAdapter(
                this@AddressListActivity,
                list,
                android.R.layout.simple_list_item_2,
                arrayOf("Name", "Address"),
                intArrayOf(android.R.id.text1, android.R.id.text2)
            )
            clusterItemsListView.adapter = adapter

            val listState: Parcelable? = savedInstanceState?.getParcelable("listState")
            if (listState != null) {
                clusterItemsListView.onRestoreInstanceState(listState)
            }

            val wasRotate = savedInstanceState?.getBoolean("wasRotate") ?: false
            if (!wasRotate && show500Message) {
                rootLayout.postDelayed(Runnable {
                    Snackbar.make(rootLayout, getString(R.string.only_500_shown, clusterSize), Snackbar.LENGTH_LONG).show()
                }, 200)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        with(outState) {
            putBoolean("wasRotate", true)
            val listState = clusterItemsListView.onSaveInstanceState()
            putParcelable("listState", listState)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
