package com.example.automation

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private var selectedLatLng: LatLng? = null
    private var radiusMeters = 200

    private var marker: Marker? = null
    private var circle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        /* ---------- MAP ---------- */

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        /* ---------- UI ---------- */

        val etSearch = findViewById<EditText>(R.id.etSearchLocation)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val tvRadius = findViewById<TextView>(R.id.tvRadius)
        val seekRadius = findViewById<SeekBar>(R.id.seekRadius)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmLocation)

        tvRadius.text = "Radius: $radiusMeters meters"

        val geocoder = Geocoder(this, Locale.getDefault())

        /* ---------- SEARCH ---------- */

        fun performSearch(query: String) {
            if (query.isBlank()) return

            try {
                val results = geocoder.getFromLocationName(query, 1)

                if (!results.isNullOrEmpty()) {
                    val address = results[0]
                    val latLng = LatLng(address.latitude, address.longitude)

                    selectedLatLng = latLng
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                    )

                    updateMarker(latLng)
                    updateCircle()
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
            }
        }

        btnSearch.setOnClickListener {
            performSearch(etSearch.text.toString())
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.text.toString())
                true
            } else {
                false
            }
        }

        /* ---------- RADIUS ---------- */

        seekRadius.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    radiusMeters = maxOf(50, progress)
                    tvRadius.text = "Radius: $radiusMeters meters"
                    updateCircle()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        /* ---------- CONFIRM ---------- */

        btnConfirm.setOnClickListener {
            val latLng = selectedLatLng ?: return@setOnClickListener

            val result = Intent().apply {
                putExtra("lat", latLng.latitude)
                putExtra("lng", latLng.longitude)
                putExtra("radius", radiusMeters)
            }

            setResult(RESULT_OK, result)
            finish()
        }
    }

    /* ---------- MAP READY ---------- */

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Default camera (Delhi)
        val defaultLatLng = LatLng(28.6139, 77.2090)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))

        map.setOnMapClickListener { latLng ->
            selectedLatLng = latLng
            updateMarker(latLng)
            updateCircle()
        }
    }

    /* ---------- MAP HELPERS ---------- */

    private fun updateMarker(latLng: LatLng) {
        marker?.remove()
        marker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Selected Location")
        )
    }

    private fun updateCircle() {
        val latLng = selectedLatLng ?: return

        circle?.remove()
        circle = map.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(radiusMeters.toDouble())
                .strokeColor(0x5500AAFF)
                .fillColor(0x2200AAFF)
        )
    }
}
