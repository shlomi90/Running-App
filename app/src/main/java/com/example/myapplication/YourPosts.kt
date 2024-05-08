package com.example.myapplication

import PostAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class YourPosts : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>
    private lateinit var databaseReference: DatabaseReference

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_your_posts)

        currentUser = auth.currentUser
        recyclerView = findViewById(R.id.recyclerViewYourPosts)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        postList = mutableListOf()
        postAdapter = PostAdapter(postList, currentUser?.uid ?: "", FirebaseDatabase.getInstance(), this)
        recyclerView.adapter = postAdapter


        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    val intent = Intent(this@YourPosts, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.posts -> {
                    // Replace MainActivity with your desired activity for the "posts" button
                    val intent = Intent(this@YourPosts, YourPosts::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.homep -> {
                    // Always navigate to MainActivity when "home" button is clicked
                    val intent = Intent(this@YourPosts, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }


        val currentUserUid = currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(
            currentUserUid.toString()
        ).child("posts")

        databaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(it)
                    }
                }
                postAdapter.notifyDataSetChanged()
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@YourPosts, "Failed to read value.", Toast.LENGTH_SHORT).show()
            }
        })


    }
}
