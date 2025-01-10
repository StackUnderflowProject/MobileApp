package com.example.spotter.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.spotter.Category
import com.example.spotter.Event
import com.example.spotter.EventViewModel
import com.example.spotter.Filter
import com.example.spotter.R
import com.example.spotter.SpotterApp
import com.example.spotter.TimeInterval
import com.example.spotter.databinding.ActivityMainBinding
import com.example.spotter.databinding.FragmentEventBinding
import com.example.spotter.databinding.FragmentHomeBinding
import com.example.spotter.databinding.FragmentImgAiBinding
import com.example.spotter.getPredictedCount
import com.example.spotter.ui.dashboard.DashboardFragment
import com.example.spotter.uploadImgResults
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.bson.types.ObjectId
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.lang.reflect.Type
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var map : MapView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var eventsViewModel : EventViewModel
    private var mainBinding : ActivityMainBinding? = null
    private lateinit var myApp : SpotterApp

    private var activeEvent : Event? = null
    private var aiBinding : FragmentImgAiBinding? = null

    private var eventBinding : FragmentEventBinding? = null

    private var events : MutableList<Event> = mutableListOf<Event>()

    private val MAX_DISTANCE = 100
    private var circleOverlay: Polygon? = null
    private val filter = Filter()
    private var programaticSeekbarChange = false
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        myApp = requireActivity().application as SpotterApp
        val containerParent = container?.parent as? View
        mainBinding = containerParent?.let { ActivityMainBinding.bind(it) }

        Configuration.getInstance().load(requireContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))

        // Initialize the map
        map = binding.map

        map.setMultiTouchControls(true) // Enable zoom and pan gestures
        val mapController = map.controller
        mapController.setZoom(15.0)
        val startPoint = GeoPoint(46.1512, 14.9955)
        mapController.setCenter(startPoint)

        val LOCATION_PERMISSION_REQUEST_CODE = 1001
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        myLocationOverlay = MyLocationNewOverlay(map)
        myLocationOverlay.enableMyLocation()

        myLocationOverlay.runOnFirstFix {
            val myLocation: Location? = myLocationOverlay.lastFix
            if (myLocation != null) {
                val geoPoint = GeoPoint(myLocation.latitude, myLocation.longitude)
                requireActivity().runOnUiThread {
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(geoPoint)
                    if (::map.isInitialized) showMyLocation(geoPoint)
                }
            }
        }

        val bottomSheet = binding.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 100
        bottomSheetBehavior.isDraggable = false

        eventsViewModel = (requireActivity().application as SpotterApp).eventsViewModel
        eventsViewModel.currentEvents.observe(viewLifecycleOwner, Observer {
            events = it
            refreshMap()
        })

        binding.btnClose.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        bindFilterView()

        return binding.root
    }

    private fun refreshMap() {
        map.overlays.clear()
        map.overlays.add(myLocationOverlay)
        showMarkers()
        val myLocation: Location? = myLocationOverlay.lastFix
        if (myLocation != null) showMyLocation(GeoPoint(myLocation.latitude, myLocation.longitude))
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showMarkers() {
        map.overlays.clear()
        var startPos = GeoPoint(46.1512, 14.9955)
        val myLocation: Location? = myLocationOverlay.lastFix
        if (myLocation != null) startPos = GeoPoint(myLocation.latitude, myLocation.longitude)
        events.forEach { e ->
            run {
                if (!filter.isEventOk(startPos, e)) return@run
                val marker = Marker(map)
                marker.position = GeoPoint(e.location.coordinates[0], e.location.coordinates[1])
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.marker_activity)
                marker.icon = drawable

                marker.title = e.name
                marker.setOnMarkerClickListener { a, b ->
                    run {
                        activeEvent = e
                        showEvent()
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        return@setOnMarkerClickListener true
                    }
                }
                map.overlays.add(marker)

                addPredictButton(e)
                addPredictLabel(e)
            }
        }
        map.invalidate()
    }

    private fun showEvent() {
        val inflater = LayoutInflater.from(requireContext())
        eventBinding = FragmentEventBinding.inflate(inflater, binding.sheetContainer, false)
        binding.sheetContainer.removeAllViews()
        binding.sheetContainer.addView(eventBinding!!.root)

        eventBinding!!.username.text = activeEvent!!.hostObj?.username ?: getString(R.string.username_not_loaded)
        eventBinding!!.userEmail.text = activeEvent!!.hostObj?.email ?: getString(R.string.email_not_loaded)
        eventBinding!!.eventDate.text = activeEvent!!.date.toString()
        eventBinding!!.eventTime.text = activeEvent!!.time
        eventBinding!!.title.text = activeEvent!!.name
        eventBinding!!.description.text = activeEvent!!.description
        eventBinding!!.location.text = activeEvent!!.location.toString()
        eventBinding!!.activity.text = activeEvent!!.activity
        eventBinding!!.activityIcon.setImageDrawable(
            when (activeEvent!!.activity.lowercase()) {
                "nogomet", "futsal", "football" -> ContextCompat.getDrawable(requireContext(), R.drawable.download_removebg_preview__1_)
                "rokomet", "handball" -> ContextCompat.getDrawable(requireContext(), R.drawable.group_91)
                else -> ContextCompat.getDrawable(requireContext(), R.drawable.group_92)
            })
        eventBinding!!.subscribeCount.visibility = View.GONE
        eventBinding!!.imgSubscribers.visibility = View.GONE

        if (activeEvent!!.hostObj == null || activeEvent!!.hostObj?.image.isNullOrEmpty()) {
            eventBinding!!.userIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.download__5__removebg_preview))
        } else {
            Picasso.get()
                .load("http://77.38.76.152:3000/public/images/profile_pictures/" + (activeEvent!!.hostObj?.image ?: ""))
                .error(ContextCompat.getDrawable(requireContext(), R.drawable.download__5__removebg_preview)!!)
                .into(eventBinding!!.userIcon)
        }

        eventBinding!!.btnSubscribe.visibility = View.GONE
        eventBinding!!.btnOptions.visibility = View.GONE

        eventBinding!!.btnGoToEvent.visibility = View.VISIBLE
        eventBinding!!.btnGoToEvent.setOnClickListener {
            DashboardFragment.scrollActive = true
            DashboardFragment.scrollEvent = activeEvent
            val navController = findNavController()
            navController.navigate(R.id.navigation_dashboard)
        }
    }

    private fun showMyLocation(p : GeoPoint) {
        if (!::map.isInitialized) return
        map.post {
            val marker = Marker(map)
            marker.position = p
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.mylocation_marker)
            marker.icon = drawable

            map.overlays.add(marker)
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

    @SuppressLint("SetTextI18n")
    private fun bindFilterView() {
        binding.btnFilter.setOnClickListener {
            binding.containerFilters.visibility = if (binding.containerFilters.visibility == View.GONE) View.VISIBLE else View.GONE
            binding.btnCloseFilters.visibility = if (binding.btnCloseFilters.visibility == View.GONE) View.VISIBLE else View.GONE
            binding.labelDistance.text = "${((binding.seekbarDistance.progress / 100.0) * MAX_DISTANCE).toInt()}km"
        }

        binding.seekbarDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (programaticSeekbarChange) {programaticSeekbarChange = false; return;}
                binding.labelDistance.text = if (progress != 0) "${((progress / 100.0) * MAX_DISTANCE).toInt()}km" else getString(R.string.none)
                binding.containerFilters.background.alpha = 100
                val location = if (myLocationOverlay.lastFix != null) GeoPoint(myLocationOverlay.lastFix.latitude, myLocationOverlay.lastFix.longitude) else GeoPoint(46.1512, 14.9955)
                map.controller.setCenter(location)
                setZoomForCircle(map, location, (progress / 100.0) * MAX_DISTANCE)
                circleOverlay?.let {
                    map.overlays.remove(circleOverlay)
                    circleOverlay = null
                }
                drawCircleOnMap(map, location, (progress / 100.0) * MAX_DISTANCE)
                filter.distance = ((progress / 100.0) * MAX_DISTANCE).toInt()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                binding.containerFilters.background.alpha = 255
                circleOverlay?.let {
                    map.overlays.remove(circleOverlay)
                    circleOverlay = null
                }
                if (filter.isDefault()) binding.btnRestoreDefaults.setBackgroundColor(Color.GRAY)
                else binding.btnRestoreDefaults.setBackgroundColor(Color.parseColor("#b3b3b3"))
                refreshMap()
            }
        })

        val spinnerCategory: Spinner = binding.spinnerCategory
        val itemsCategory = listOf("All", "Football", "Handball", "Other")
        val adapterCategory = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, itemsCategory)
        adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapterCategory

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = itemsCategory[position]
                filter.category = Category.fromString(selectedItem)
                if (filter.isDefault()) binding.btnRestoreDefaults.setBackgroundColor(Color.GRAY)
                else binding.btnRestoreDefaults.setBackgroundColor(Color.parseColor("#b3b3b3"))
                refreshMap()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle no selection
            }
        }

        val spinnerTime: Spinner = binding.spinnerTime
        val itemsTime = listOf("All", "Upcoming", "Today", "Week", "Month")
        val adapterTime = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, itemsTime)
        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTime.adapter = adapterTime

        spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = itemsTime[position]
                filter.time = TimeInterval.fromString(selectedItem)
                if (filter.isDefault()) binding.btnRestoreDefaults.setBackgroundColor(Color.GRAY)
                else binding.btnRestoreDefaults.setBackgroundColor(Color.parseColor("#b3b3b3"))
                refreshMap()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle no selection
            }
        }

        binding.btnRestoreDefaults.setOnClickListener {
            if (!filter.isDefault()) {
                filter.restoreDefaults()
                binding.spinnerCategory.setSelection(0)
                binding.spinnerTime.setSelection(0)
                programaticSeekbarChange = true
                binding.seekbarDistance.progress = 0
                binding.labelDistance.text = getString(R.string.none)
                binding.btnRestoreDefaults.setBackgroundColor(Color.GRAY)
                refreshMap()
            }
        }

        binding.btnCloseFilters.setOnClickListener {
            binding.containerFilters.visibility = View.GONE
            binding.btnCloseFilters.visibility = View.GONE
        }
    }
    private fun drawCircleOnMap(map: MapView, center: GeoPoint, radius: Double) {
        val circle = Polygon(map).apply {
            outlinePaint.color = android.graphics.Color.BLUE
            outlinePaint.strokeWidth = 4f
            fillPaint.color = android.graphics.Color.argb(50, 0, 0, 255) // 50% transparent blue
        }
        circleOverlay = circle

        val points = calculateCirclePoints(center, radius * 1000)
        circle.points = points

        map.overlays.add(circleOverlay)
        map.invalidate()
    }
    private fun calculateCirclePoints(center: GeoPoint, radiusInMeters: Double, numPoints: Int = 360): List<GeoPoint> {
        val earthRadius = 6371000.0 // Radius of the Earth in meters
        val points = mutableListOf<GeoPoint>()

        val radiusInRadians = radiusInMeters / earthRadius

        val centerLatRadians = Math.toRadians(center.latitude)
        val centerLonRadians = Math.toRadians(center.longitude)

        for (i in 0 until numPoints) {
            val angle = 2 * PI * i / numPoints // Angle in radians

            val lat = Math.asin(
                sin(centerLatRadians) * cos(radiusInRadians) +
                        cos(centerLatRadians) * sin(radiusInRadians) * cos(angle)
            )
            val lon = centerLonRadians + Math.atan2(
                sin(angle) * sin(radiusInRadians) * cos(centerLatRadians),
                cos(radiusInRadians) - sin(centerLatRadians) * sin(lat)
            )
            points.add(GeoPoint(Math.toDegrees(lat), Math.toDegrees(lon)))
        }

        points.add(points[0])
        return points
    }
    private fun setZoomForCircle(mapView: MapView, center: GeoPoint, radiusParam: Double) {
        var radius = radiusParam
        if (radiusParam == 0.0) radius = 300.0

        val radiusInDegrees = (radius * 1000) / 111320.0 // 1 degree latitude ≈ 111.32 km

        val north = center.latitude + radiusInDegrees
        val south = center.latitude - radiusInDegrees
        val east = center.longitude + (radiusInDegrees / Math.cos(Math.toRadians(center.latitude)))
        val west = center.longitude - (radiusInDegrees / Math.cos(Math.toRadians(center.latitude)))

        val boundingBox = BoundingBox(north, east, south, west)
        mapView.zoomToBoundingBox(boundingBox, true) // 'true' for animated zoom
    }


    private fun addPredictButton(e: Event) {
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
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.camera_white)
                val iconBitmap = (drawable as BitmapDrawable).bitmap

                // Icon dimensions
                val iconWidth = iconBitmap.width
                val iconHeight = iconBitmap.height

                // Button padding
                val padding = 20
                val contentWidth = textWidth + iconWidth + 2 * padding + 10 // 10 = space between text and icon
                val buttonWidth = contentWidth
                val buttonHeight = maxOf(textHeight, iconHeight) + 2 * padding

                // Button coordinates (center the button at the marker's position)
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

                // Get the button's on-screen coordinates
                val markerPosition = GeoPoint(e.location.coordinates[0], e.location.coordinates[1])
                val markerScreenPoint = Point()
                mapView.projection.toPixels(markerPosition, markerScreenPoint)

                val buttonX = markerScreenPoint.x - 200 // Adjust to match button position
                val buttonY = markerScreenPoint.y - 100 // Adjust to match button position
                val buttonWidth = 400 // Update with dynamic width from button content
                val buttonHeight = 200 // Update with dynamic height

                // Check if the tap is within the button bounds
                if (tapPoint.x in buttonX..(buttonX + buttonWidth) && tapPoint.y in buttonY..(buttonY + buttonHeight)) {
                    activeEvent = e
                    val inflater = LayoutInflater.from(requireContext())
                    aiBinding = FragmentImgAiBinding.inflate(inflater, binding.sheetContainer, false)
                    binding.sheetContainer.removeAllViews()
                    binding.sheetContainer.addView(aiBinding!!.root)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    handlePictureUpload()
                    return true
                }

                return super.onSingleTapConfirmed(motionEvent, mapView)
            }
        }
        map.overlays.add(buttonOverlay)
    }
    private fun addPredictLabel(e: Event) {
        if (e.predicted_count != null && e.predicted_count > -1) {
            val predictedCountOverlay = object : Overlay() {
                override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
                    if (canvas == null || mapView == null) return

                    // Example marker position (replace this with the actual GeoPoint of your marker)
                    val markerPosition = GeoPoint(e.location.coordinates[0], e.location.coordinates[1])
                    val point = Point()
                    mapView.projection.toPixels(markerPosition, point)

                    // Text and Paints for the predicted count label
                    val predictedCountText =
                        getString(R.string.predicted_count, e.predicted_count)  // Dynamic count value
                    val textPaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 40f
                        textAlign = Paint.Align.CENTER
                    }

                    // Measure text width and height
                    val textBounds = Rect()
                    textPaint.getTextBounds(
                        predictedCountText,
                        0,
                        predictedCountText.length,
                        textBounds
                    )
                    val textWidth = textBounds.width()
                    val textHeight = textBounds.height()

                    // Position for the label (above the marker)
                    val labelX = point.x
                    val labelY =
                        point.y - 200  // Adjust the Y offset to move the label higher or lower

                    // Draw text label
                    canvas.drawText(
                        predictedCountText,
                        labelX.toFloat(),
                        (labelY - textHeight / 2).toFloat(),
                        textPaint
                    )
                }
            }
            map.overlays.add(predictedCountOverlay)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            aiBinding!!.dimmer.visibility = View.VISIBLE
            Log.i("SIZE", "${getFileFromUri(requireContext(), imageUri!!)!!.length()}")
            getPredictedCount(myApp.user, activeEvent!!, createImagePart(compressImage(getFileFromUri(requireContext(), imageUri!!)))!!) { predictedCount ->
                if (predictedCount != -1) {
                    uploadImgResults(myApp.user, activeEvent as Event, createImagePart(getFileFromUri(requireContext(), imageUri!!))!!, predictedCount) { e ->
                        if (e != null) {
                            aiBinding!!.photoFrame.visibility = View.VISIBLE
                            aiBinding!!.description.text = getString(R.string.estimated_count, predictedCount)
                            aiBinding!!.btnTakePhoto.text = getString(R.string.retake)
                            aiBinding!!.btnApprove.visibility = View.VISIBLE
                            aiBinding!!.photoFrame.setImageURI(imageUri)
                            aiBinding!!.dimmer.visibility = View.GONE
                        }
                    }
                } else {
                    aiBinding!!.errorAI.text = getString(R.string.error_ai)
                    aiBinding!!.errorAI.visibility = View.VISIBLE
                    aiBinding!!.dimmer.visibility = View.GONE
                }
            }
        }
    }
    private fun handlePictureUpload() {
        aiBinding!!.btnTakePhoto.setOnClickListener {
            requestCameraPermission()
        }
        aiBinding!!.btnApprove.setOnClickListener {
            aiBinding!!.btnApprove.text = getString(R.string.uploading)
            uploadImgResults(myApp.user, activeEvent as Event, createImagePart(getFileFromUri(requireContext(), imageUri!!))!!, activeEvent!!.predicted_count!!) { e ->
                if (e != null) {
                    aiBinding!!.btnApprove.text = getString(R.string.success)
                } else {
                    aiBinding!!.btnApprove.text = getString(R.string.failed)
                }
            }
        }
    }
    private val CAMERA_REQUEST_CODE = 100
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            openCamera()
        }
    }
    private val IMAGE_CAPTURE_CODE = 101
    private var imageUri: Uri? = null
    private fun openCamera() {
        val photoFile = createImageFile()
        imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val aspectRatio: Float = bitmap.width.toFloat() / bitmap.height.toFloat()
        var width = maxWidth
        var height = maxHeight
        if (bitmap.width > bitmap.height) {
            height = (width / aspectRatio).toInt()
        } else {
            width = (height * aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }
    fun compressImage(imageFile: File?): File? {
        if (imageFile == null || !imageFile.exists()) return null
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val maxWidth = 1024
        val maxHeight = 1024
        val resizedBitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
        val compressedFile = File(requireContext().cacheDir, "compressed_${imageFile.name}")
        val outputStream = FileOutputStream(compressedFile)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.close()
        return compressedFile
    }
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
    fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val file = File(context.cacheDir, "temp_image")
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = file.outputStream()
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    fun createImagePart(file: File?): MultipartBody.Part? {
        if (file == null || !file.exists()) {
            Log.e("Error", "File does not exist")
            return null
        }

        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("image", file.name, requestBody)
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class LocalDateDeserializer : JsonDeserializer<LocalDate> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate {
        return LocalDate.parse(json?.asString, DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
class LocalDateSerializer : JsonSerializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
}
class ObjectIdSerializer : JsonSerializer<ObjectId> {
    override fun serialize(
        src: ObjectId?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        // Return the string representation of the ObjectId
        return JsonPrimitive(src?.toString() ?: "")
    }
}
