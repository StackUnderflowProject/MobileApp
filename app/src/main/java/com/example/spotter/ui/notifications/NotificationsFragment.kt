package com.example.spotter.ui.notifications

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.UserManager
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.example.spotter.LoginFragment
import com.example.spotter.MainActivity
import com.example.spotter.R
import com.example.spotter.RetrofitInstance
import com.example.spotter.ServerResponse
import com.example.spotter.SpotterApp
import com.example.spotter.User
import com.example.spotter.databinding.FragmentUserProfileBinding
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var myApp: SpotterApp
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        myApp = requireActivity().application as SpotterApp

        sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isDarkMode: Boolean = sharedPreferences.getBoolean("dark_mode", true)
        val language = sharedPreferences.getString("language", "en") ?: "en"

        Picasso.get()
            .load(("http://77.38.76.152:3000/public/images/profile_pictures/" + myApp.user?.image) ?: "")
            .error(ContextCompat.getDrawable(requireContext(), R.drawable.download__5__removebg_preview)!!)
            .into(binding.imageProfilePicture)

        binding.labelUsername.text = myApp.user?.username ?: "Username"
        binding.labelEmail.text = myApp.user?.email ?: "example@gmail.com"

        binding.btnChangeProfilePicture.setOnClickListener {
            requestCameraPermission()
            binding.dimmer.visibility = View.VISIBLE
        }

        binding.btnLanguage.setOnClickListener {
            val languageEntries = arrayOf(getString(R.string.english), getString(R.string.slovenian), getString(R.string.german))
            val languageValues = arrayOf("en", "sl", "de")

            val currentIndex = languageValues.indexOf(language)

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(languageEntries, currentIndex) { dialog, which ->
                    // Save and apply selected language
                    val selectedLanguage = languageValues[which]
                    sharedPreferences.edit().putString("language", selectedLanguage).apply()
                    setLocale(selectedLanguage)

                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        binding.btnTheme.setOnClickListener {
            val options = arrayOf(getString(R.string.light_theme), getString(R.string.dark_theme))
            val optionsVal = arrayOf(false, true)

            val currentIndex = optionsVal.indexOf(isDarkMode)

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_theme))
                .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                    // Save and apply selected language
                    val selectedTheme = optionsVal[which]
                    sharedPreferences.edit().putBoolean("dark_mode", selectedTheme).apply()
                    AppCompatDelegate.setDefaultNightMode(
                        if (selectedTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )

                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        binding.btnLogout.setOnClickListener {
            myApp.user = null
            (requireActivity() as MainActivity).launchFragment(LoginFragment())
        }

        return binding.root
    }

    private fun setLocale(language: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    // upload profile picture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            if (myApp.user == null) return
            Log.i("Output", createImagePart(compressImage(getFileFromUri(requireContext(), imageUri!!)))!!.toString())
            RetrofitInstance.api.uploadProfilePicture("Bearer: " + myApp.user!!.token, createImagePart(compressImage(getFileFromUri(requireContext(), imageUri!!)))!!).enqueue(object : Callback<User> {
                override fun onResponse(
                    call: Call<User>,
                    response: Response<User>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {myApp.user!!.image = response.body()!!.image; myApp.storeUser(requireContext(), myApp.user!!)}
                        binding.imageProfilePicture.setImageURI(imageUri)
                    }
                }
                override fun onFailure(call: Call<User>, t: Throwable) {
                    Log.i("Output", "Failed ${t.message}")
                }
            })
            binding.dimmer.visibility = View.GONE
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
        return MultipartBody.Part.createFormData("profile_picture", file.name, requestBody)
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