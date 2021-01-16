package com.example.geogarbagetracker

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.ImageView
import coil.load
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.google.maps.android.heatmaps.HeatmapTileProvider

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener {

    lateinit var mMap: GoogleMap
    private val mLocations: MutableList<GarbageLocation> = mutableListOf()
    private var mOverlay: TileOverlay? = null
    private val mAddedMarkers: MutableList<Marker> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        mMap.setOnCameraMoveListener(this)
        val database = FirebaseDatabase.getInstance().reference
        val locations: DatabaseReference = database.child("locations")
        locations.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mMap.clear()
                mLocations.clear()
                mAddedMarkers.clear()
                val locationList = snapshot.children
                for (location in locationList.withIndex()) {
                    val garbageLocation = location.value.getValue(GarbageLocation::class.java)!!
                    mLocations.add(garbageLocation)
                    val position = LatLng(garbageLocation.lat, garbageLocation.lon)
                    mAddedMarkers.add(mMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(garbageLocation.title)))
                    if (location.index == 0) {
                        Log.i("MapActivity", "first location: $position")
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 10F))
                    }
                    Log.i("MapActivity", garbageLocation.toString())
                }
                addHeatMap()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun addHeatMap() {
        val latLngs = mutableListOf<LatLng>()
        for (location in mLocations) {
            latLngs.add(LatLng(location.lat, location.lon))
        }
        val provider = HeatmapTileProvider.Builder()
            .data(latLngs)
            .build()
        mOverlay?.remove()
        mOverlay = mMap.addTileOverlay(TileOverlayOptions().tileProvider(provider))
    }

    override fun onCameraMove() {
        if (mMap.cameraPosition.zoom < 15F) {
            hideMarkers()
            mOverlay?.isVisible = true
        } else {
            showMarkers()
            mOverlay?.isVisible = false
        }
    }

    private fun hideMarkers() {
        for (marker in mAddedMarkers) {
            marker.isVisible = false
        }
    }

    private fun showMarkers() {
        for (marker in mAddedMarkers) {
            marker.isVisible = true
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val garbageDialog = Dialog(this)
        garbageDialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        val view = layoutInflater.inflate(R.layout.dialog_garbage_info, null)
        val url = mLocations.find { it.title == marker.title }?.url
        Log.i("MapActivity", "$url")
        view.findViewById<ImageView>(R.id.garbage_image).load(url ?: "")
        garbageDialog.setContentView(view)
        garbageDialog.show()
        return false
    }
}