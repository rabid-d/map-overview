package overview.map

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.transition.TransitionManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.coroutines.*
import overview.map.db.AppDatabase
import overview.map.db.Point
import overview.map.db.Programmer
import net.sharewire.googlemapsclustering.Cluster
import net.sharewire.googlemapsclustering.ClusterManager
import kotlin.coroutines.CoroutineContext


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, CoroutineScope {
    private lateinit var mMap: GoogleMap
    private lateinit var mClusterManager: ClusterManager<Point>
    private lateinit var db: AppDatabase
    private var isPopupShown: Boolean = false
    private var popupProgrammer: Programmer? = null
    private var statusBarHeight = 0
    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0
    private var lastZoom: Float = 0f
    private val searchRequestCode = 1
    private var selectedMarker: LatLng? = null
    private var activityResult = false
    private var runnable: Runnable? = null
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        with(PreferenceManager.getDefaultSharedPreferences(this)) {
            val firstRunKey = "firstRun_" + BuildConfig.VERSION_CODE
            if (getBoolean(firstRunKey, true)) {
                //Log.e("MAPS", "First run!")
                deleteDatabase("overview.db")
                edit().putBoolean(firstRunKey, false).commit()
            }
        }

        rootLayout.setOnApplyWindowInsetsListener(onApplyWindowsInsetsListener)

        db = AppDatabase.getInstance(this)

        readAppSettings()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        search_fab.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivityForResult(intent, searchRequestCode)
        }
        license_fab.setOnClickListener {
            val intent = Intent(this, LicenseActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        runnable = Runnable {
            val popupProgrammerId = savedInstanceState.getInt("popupProgrammer", -1)
            if (popupProgrammerId != -1) {
                popup(popupProgrammerId)
            }
        }
        popupLayout.postDelayed(runnable, 200)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        if (isPopupShown) {
            popupProgrammer?.let {
                outState!!.putInt("popupProgrammer", it.id)
            }
        }
        super.onSaveInstanceState(outState)
    }

    private fun readAppSettings() {
        with(PreferenceManager.getDefaultSharedPreferences(this)) {
            lastLat = getFloat("lastLat", 49.0275000f).toDouble()
            lastLng = getFloat("lastLng", 31.1827778f).toDouble()
            lastZoom = getFloat("lastZoom", 5f)
        }
    }

    override fun onResume() {
        super.onResume()
        if (activityResult) return
        readAppSettings()
    }

    override fun onPause() {
        super.onPause()
        if (this::mMap.isInitialized) {
            val lastLatLng = mMap.projection.visibleRegion.latLngBounds.center
            val lastZoom = mMap.cameraPosition.zoom
            val sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit()
            with(sharedPrefEditor) {
                putFloat("lastLat", lastLatLng.latitude.toFloat())
                putFloat("lastLng", lastLatLng.longitude.toFloat())
                putFloat("lastZoom", lastZoom)
                commit()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == searchRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {
                val programmer = it.getParcelableExtra<Programmer>("overview.map.SEARCH_RESULT")
                if (programmer != null) {
                    popup(null)
                    val programmerCoords = LatLng(programmer.lat.toDouble(), programmer.lng.toDouble())
                    if (this::mMap.isInitialized) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(programmerCoords, 18f))
                    } else {
                        lastLat = programmer.lat.toDouble()
                        lastLng = programmer.lng.toDouble()
                        lastZoom = 18f
                        activityResult = true
                        // Don't show popup of prev selected if we came from search.
                        popupLayout.removeCallbacks(runnable)
                    }
                }
            }
        }
    }

    private val onApplyWindowsInsetsListener = View.OnApplyWindowInsetsListener { view, insets->
        this.statusBarHeight = insets.systemWindowInsetTop
        return@OnApplyWindowInsetsListener insets
    }

    private fun popup(id: Int) {
        val point = Point()
        point.id = id
        popup(point)
    }

    private fun popup(item: Point?) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)
        if (item == null && isPopupShown) {
            selectedMarker?.run { mClusterManager.deselectClusterItem(this) }
            isPopupShown = false
            popupProgrammer = null
            selectedMarker = null
            constraintSet.clear(R.id.popupLayout, ConstraintSet.TOP)
            constraintSet.connect(R.id.popupLayout, ConstraintSet.BOTTOM, R.id.rootLayout, ConstraintSet.TOP, 8)
            TransitionManager.beginDelayedTransition(popupLayout)
            constraintSet.applyTo(rootLayout)
        }
        if (item != null) {
            // Update info
            launch {
                val programmer = withContext(Dispatchers.IO) { db.programmerDao().getProgrammer(item.id) }
                addressTextView.text = programmer.address
                fioTextView.text = programmer.fio
                selectedMarker?.run { mClusterManager.deselectClusterItem(this) }
                val lat = programmer.lat.toDouble()
                val lng = programmer.lng.toDouble()
                selectedMarker = LatLng(lat, lng)
                mClusterManager.selectClusterItem(LatLng(lat, lng))
                popupProgrammer = programmer
            }
            if (isPopupShown == false) {
                // Show
                isPopupShown = true
                val topPopupMargin = 24
                constraintSet.clear(R.id.popupLayout, ConstraintSet.BOTTOM)
                constraintSet.connect(R.id.popupLayout, ConstraintSet.TOP, R.id.rootLayout, ConstraintSet.TOP, statusBarHeight + topPopupMargin)
                TransitionManager.beginDelayedTransition(popupLayout)
                constraintSet.applyTo(rootLayout)
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lastLat, lastLng), lastZoom))

        val styleJson = getString(R.string.map_style_json)
        mMap.setMapStyle(MapStyleOptions(styleJson))
        /*val success = mMap.setMapStyle(MapStyleOptions(styleJson))
        if (success) {
            Log.e("MAPS", "Style successfully loaded!");
        } else {
            Log.e("MAPS", "Style NOT loaded!");
        }*/
        mMap.uiSettings.isRotateGesturesEnabled = false

        mMap.setOnMapClickListener {
            popup(null)
        }

        mClusterManager = ClusterManager(this, mMap)
        mMap.setOnCameraIdleListener(mClusterManager)

        launch {
            val points = withContext(Dispatchers.IO) { db.programmerDao().getAllPoints() }
            mClusterManager.setItems(points)
        }

        mClusterManager.setCallbacks(clusterManagerCallbacks)
    }

    private val clusterManagerCallbacks = object: ClusterManager.Callbacks<Point> {
        override fun onClusterClick(cluster: Cluster<Point>): Boolean {
            val intent = Intent(this@MapsActivity, AddressListActivity::class.java).apply {
                if (cluster.items.size > 500) {
                    val first500 = cluster.items.subList(0, 500)
                    putExtra("overview.map.ITEMS", first500.toTypedArray())
                    putExtra("overview.map.CLUSTER_SIZE", cluster.items.size)
                    return@apply
                }
                putExtra("overview.map.ITEMS", cluster.items.toTypedArray())
            }
            startActivity(intent)

            return true
        }

        override fun onClusterItemClick(clusterItem: Point): Boolean {
            popup(clusterItem)
            return true
        }
    }
}
