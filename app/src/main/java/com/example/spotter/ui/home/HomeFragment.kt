package com.example.spotter.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.spotter.R
import com.example.spotter.databinding.FragmentHomeBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var map : MapView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        Configuration.getInstance().load(requireContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))

        // Initialize the map
        map = binding.map

        showMarkers()

        map.setMultiTouchControls(true) // Enable zoom and pan gestures
        val startPoint = GeoPoint(46.5547, 15.6459)
        val mapController = map.controller
        mapController.setZoom(15.0)
        mapController.setCenter(startPoint)

        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showMarkers() {
        val marker = Marker(map)
        marker.position = GeoPoint(46.5547, 15.6459)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = resources.getDrawable(R.drawable.marker_activity, null)
        marker.title = "Eiffel Tower"
        map.overlays.add(marker)
    }

    override fun onPause() {
        super.onPause()
        map.onPause() // Pause the map
    }

    override fun onResume() {
        super.onResume()
        map.onResume() // Resume the map
    }
}