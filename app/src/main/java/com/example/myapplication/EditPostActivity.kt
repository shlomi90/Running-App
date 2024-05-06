package com.example.myapplication

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import com.squareup.picasso.Picasso
import java.util.Calendar

class EditPostActivity : AppCompatActivity() {

    private lateinit var imageViewPost: ImageView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewTime: TextView
    private var selectedLocation: String = ""
    private var selectedHour: String = ""
    private var selectedImageUri: Uri? = null
    private var editTextContent: EditText? = null
    private lateinit var deleteButton: Button
    private lateinit var updateButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_post)

        // Initialize views
        imageViewPost = findViewById(R.id.imageViewPost)
        textViewLocation = findViewById(R.id.textViewLocation)
        textViewTime = findViewById(R.id.textViewTime)
        editTextContent = findViewById(R.id.editTextContent)
        deleteButton = findViewById(R.id.buttonDelete)
        updateButton = findViewById(R.id.buttonUpdate)


        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    val intent = Intent(this@EditPostActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.posts -> {
                    // Replace MainActivity with your desired activity for the "posts" button
                    val intent = Intent(this@EditPostActivity, YourPosts::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.homep -> {
                    // Always navigate to MainActivity when "home" button is clicked
                    val intent = Intent(this@EditPostActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Retrieve post details from intent extras
        val postId = intent.getStringExtra("postId")
        Log.d("EditPostActivity", "postKey: $postId")

        val post = intent.getParcelableExtra<Post>("post")



        // Set post details to the views
        editTextContent?.setText(post?.content)
        textViewLocation.text = post?.location
        textViewTime.text = post?.time

        if (!post?.imageUrl.isNullOrEmpty()) {
            Picasso.get().load(post?.imageUrl).into(imageViewPost)
        }

        Places.initialize(applicationContext, "AIzaSyAz0mzKXT-CG3aYYDFsFqEAhQNMF1ILro4")

        // Set click listeners for selecting image, location, and time
        imageViewPost.setOnClickListener { selectImage() }
        textViewLocation.setOnClickListener { selectLocation() }
        textViewTime.setOnClickListener { selectTime() }
        deleteButton.setOnClickListener(deletePost(intent.getStringExtra("postId")))
        updateButton.setOnClickListener(updatePost(intent.getStringExtra("postId")))

    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    private fun selectLocation() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    private fun selectTime() {
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

    private fun deletePost(postId: String?): View.OnClickListener {
        return View.OnClickListener {
            Log.d("EditPostActivity", "deletePost called")
            if (postId != null) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val databaseRef = FirebaseDatabase.getInstance().reference.child("users")
                        .child(currentUser.uid)
                        .child("posts")
                        .child(postId)

                    databaseRef.removeValue()
                        .addOnSuccessListener {
                            Log.d("EditPostActivity", "Post deleted successfully")
                            Toast.makeText(this, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                            finish() // Finish the EditPostActivity
                        }
                        .addOnFailureListener { exception ->
                            Log.e("EditPostActivity", "Failed to delete post: ${exception.message}")
                            Toast.makeText(this, "Failed to delete post: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }


    private fun updatePost(postKey: String?): View.OnClickListener {
        return View.OnClickListener {
            Log.d("EditPostActivity", "updatePost called")
            if (postKey != null) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val databaseRef = FirebaseDatabase.getInstance().reference.child("users")
                        .child(currentUser.uid)
                        .child("posts")
                        .child(postKey)

                    // Retrieve the existing post data from Firebase
                    databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                val existingPost = dataSnapshot.getValue(Post::class.java)

                                // Update the fields if they have been modified
                                existingPost?.let {
                                    val updatedContent = editTextContent?.text.toString().trim()
                                    if (!updatedContent.isNullOrEmpty()) {
                                        it.content = updatedContent
                                    }

                                    val updatedLocation = selectedLocation
                                    if (updatedLocation.isNotEmpty()) {
                                        it.location = updatedLocation
                                    }

                                    val updatedTime = selectedHour
                                    if (updatedTime.isNotEmpty()) {
                                        it.time = updatedTime
                                    }

                                    val updatedImageUrl = selectedImageUri?.toString()
                                    if (!updatedImageUrl.isNullOrEmpty()) {
                                        it.imageUrl = updatedImageUrl
                                    }

                                    // Set the updated post data back to Firebase
                                    databaseRef.setValue(existingPost)
                                        .addOnSuccessListener {
                                            Log.d("EditPostActivity", "Post updated successfully")
                                            Toast.makeText(this@EditPostActivity, "Post updated successfully", Toast.LENGTH_SHORT).show()
                                            finish() // Finish the EditPostActivity
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.e("EditPostActivity", "Failed to update post: ${exception.message}")
                                            Toast.makeText(this@EditPostActivity, "Failed to update post: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("EditPostActivity", "Failed to retrieve post data: ${databaseError.message}")
                        }
                    })
                }
            }
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AUTOCOMPLETE_REQUEST_CODE -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    selectedLocation = "${place.name}, ${place.address}"
                    updateTextViews()
                }
                IMAGE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        imageViewPost.setImageURI(uri)
                    }
                }
            }
        }
    }

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 1
        private const val IMAGE_REQUEST_CODE = 1001
    }
}