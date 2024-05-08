import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

import com.example.myapplication.EditPostActivity
import com.example.myapplication.MainActivity

import com.example.myapplication.ParticipantsDialog
import com.example.myapplication.Post
import com.example.myapplication.R

import com.example.myapplication.User
import com.example.myapplication.WeatherData

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PostAdapter(
    private val postList: List<Post>,
    private val currentUserId: String,
    private val database: FirebaseDatabase,
    private val context: Context

) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewPost)
        val contentTextView: TextView = itemView.findViewById(R.id.textViewContent)
        val locationTextView: TextView = itemView.findViewById(R.id.textViewLocation)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val joinPostsButton: Button = itemView.findViewById(R.id.buttonJoin)
        val ParticepatePostsButton: Button = itemView.findViewById(R.id.buttonParticipants)
        val ContactButton: Button = itemView.findViewById(R.id.buttonContact)
        val textViewWeather: TextView = itemView.findViewById(R.id.textViewWeather)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_card_layout, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentItem = postList[position]
        val postKey = currentItem.id





        holder.contentTextView.text = currentItem.content
        holder.timeTextView.text = currentItem.time
        holder.locationTextView.text = currentItem.location
        holder.ParticepatePostsButton.text = "Participants: ${currentItem.numberOfParticipants}"

        Picasso.get()
            .load(currentItem.imageUrl)
            .into(holder.imageView, object : Callback {
                override fun onSuccess() {}

                override fun onError(e: Exception?) {
                    Log.e("Picasso", "Error loading image: $e")
                    holder.imageView.setImageResource(R.drawable.error_image)
                }
            })

        holder.joinPostsButton.setOnClickListener {
            Log.d("JoinButton", "Join button clicked")
            val userName= FirebaseAuth.getInstance().currentUser?.displayName.toString()
            Log .d("JoinButton", "userName: $userName")
            val postRef = database.reference.child("users").child(currentItem.postCreator).child("posts").child(postKey)
            Log.d("JoinButton", "postRef: $postRef")
            postRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d("JoinButton", "DataSnapshot: ${dataSnapshot.exists()}")
                    if (dataSnapshot.exists()) {
                        val post = dataSnapshot.getValue(Post::class.java)
                        Log.d("JoinButton", "Post: $post")
                        val participants = post?.participants
                        Log.d("JoinButton", "Participants: $participants")
                        val numberOfParticipants = post?.numberOfParticipants ?: 0
                        Log.d("JoinButton", "Number of participants: $numberOfParticipants")

                        if (participants != null && userName != null) {
                            if (participants.contains(userName)) {
                                // User is already in the participants list, so leave
                                participants.remove(userName)
                                postRef.child("participants").setValue(participants)
                                holder.joinPostsButton.text = "Join"
                                Toast.makeText(
                                    holder.itemView.context,
                                    "You have left this post",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // User is joining for the first time
                                participants.add(userName)
                                postRef.child("participants").setValue(participants)
                                holder.joinPostsButton.text = "Leave"
                                Toast.makeText(
                                    holder.itemView.context,
                                    "You have joined this post",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            // Update the number of participants
                            postRef.child("numberOfParticipants").setValue(
                                if (participants.isEmpty()) 0 else participants.size
                            )
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("JoinButton", "Failed to read post data: ${databaseError.message}")
                }
            })
        }


        holder.ContactButton.setOnClickListener {
            val postCreatorId = currentItem.postCreator
            val userRef = database.reference.child("users").child(postCreatorId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)
                        val phoneNumber = user?.phoneNumber

                        if (!phoneNumber.isNullOrEmpty()) {
                            openWhatsApp(holder.itemView.context, phoneNumber)
                        } else {
                            Log.d("ContactButton", "Phone number is not available for this user")
                        }
                    } else {
                        Log.d("ContactButton", "User data does not exist")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ContactButton", "Failed to read user data: ${databaseError.message}")
                }
            })
        }

        holder.itemView.setOnClickListener {
            if(currentItem.postCreator == currentUserId) {
                val intent = Intent(holder.itemView.context, EditPostActivity::class.java)
                intent.putExtra("postId", postKey)
                Log.d("PostAdapter", "postKey: $postKey")
                intent.putExtra("post", currentItem)
                holder.itemView.context.startActivity(intent)
            }


        }

        holder.ParticepatePostsButton.setOnClickListener {
            val participants= currentItem.participants
            val dialog = ParticipantsDialog.newInstance(participants)
            dialog.show((holder.itemView.context as MainActivity).supportFragmentManager, "ParticipantsDialog")

        }


        fetchWeatherData(currentItem.location) { weatherData ->
            // Update the UI with weather data
            holder.textViewWeather.text = " ${weatherData.temperature} °C,  ${weatherData.description}"
        }

    }


    private fun fetchWeatherData(location: String, onWeatherDataFetched: (WeatherData) -> Unit) {
        val apiKey = "e4ba4d84ffb3f4d6d9d467cae6818f0e"
        val parts = location.split(",")
        val city = if (parts.size == 3) {
            parts[1].trim()
        } else {
            parts[0].trim()
        }
        Log.d("Weather", "City: $city")
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherMapService::class.java)
        val call = service.getWeather(city, apiKey)

        call.enqueue(object : retrofit2.Callback<WeatherResponse> {
            override fun onResponse(call: retrofit2.Call<WeatherResponse>, response: retrofit2.Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    if (weatherResponse != null) {
                        val temperatureKelvin = weatherResponse.main.temperature
                        val temperatureCelsius = temperatureKelvin - 273.15 // Convert Kelvin to Celsius
                        val formattedTemperature = String.format("%.1f", temperatureCelsius) // Format temperature to one decimal point
                        val description = weatherResponse.weather.firstOrNull()?.description ?: "N/A"
                        // Create a WeatherData object with formatted temperature and description
                        val weatherData = WeatherData(formattedTemperature.toDouble(), description)
                        // Invoke the callback with the weather data
                        onWeatherDataFetched(weatherData)
                    }
                } else {
                    Log.e("Weather", "Failed to fetch weather data: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<WeatherResponse>, t: Throwable) {
                Log.e("Weather", "Failed to fetch weather data: ${t.message}")
            }
        })
    }


    override fun getItemCount() = postList.size

    private fun openWhatsApp(context: Context, phoneNumber: String) {
        val pm = context.packageManager
        val appInstalled = try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (appInstalled) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }


}
