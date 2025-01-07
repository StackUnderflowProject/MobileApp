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
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.spotter.Event
import com.example.spotter.EventClickListener
import com.example.spotter.EventViewModel
import com.example.spotter.MainActivity
import com.example.spotter.R
import com.example.spotter.SpotterApp
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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.lang.reflect.Type


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

        binding.btnClose.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.btnFilter.setOnClickListener {
            binding.containerFilters.visibility = if (binding.containerFilters.visibility == View.GONE) View.VISIBLE else View.GONE
            binding.btnCloseFilters.visibility = if (binding.btnCloseFilters.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        binding.btnCloseFilters.setOnClickListener {
            binding.containerFilters.visibility = View.GONE
            binding.btnCloseFilters.visibility = View.GONE
        }

        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showMarkers() {
        map.overlays.clear()
        Log.i("Create", "---------")
        events.forEach { e ->
            Log.i("Create", "${e.name}")
            run {
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

        eventBinding!!.username.text = activeEvent!!.hostObj?.username ?: "Username not loaded"
        eventBinding!!.userEmail.text = activeEvent!!.hostObj?.email ?: "Email not loaded"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            aiBinding!!.dimmer.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Photo saved at: ${imageUri.toString()}", Toast.LENGTH_SHORT).show()
            Log.i("SIZE", "${getFileFromUri(requireContext(), imageUri!!)!!.length()}")
            getPredictedCount(myApp.user, activeEvent!!, createImagePart(compressImage(getFileFromUri(requireContext(), imageUri!!)))!!) { predictedCount ->
                if (predictedCount != -1) {
                    uploadImgResults(myApp.user, activeEvent as Event, createImagePart(getFileFromUri(requireContext(), imageUri!!))!!, predictedCount) { e ->
                        if (e != null) {
                            aiBinding!!.photoFrame.visibility = View.VISIBLE
                            aiBinding!!.description.text = "Estimated count: $predictedCount"
                            aiBinding!!.btnTakePhoto.text = "RETAKE"
                            aiBinding!!.btnApprove.visibility = View.VISIBLE
                            aiBinding!!.photoFrame.setImageURI(imageUri)
                            aiBinding!!.dimmer.visibility = View.GONE
                        }
                    }
                } else {
                    aiBinding!!.errorAI.text = "Request failed :("
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
            aiBinding!!.btnApprove.text = "UPLOADING..."
            uploadImgResults(myApp.user, activeEvent as Event, createImagePart(getFileFromUri(requireContext(), imageUri!!))!!, activeEvent!!.predicted_count!!) { e ->
                if (e != null) {
                    aiBinding!!.btnApprove.text = "SUCCESS"
                } else {
                    aiBinding!!.btnApprove.text = "FAILED"
                }
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
                        "Predicted count: ${e.predicted_count}"  // Dynamic count value
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
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
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
