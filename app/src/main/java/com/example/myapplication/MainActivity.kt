package com.example.myapplication

import PostAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var buttonLogout: Button
    private lateinit var buttonApi: Button
    private lateinit var addPost: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>
    private lateinit var databaseReference: DatabaseReference

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        buttonLogout = findViewById(R.id.logout)
        buttonApi = findViewById(R.id.api)
        addPost = findViewById(R.id.add_button)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        postList = mutableListOf()
        postAdapter = PostAdapter(postList, currentUser?.uid ?: "", FirebaseDatabase.getInstance(), this)
        recyclerView.adapter = postAdapter

        val currentUserUid = currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")


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
                    // Replace MainActivity with your desired activity for the "posts" button
                    val intent = Intent(this@MainActivity, YourPosts::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.homep -> {
                    // Always navigate to MainActivity when "home" button is clicked
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

        buttonLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this@MainActivity, Login::class.java)
            startActivity(intent)
            finish()
        }

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                val intent = Intent(this@MainActivity, Login::class.java)
                startActivity(intent)
                finish()
            } else {
                Log.d("MainActivity", "onAuthStateChanged:signed_in:" + user.uid)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authListener!!)

        databaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (userSnapshot in snapshot.children) {
                    val userPostsReference = userSnapshot.child("posts")
                    for (postSnapshot in userPostsReference.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.let {
                            postList.add(it)
                        }
                    }
                }
                postAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to read value.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authListener!!)
    }
}
