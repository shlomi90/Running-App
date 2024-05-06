package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class Register : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextAge: TextInputEditText
    private lateinit var editTextPhone: TextInputEditText
    private lateinit var imageViewProfile: ImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var editTextName: TextInputEditText
    private lateinit var buttonReg: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private var selectedImageUri: Uri? = null
    private lateinit var storageReference: StorageReference
    private lateinit var textView: TextView


    companion object {
        private const val IMAGE_PICK_REQUEST_CODE = 1001
        private const val PROFILE_IMAGES_FOLDER = "profile_images"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference.child(PROFILE_IMAGES_FOLDER)

        // Initialize views
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        editTextAge = findViewById(R.id.age)
        editTextPhone = findViewById(R.id.phone)
        imageViewProfile = findViewById(R.id.imageViewProfile)
        editTextName = findViewById(R.id.name)
        buttonReg = findViewById(R.id.btn_register)
        progressBar = findViewById(R.id.progressBarRegister)
        textView=findViewById(R.id.loginNow)

        imageViewProfile.setOnClickListener {
            selectImage()
        }

        buttonReg.setOnClickListener {
            registerUser()
        }

        textView.setOnClickListener {
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
    }

    private fun registerUser() {
        progressBar.visibility = View.VISIBLE
        val email = editTextEmail.text.toString()
        val password = editTextPassword.text.toString()
        val name = editTextName.text.toString()
        val age = editTextAge.text.toString()
        val phone = editTextPhone.text.toString()

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(name) || TextUtils.isEmpty(
                age) || TextUtils.isEmpty(phone) || selectedImageUri == null
        ) {
            Toast.makeText(this@Register, "Please fill in all fields", Toast.LENGTH_SHORT)
                .show()
            progressBar.visibility = View.GONE
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    uploadProfileImage(user?.uid ?: "")
                } else {
                    Toast.makeText(
                        baseContext, "Authentication failed.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun uploadProfileImage(userId: String) {
        val imageRef = storageReference.child("$userId.jpg")
        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserProfile(userId, uri.toString())
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        baseContext, "Failed to upload profile image.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    baseContext, "Failed to upload profile image.", Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUserProfile(userId: String, imageUrl: String) {
        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(editTextName.text.toString())
            .setPhotoUri(Uri.parse(imageUrl))
            .build()
        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userData = User(
                        editTextEmail.text.toString(),
                        editTextName.text.toString(),
                        editTextAge.text.toString().toInt(),
                        editTextPhone.text.toString(),
                        imageUrl
                    )
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(userId)
                        .setValue(userData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                baseContext, "Account created successfully.", Toast.LENGTH_SHORT
                            ).show()
                            Toast.makeText(
                                baseContext, "Welcome, ${user?.displayName}", Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(applicationContext, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                baseContext, "Failed to create account: ${e.message}", Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        baseContext, "Failed to update user profile.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data ?: return
            Picasso.get().load(selectedImageUri).into(imageViewProfile)
        }
    }
}
