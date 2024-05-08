package com.example.myapplication

import WeatherMapService
import WeatherResponse
import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.datatypes.ExerciseRoute
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.example.myapplication.R.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class NewPost : AppCompatActivity() {

    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private val IMAGE_REQUEST_CODE = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 1002
    private lateinit var imageViewPost: ImageView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewTime: TextView

    private var selectedLocation: String = ""
    private var selectedHour: String = ""

    private var selectedImageUri: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var editTextContent: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(layout.activity_new_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        editTextContent = findViewById(id.editTextContent)
        imageViewPost = findViewById(id.imageViewPost)
        textViewLocation = findViewById(id.textViewLocation)
        textViewTime = findViewById(id.textViewTime)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()


        val buttonPost: Button = findViewById(id.buttonPost)
        buttonPost.setOnClickListener {
            post()
        }

        val bottomNavigationView: BottomNavigationView = findViewById(id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                id.profile -> {
                    val intent = Intent(this@NewPost, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                id.posts -> {
                    // Replace MainActivity with your desired activity for the "posts" button
                    val intent = Intent(this@NewPost, YourPosts::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                id.homep -> {
                    // Always navigate to MainActivity when "home" button is clicked
                    val intent = Intent(this@NewPost, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        Places.initialize(applicationContext, "AIzaSyAz0mzKXT-CG3aYYDFsFqEAhQNMF1ILro4")
    }

    private fun requestLocationPermission() {
ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Get the location details
                    val latitude = location.latitude
                    val longitude = location.longitude
                    selectedLocation = "$latitude, $longitude"
                    updateTextViews()
                } else {
                    // Location is null, handle accordingly
                }
            }
            .addOnFailureListener { e ->
                // Failed to get location
                Log.e("Location", "Failed to get location: ${e.message}")
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get current location
                getCurrentLocation()
            } else {
                // Permission denied, handle accordingly
            }
        }
    }


    fun selectImage(view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }



    fun selectLocation(view: View) {
        // Set the fields to specify the types of place data to return
        val fields = listOf(Place.Field.NAME, Place.Field.ADDRESS)

        // Start the autocomplete intent
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    fun selectTime(view: View) {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(
            this,
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                selectedHour = "$hourOfDay:$minute"
                updateTextViews()
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val place = Autocomplete.getPlaceFromIntent(data!!)
                        // Handle the selected place
                        // You can get place details like place.name, place.address, etc.
                        selectedLocation = " ${place.address}"
                        updateTextViews()
                    }
                    AutocompleteActivity.RESULT_ERROR -> {
                        val status = Autocomplete.getStatusFromIntent(data!!)
                        // Handle the error
                        textViewLocation.text = status.statusMessage
                    }
                    Activity.RESULT_CANCELED -> {
                        // The user canceled the operation
                        textViewLocation.text = "Please select a location"
                    }
                }
            }
            IMAGE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        imageViewPost.setImageURI(uri)
                        val textViewUploadPhoto = findViewById<TextView>(R.id.textViewUploadPhoto)
                        textViewUploadPhoto.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateTextViews() {
        textViewLocation.text = if (selectedLocation.isNotEmpty()) {
            selectedLocation
        } else {
            "Location"
        }

        textViewTime.text = if (selectedHour.isNotEmpty()) {
            "Selected Hour: $selectedHour"
        } else {
            "Time"
        }
    }



    private fun post() {
        val currentUser = auth.currentUser ?: return

        // Get user ID
        val userId = currentUser.uid

        //Get user name
        val userName = currentUser.displayName

        // Get post details
        val content = editTextContent?.text.toString()
        val location = selectedLocation
        val time = selectedHour
        val imageUrl = selectedImageUri?.toString() ?: ""




        // Check if an image is selected
        if (selectedImageUri != null) {
            // Get a reference to the Firebase Storage location where the image will be stored
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("images/${UUID.randomUUID()}")

            // Upload the image to Firebase Storage
            val uploadTask = imageRef.putFile(selectedImageUri!!)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                // Continue with the task to get the download URL
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Image uploaded successfully, get the download URL
                    val downloadUri = task.result

                    // Get a reference to the user's posts node
                    val userPostsRef = database.reference.child("users").child(userId).child("posts")

                    // Generate a unique ID for the new post
                    val postId = userPostsRef.push().key ?: ""

                    // Create a Post object with the download URL
                    val post = Post(postId ,content, location, time, downloadUri.toString(),0, mutableListOf(userName.toString()),userId)

                    // Save the post to the user's posts node with the generated ID
                    userPostsRef.child(postId).setValue(post)
                        .addOnSuccessListener {
                            // Post saved successfully
                            Toast.makeText(this@NewPost, "Post successful", Toast.LENGTH_SHORT).show()
                            //move to home page
                            val intent = Intent(this@NewPost, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                        }
                        .addOnFailureListener { e ->
                            // Failed to save post
                            Toast.makeText(this@NewPost, "Failed to post: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Image upload failed
                    val exception = task.exception
                    if (exception != null) {
                        // Log the error message
                        Log.e("Upload Image", "Failed to upload image: ${exception.message}")
                    }
                    Toast.makeText(this@NewPost, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
                    // Failed to save post
                    Toast.makeText(this@NewPost, "Failed to post", Toast.LENGTH_SHORT).show()
                }
        }
    }

