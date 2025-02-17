package com.example.spotter

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.util.Calendar
import com.example.spotter.databinding.FragmentUpdateEventBinding
import com.example.spotter.ui.dashboard.DashboardFragment
import com.example.spotter.ui.home.LocalDateDeserializer
import com.example.spotter.ui.home.LocalDateSerializer
import com.example.spotter.ui.home.ObjectIdSerializer
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.bson.types.ObjectId
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.time.LocalDate

class UpdateEventFragment : Fragment() {
    private lateinit var myApp : SpotterApp
    private lateinit var eventsViewModel : EventViewModel
    private lateinit var mapView : MapView
    private lateinit var binding: FragmentUpdateEventBinding
    private lateinit var gson : Gson
    private var globalEvent : Event? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentUpdateEventBinding.inflate(inflater, container, false)
        myApp = requireActivity().application as SpotterApp
        eventsViewModel = (requireActivity().application as SpotterApp).eventsViewModel

        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        gsonBuilder.registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
        gsonBuilder.registerTypeAdapter(ObjectId::class.java, ObjectIdSerializer())
        gsonBuilder.registerTypeAdapter(ObjectId::class.java, RetrofitInstance.ObjectIdDeserializer())
        gson = gsonBuilder.create()

        arguments?.let {
            val eventJSON = requireArguments().getString("updateEvent", "")
            if (eventJSON.isNotEmpty()) {
                val event = gson.fromJson(eventJSON, Event::class.java)
                globalEvent = event

                mapView = binding.map

                locationSelected = GeoPoint(event.location.coordinates[1], event.location.coordinates[0])

                binding.inputName.setText(event.name)
                binding.inputDescription.setText(event.description)
                binding.inputActivity.setText(event.activity)
                binding.inputDate.setText("${event.date.dayOfMonth}/${event.date.monthValue}/${event.date.year}")
                binding.inputTime.setText(event.time)

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

                binding.btnChangeLocation.setOnClickListener {
                    mapView.setMultiTouchControls(true) // Enable zoom and pan gestures
                    val startPoint = GeoPoint(event.location.coordinates[1], event.location.coordinates[0])
                    val mapController = mapView.controller
                    mapController.setZoom(15.0)
                    mapController.setCenter(startPoint)
                    showMarker(startPoint)
                    mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    addMapClickListener()
                    binding.mapContainer.visibility = View.VISIBLE
                }

                binding.btnSaveLocation.setOnClickListener {
                    binding.mapContainer.visibility = View.GONE
                }

                binding.btnSaveEvent.setOnClickListener {
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
                        binding.dimmer.visibility = View.VISIBLE
                        val newEvent = Event(
                            binding.inputName.text.toString(),
                            binding.inputDescription.text.toString(),
                            binding.inputActivity.text.toString(),
                            convertToLocalDate(binding.inputDate.text.toString()),
                            binding.inputTime.text.toString(),
                            LOCATION("point", listOf(locationSelected!!.longitude, locationSelected!!.latitude)),
                            host = myApp.user?._id ?: ObjectId(),
                            _id = event._id
                        )
                        eventsViewModel.updateItem(newEvent, myApp.user) { success ->
                            binding.dimmer.visibility = View.GONE
                            if (success) {
                                DashboardFragment.scrollActive = true
                                DashboardFragment.scrollEvent = event
                                requireActivity().supportFragmentManager.popBackStack("UpdateEventFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            } else {
                                binding.errorLabel.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        if (timeErrorMsg.isNotEmpty()) {binding.errorTime.text = timeErrorMsg; binding.errorTime.visibility = View.VISIBLE; binding.errorTime.requestFocus();}
                        if (dateErrorMsg.isNotEmpty()) {binding.errorDate.text = dateErrorMsg; binding.errorDate.visibility = View.VISIBLE; binding.errorDate.requestFocus();}
                        if (activityErrorMsg.isNotEmpty()) {binding.errorActivity.text = activityErrorMsg; binding.errorActivity.visibility = View.VISIBLE; binding.errorActivity.requestFocus();}
                        if (descriptionErrorMsg.isNotEmpty()) {binding.errorDescription.text = descriptionErrorMsg; binding.errorDescription.visibility = View.VISIBLE; binding.errorDescription.requestFocus();}
                        if (nameErrorMsg.isNotEmpty()) {binding.errorName.text = nameErrorMsg; binding.errorName.visibility = View.VISIBLE; binding.errorName.requestFocus();}
                    }
                }
            }
        }

        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.mapContainer.visibility == View.VISIBLE) binding.mapContainer.visibility = View.GONE
                else {
                    DashboardFragment.scrollActive = true
                    DashboardFragment.scrollEvent = globalEvent
                    requireActivity().supportFragmentManager.popBackStack("UpdateEventFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE)
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        return binding.root
    }

    private var locationSelected : GeoPoint? = null
    private fun addMapClickListener() {
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                showMarker(p)
                locationSelected = p
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        })
        mapView.overlays?.add(mapEventsOverlay)
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
        marker.infoWindow = null
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
