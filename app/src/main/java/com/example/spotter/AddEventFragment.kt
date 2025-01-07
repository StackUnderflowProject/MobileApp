package com.example.spotter

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.ListFragment
import com.example.spotter.databinding.FragmentAddEventBinding
import com.example.spotter.ui.dashboard.DashboardFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.bson.types.ObjectId
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddEventFragment : Fragment() {
    private lateinit var myApp : SpotterApp
    private lateinit var eventsViewModel : EventViewModel
    private lateinit var mapView : MapView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        val binding: FragmentAddEventBinding = FragmentAddEventBinding.inflate(inflater, container, false)
        myApp = requireActivity().application as SpotterApp
        eventsViewModel = (requireActivity().application as SpotterApp).eventsViewModel

        mapView = binding.map
        mapView.setMultiTouchControls(true) // Enable zoom and pan gestures
        val startPoint = GeoPoint(46.5547, 15.6459)
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        mapController.setCenter(startPoint)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        addMapClickListener()

        val bottomSheet = binding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 100
        bottomSheetBehavior.isDraggable = false

        binding.inputDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                binding.inputDate.setText(selectedDate)
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        binding.inputTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                val selectedTime = "$hourOfDay:$minute"
                binding.inputTime.setText(selectedTime)
            }
            val timePickerDialog = TimePickerDialog(
                requireContext(), timeSetListener,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        }

        binding.btnCreateEvent.setOnClickListener {
            var formOK = true

            var nameErrorMsg : String = ""
            if (binding.inputName.text.isEmpty()) {nameErrorMsg = getString(R.string.error_empty_field); formOK = false;}

            var descriptionErrorMsg : String = ""
            if (binding.inputDescription.text.isEmpty()) {descriptionErrorMsg = getString(R.string.error_empty_field); formOK = false;}

            var activityErrorMsg : String = ""
            if (binding.inputActivity.text.isEmpty()) {activityErrorMsg = getString(R.string.error_empty_field); formOK = false;}

            var dateErrorMsg : String = ""
            if (binding.inputDate.text.isEmpty()) {dateErrorMsg = getString(R.string.error_empty_field); formOK = false;}

            var timeErrorMsg : String = ""
            if (binding.inputTime.text.isEmpty()) {timeErrorMsg = getString(R.string.error_empty_field); formOK = false;}

            if (formOK) {
                val event = Event(
                    binding.inputName.text.toString(),
                    binding.inputDescription.text.toString(),
                    binding.inputActivity.text.toString(),
                    convertToLocalDate(binding.inputDate.text.toString()),
                    binding.inputTime.text.toString(),
                    LOCATION("point", listOf(locationSelected!!.latitude, locationSelected!!.longitude)),
                    host = myApp.user?._id ?: ObjectId()
                )
                eventsViewModel.addItem(event, myApp.user) { success ->
                    if (success) {
                        DashboardFragment.scrollActive = true
                        DashboardFragment.scrollEvent = event
                        (activity as? MainActivity)?.launchFragment(
                            DashboardFragment(),
                            null
                        )
                    } else {
                        binding.errorLabel.visibility = View.VISIBLE
                    }
                }

                binding.inputName.setText(String())
                binding.inputDescription.setText(String())
                binding.inputActivity.setText(String())
                binding.inputDate.setText(String())
                binding.inputTime.setText(String())
                binding.errorLabel.visibility = View.GONE
                locationSelected = null
            } else {
                if (timeErrorMsg.isNotEmpty()) {binding.errorTime.text = timeErrorMsg; binding.errorTime.visibility = View.VISIBLE; binding.errorTime.requestFocus();}
                if (dateErrorMsg.isNotEmpty()) {binding.errorDate.text = dateErrorMsg; binding.errorDate.visibility = View.VISIBLE; binding.errorDate.requestFocus();}
                if (activityErrorMsg.isNotEmpty()) {binding.errorActivity.text = activityErrorMsg; binding.errorActivity.visibility = View.VISIBLE; binding.errorActivity.requestFocus();}
                if (descriptionErrorMsg.isNotEmpty()) {binding.errorDescription.text = descriptionErrorMsg; binding.errorDescription.visibility = View.VISIBLE; binding.errorDescription.requestFocus();}
                if (nameErrorMsg.isNotEmpty()) {binding.errorName.text = nameErrorMsg; binding.errorName.visibility = View.VISIBLE; binding.errorName.requestFocus();}
            }
        }

        return binding.root
    }


    private var animationPlaying = false
    private var locationSelected : GeoPoint? = null
    private fun addMapClickListener() {
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                showMarker(p)
                locationSelected = p
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                animationPlaying = true
                mapView.controller.animateTo(
                    GeoPoint(p.latitude - 0.01, p.longitude),    // Target position
                    15.0,           // Desired zoom level
                    1000L           // Animation duration in milliseconds
                )
                Handler().postDelayed({animationPlaying = false}, 1050L)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        })
        mapView.overlays?.add(mapEventsOverlay)
        mapView.setMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                if (event != null && !animationPlaying) bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            override fun onZoom(event: ZoomEvent?): Boolean {
                if (event != null && !animationPlaying) bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
        })
    }

    private fun showMarker(p : GeoPoint) {
        clearMarkers()
        val marker = Marker(mapView)
        marker.position = p
        val density = resources.displayMetrics.density
        val width = (44 * density).toInt() // Scale based on density
        val height = (64 * density).toInt()

        val markerDrawable = ResourcesCompat.getDrawable(resources, R.drawable.marker_activity, null)
        val scaledDrawable = BitmapDrawable(resources, Bitmap.createScaledBitmap(
            (markerDrawable as BitmapDrawable).bitmap,
            width,
            height,
            true
        ))
        marker.icon = scaledDrawable
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
    }

    private fun clearMarkers() {
        mapView.overlays.removeAll { it is Marker }
        mapView.invalidate()
    }

    private fun convertToLocalDate(dateString: String): LocalDate {
        val regex = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})$""")
        val matchResult = regex.matchEntire(dateString)

        return if (matchResult != null) {
            try {
                val (day, month, year) = matchResult.destructured
                LocalDate.of(year.toInt(), month.toInt(), day.toInt())
            } catch (e: Exception) {
                Log.i("Output", "Invalid date values in: $dateString")
                LocalDate.of(1970, 1, 1) // Fallback default date
            }
        } else {
            Log.i("Output", "Invalid date format: $dateString")
            LocalDate.of(1970, 1, 1) // Fallback default date
        }
    }
}
