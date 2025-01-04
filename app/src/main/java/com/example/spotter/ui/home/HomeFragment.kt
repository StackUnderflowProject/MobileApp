package com.example.spotter.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.spotter.Event
import com.example.spotter.EventViewModel
import com.example.spotter.R
import com.example.spotter.SpotterApp
import com.example.spotter.databinding.FragmentHomeBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var map : MapView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var eventsViewModel : EventViewModel

    private var events : MutableList<Event> = mutableListOf<Event>()

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

        val bottomSheet = binding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 100
        bottomSheetBehavior.isDraggable = false

        eventsViewModel = (requireActivity().application as SpotterApp).eventsViewModel
        eventsViewModel.currentEvents.observe(viewLifecycleOwner, Observer {
            events = it
            showMarkers()
        })

        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showMarkers() {
        events.forEach { e ->
            run {
                val marker = Marker(map)
                marker.position = GeoPoint(e.location.coordinates[0], e.location.coordinates[1])
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.marker_activity)
                marker.icon = drawable

                marker.title = e.name
                marker.setOnMarkerClickListener { a, b ->
                    run {
                        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        } else {
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                        return@setOnMarkerClickListener true
                    }
                }
                map.overlays.add(marker)
            }
        }
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