package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.myapplication.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextName: EditText
    private lateinit var editTextAge: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var buttonSave: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var currentUserUid: String

    private lateinit var imageViewProfile: ImageView
    private lateinit var buttonSelectImage: Button

    private var imageUri: Uri? = null

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                imageUri = uri
                Picasso.get().load(imageUri).into(imageViewProfile)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        currentUserUid = auth.currentUser!!.uid

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextName = findViewById(R.id.editTextName)
        editTextAge = findViewById(R.id.editTextAge)
        editTextPhone = findViewById(R.id.editTextPhone)
        buttonSave = findViewById(R.id.buttonSave)

        imageViewProfile = findViewById(R.id.imageViewProfile)

        imageViewProfile.setOnClickListener {
            getContent.launch("image/*")
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    true
                }
                R.id.posts -> {
                    val intent = Intent(this@ProfileActivity, YourPosts::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.homep -> {
                    val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        fetchUserData()

        buttonSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun fetchUserData() {
        val userReference = database.child("users").child(currentUserUid)

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(User::class.java)
                if (userData != null) {
                    editTextEmail.setText(auth.currentUser!!.email)
                    editTextName.setText(userData.name)
                    editTextAge.setText(userData.age.toString())
                    editTextPhone.setText(userData.phoneNumber)
                    if (!userData.profilePicture.isNullOrEmpty()) {
                        Picasso.get().load(userData.profilePicture.toUri()).into(imageViewProfile)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun saveChanges() {
        val newName = editTextName.text.toString().trim()
        val newAge = editTextAge.text.toString().trim().toInt()
        val newEmail = editTextEmail.text.toString().trim()
        val newPhone= editTextPhone.text.toString().trim()

        val userUpdates = mutableMapOf<String, Any>(
            "name" to newName,
            "age" to newAge,
            "email" to newEmail,
            "phoneNumber" to newPhone
        )

        if (imageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_images").child(currentUserUid)
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        userUpdates["profilePicture"] = downloadUrl
                        updateUserData(userUpdates)
                    }.addOnFailureListener { e ->
                        // Handle failed to get download URL
                        Toast.makeText(baseContext, "Failed to get download URL", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    // Handle failed to upload image
                    Toast.makeText(baseContext, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        } else {
            updateUserData(userUpdates)
        }
    }


    private fun updateUserData(updates: Map<String, Any>) {
        database.child("users").child(currentUserUid)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        baseContext, "Success to update user profile.", Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        baseContext, "Failed to update user profile.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}