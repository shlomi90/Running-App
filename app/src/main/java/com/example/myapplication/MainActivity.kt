package com.example.myapplication


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomnavigation.BottomNavigationView



class MainActivity : AppCompatActivity() {

    private lateinit var buttonLogout: Button
    private lateinit var buttonApi: Button
    private lateinit var textViewUserDetails: TextView
    private lateinit var addPost: Button
    private lateinit var auth: FirebaseAuth
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        buttonLogout = findViewById(R.id.logout)
        buttonApi = findViewById(R.id.api)
        textViewUserDetails = findViewById(R.id.user_details)
        addPost=findViewById(R.id.add_button)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.posts -> {
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.home -> {
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }



        addPost.setOnClickListener {
            val intent = Intent(this@MainActivity, NewPost::class.java)
            startActivity(intent)
            finish()
        }



        // Set onClickListener for logout button
        buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this@MainActivity, Login::class.java)
            startActivity(intent)
            finish()
        }

        // Set onClickListener for API button (Placeholder)
        buttonApi.setOnClickListener {
            // Handle API button click
        }

        // Set up AuthStateListener
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                val intent = Intent(this@MainActivity, Login::class.java)
                startActivity(intent)
                finish()
            } else {
                textViewUserDetails.text = user.email
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for authentication state changes
        auth.addAuthStateListener(authListener!!)
    }

    override fun onStop() {
        super.onStop()
        // Stop listening for authentication state changes
        auth.removeAuthStateListener(authListener!!)
    }
}
