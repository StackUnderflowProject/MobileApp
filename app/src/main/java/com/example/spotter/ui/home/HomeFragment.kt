package com.example.spotter.ui.home

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
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
import org.osmdroid.views.overlay.Overlay


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

                val buttonOverlay = object : Overlay() {
                    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
                        if (canvas == null || mapView == null) return

                        // Convert GeoPoint to screen coordinates
                        val markerPosition = GeoPoint(e.location.coordinates[0], e.location.coordinates[1])
                        val point = Point()
                        mapView.projection.toPixels(markerPosition, point)

                        // Text and Paints
                        val buttonText = getString(R.string.btn_take_photo)
                        val buttonPaint = Paint().apply {
                            color = Color.BLUE
                            style = Paint.Style.FILL
                        }
                        val textPaint = Paint().apply {
                            color = Color.WHITE
                            textSize = 40f
                            textAlign = Paint.Align.LEFT
                        }

                        // Measure text width and height
                        val textBounds = Rect()
                        textPaint.getTextBounds(buttonText, 0, buttonText.length, textBounds)
                        val textWidth = textBounds.width()
                        val textHeight = textBounds.height()

                        // Load image as Bitmap
                        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.camera)
                        val iconBitmap = (drawable as BitmapDrawable).bitmap

                        // Icon dimensions
                        val iconWidth = iconBitmap.width
                        val iconHeight = iconBitmap.height

                        // Button padding
                        val padding = 20
                        val contentWidth = textWidth + iconWidth + 2 * padding + 10 // 10 = space between text and icon
                        val buttonWidth = contentWidth
                        val buttonHeight = maxOf(textHeight, iconHeight) + 2 * padding

                        // Button coordinates
                        val buttonX = point.x - buttonWidth / 2
                        val buttonY = point.y + 20 // Below marker

                        // Draw rounded button
                        val rect = RectF(
                            buttonX.toFloat(),
                            buttonY.toFloat(),
                            (buttonX + buttonWidth).toFloat(),
                            (buttonY + buttonHeight).toFloat()
                        )
                        canvas.drawRoundRect(rect, 25f, 25f, buttonPaint)

                        // Draw text on the button
                        val textX = buttonX + padding
                        val textY = rect.centerY() + textHeight / 2f
                        canvas.drawText(buttonText, textX.toFloat(), textY, textPaint)

                        // Draw icon to the right of the text
                        val iconX = textX + textWidth + 15 // 15 = space between text and icon
                        val iconY = rect.centerY() - iconHeight / 2
                        canvas.drawBitmap(iconBitmap, iconX.toFloat(), iconY.toFloat(), null)
                    }

                    override fun onSingleTapConfirmed(motionEvent: MotionEvent?, mapView: MapView?): Boolean {
                        if (motionEvent == null || mapView == null) return false

                        val tapPoint = Point(motionEvent.x.toInt(), motionEvent.y.toInt())

                        // Check if the tap is within the button bounds
                        val markerPosition = GeoPoint(e.location.coordinates[0], e.location.coordinates[1])
                        val markerScreenPoint = Point()
                        mapView.projection.toPixels(markerPosition, markerScreenPoint)

                        val buttonX = markerScreenPoint.x - 50 // Adjust to match button position
                        val buttonY = markerScreenPoint.y + 20 // Adjust to match button position
                        val buttonWidth = 100
                        val buttonHeight = 50

                        if (tapPoint.x in buttonX..(buttonX + buttonWidth) && tapPoint.y in buttonY..(buttonY + buttonHeight)) {
                            Toast.makeText(mapView.context, "Button clicked!", Toast.LENGTH_SHORT).show()
                            return true
                        }

                        return super.onSingleTapConfirmed(motionEvent, mapView)
                    }
                }
                map.overlays.add(buttonOverlay)
                map.invalidate()
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
