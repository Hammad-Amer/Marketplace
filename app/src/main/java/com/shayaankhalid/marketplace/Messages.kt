package com.shayaankhalid.marketplace

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log // <-- add this
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject

class Messages : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val userList = mutableListOf<MessagesModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        recyclerView = findViewById(R.id.recyclerViewMessages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        Log.d("Messages", "user_id from SharedPreferences: $userId")

        if (userId == -1) {
            Log.e("Messages", "Invalid user_id, stopping fetch")
            return
        }

        fetchChatUsers(userId)
        setupBottomNav()
        setupProfileImage()
    }

    private fun setupProfileImage() {
        val profileImage = findViewById<ImageView>(R.id.profile_icon)
        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val pfpBase64 = sharedPref.getString("pfp", null)

        val bitmap = if (!pfpBase64.isNullOrEmpty()) {
            try {
                decodeBase64ToBitmap(pfpBase64)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        profileImage.setImageBitmap(bitmap ?: getDrawable(R.drawable.empty_user)?.toBitmap())
        profileImage.setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }
    }

    private fun fetchChatUsers(userId: Int) {
        val url = "http://10.0.2.2/marketplace/get_conversations.php?user_id=$userId"
        Log.d("Messages", "Fetching URL: $url")

        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                Log.d("Messages", "Raw response: $response")

                try {
                    val json = JSONObject(response)
                    val conversations = json.getJSONArray("conversations")

                    for (i in 0 until conversations.length()) {
                        val user = conversations.getJSONObject(i)

                        val id = user.getInt("id")
                        val name = user.getString("name")
                        val pfpBase64 = user.getString("pfp")

                        Log.d("Messages", "User $i -> id: $id, name: $name")
                        Log.d("Messages", "Base64 PFP snippet: ${pfpBase64.take(30)}...")

                        val messagesModel = MessagesModel(id, name, pfpBase64)
                        userList.add(messagesModel)
                    }

                    messageAdapter = MessageAdapter(userList)
                    recyclerView.adapter = messageAdapter
                } catch (e: Exception) {
                    Log.e("Messages", "Failed to parse JSON", e)
                }
            },
            { error ->
                Log.e("Messages", "Volley error: ${error.message}")
                error.printStackTrace()
            })

        queue.add(request)
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun setupBottomNav() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_message
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Homescreen::class.java))
                    finishAffinity()
                    true
                }
                R.id.nav_search -> {
                    startActivity(Intent(this, Search::class.java))
                    finish()
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddProducts::class.java))
                    true
                }
                R.id.nav_my_products -> {
                    startActivity(Intent(this, MyProducts::class.java))
                    finish()
                    true
                }
                R.id.nav_message -> true
                else -> false
            }
        }
    }
}
