package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextName: EditText
    private lateinit var editTextAge: EditText
    private lateinit var buttonSave: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var currentUserUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase components
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance().reference
        currentUserUid = auth.currentUser!!.uid

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextName = findViewById(R.id.editTextName)
        editTextAge = findViewById(R.id.editTextAge)
        buttonSave = findViewById(R.id.buttonSave)

        // Fetch and display user data
        fetchUserData()

        // Set onClickListener for Save Changes button
        buttonSave.setOnClickListener {
            saveChanges()
            val intent = Intent(this@ProfileActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
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
        val newEmail=editTextEmail.text.toString().trim()

        // Update user data in Firebase Realtime Database
        val userUpdates = mapOf<String, Any>(
            "name" to newName,
            "age" to newAge,
            "email" to newEmail,

        )

        database.child("users").child(currentUserUid).updateChildren(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Changes saved successfully
                    // You can show a toast or perform any other action to notify the user
                    Toast.makeText(
                        baseContext, "Success to update user profile.", Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Failed to save changes
                    // Handle error
                    Toast.makeText(
                        baseContext, "Failed to update user profile.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
