package overview.map

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.*
import overview.map.db.AppDatabase
import overview.map.db.Programmer
import kotlin.coroutines.CoroutineContext


class SearchActivity : AppCompatActivity(), SearchRecyclerViewAdapter.SuggestionClickListener, CoroutineScope {
    private lateinit var db: AppDatabase
    private lateinit var searchView: SearchView
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchRecyclerViewAdapter
    private var getSuggestionsJob: Deferred<List<Programmer>>? = null
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        searchRecyclerView = findViewById<RecyclerView>(R.id.search_results_rv).apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            setOnTouchListener { _, _ ->
                searchView.clearFocus()
                return@setOnTouchListener false
            }
            searchAdapter = SearchRecyclerViewAdapter(this@SearchActivity)
            searchAdapter.setClickListener(this@SearchActivity)
            adapter = searchAdapter
        }
        db = AppDatabase.getInstance(this)
    }

    override fun onSuggestionClick(view: View, programmer: Programmer) {
        if (programmer.lat == 0.0f || programmer.lng == 0.0f) {
            Snackbar.make(searchRootLayout, "Сталося страшне :(", Snackbar.LENGTH_SHORT).show()
            return
        }
        val data = Intent()
        data.putExtra("overview.map.SEARCH_RESULT", programmer)
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        searchView = menu.findItem(R.id.action_search).actionView as SearchView
        with (searchView) {
            val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
            setSearchableInfo(searchManager.getSearchableInfo(ComponentName(this@SearchActivity, SearchActivity::class.java)))
            queryHint = getString(R.string.search_hint)
            requestFocus()
            isIconified = false
            setIconifiedByDefault(false)
            maxWidth = Int.MAX_VALUE
            imeOptions = searchView.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            // Це не працює. Енівей нехай буде, може на якихось пристроях прокне.
            inputType = searchView.inputType or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setOnQueryTextListener(onQueryTextListener)
        }

        val magId = resources.getIdentifier("android:id/search_mag_icon", null, null)
        val searchIcon = searchView.findViewById<ImageView>(magId)
        searchIcon.layoutParams = LinearLayout.LayoutParams(0, 0)

        val item: MenuItem = menu.findItem(R.id.action_search)
        item.setOnActionExpandListener(onActionExpandListener)
        item.expandActionView()

        with(PreferenceManager.getDefaultSharedPreferences(this)) {
            val lastSearchQuery = getString("searchQuery", "")
            searchView.setQuery(lastSearchQuery, true)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPause() {
        if (this::searchView.isInitialized) {
            with(PreferenceManager.getDefaultSharedPreferences(this).edit()) {
                putString("searchQuery", searchView.query.toString())
                commit()
            }
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }

    private val onQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {
            if (query == "") {
                getSuggestionsJob?.cancel()
                searchAdapter.setSearchResults(listOf(), query)
                searchRecyclerView.scrollToPosition(0)
                return false
            }
            if (getSuggestionsJob?.isActive == true) {
                getSuggestionsJob?.cancel()
            }
            launch {
                getSuggestionsJob = async(Dispatchers.IO) {
                    db.programmerDao().searchByFio(query.toUpperCase())
                }

                val suggestedProgrammers = getSuggestionsJob?.await() ?: listOf()
                searchAdapter.setSearchResults(suggestedProgrammers, query)
                searchRecyclerView.scrollToPosition(0)
            }
            return false
        }

        override fun onQueryTextChange(query: String): Boolean {
            onQueryTextSubmit(query)
            return false
        }
    }

    private val onActionExpandListener = object : MenuItem.OnActionExpandListener {
        override fun onMenuItemActionExpand(item:MenuItem):Boolean {
            // Do whatever you need
            return true // KEEP IT TO TRUE OR IT DOESN'T OPEN !!
        }
        override fun onMenuItemActionCollapse(item:MenuItem):Boolean {
            // Do whatever you need
            finish()
            return false // OR FALSE IF YOU DIDN'T WANT IT TO CLOSE!
        }
    }

    override fun onBackPressed() {
        finish()
    }
}
