package com.shayaankhalid.marketplace

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import android.widget.EditText
class Chat : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessageModel>()
    private var firstLoad = true

    private val handler = Handler()
    private lateinit var updateRunnable: Runnable
    private val updateInterval: Long = 3000

    private var currentUserId: Int = -1
    private var receiverId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerView = findViewById(R.id.recyclerViewMessages)
        recyclerView.layoutManager = LinearLayoutManager(this)

        receiverId = intent.getIntExtra("reciever_id", -1)
        val receiverName = intent.getStringExtra("reciever_name") ?: "Unknown"
        val receiverPfp = intent.getStringExtra("reciever_pfp")
        val editTextMessage = findViewById<EditText>(R.id.editTextMessage)
        val buttonSend = findViewById<ImageView>(R.id.buttonSend)

        buttonSend.setOnClickListener {
            val messageContent = editTextMessage.text.toString()
            if (messageContent.isNotEmpty()) {
                sendMessage(currentUserId, receiverId, messageContent)
                editTextMessage.text.clear()
            }
        }

        val nameView = findViewById<TextView>(R.id.profileName)
        val imageView = findViewById<ImageView>(R.id.profileImage)
        nameView.text = receiverName

        try {
            val decodedBytes = Base64.decode(receiverPfp, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        findViewById<ImageView>(R.id.buttonBack).setOnClickListener {
            finish()
        }

        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        currentUserId = sharedPref.getInt("user_id", -1)

        adapter = ChatAdapter(messageList, currentUserId)
        recyclerView.adapter = adapter

        updateRunnable = object : Runnable {
            override fun run() {
                fetchMessages()
                handler.postDelayed(this, updateInterval)
            }
        }
        handler.post(updateRunnable)

        val rootView = findViewById<android.view.View>(R.id.main)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                recyclerView.post {
                    recyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    private fun fetchMessages() {
        val url = "http://10.0.2.2/marketplace/get_messages.php?user1=$currentUserId&user2=$receiverId"
        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(Request.Method.GET, url, { response ->
            try {
                val jsonObject = JSONObject(response)
                if (jsonObject.getString("status") == "success") {
                    val messagesArray: JSONArray = jsonObject.getJSONArray("messages")

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                    val isAtBottom = lastVisiblePosition >= messageList.size - 2 || lastVisiblePosition == RecyclerView.NO_POSITION

                    messageList.clear()
                    for (i in 0 until messagesArray.length()) {
                        val msgObj = messagesArray.getJSONObject(i)
                        val senderId = msgObj.getInt("sender_id")
                        val receiverIdMsg = msgObj.getInt("receiver_id")
                        val content = msgObj.getString("message")
                        val timestamp = msgObj.getLong("timestamp")

                        messageList.add(ChatMessageModel(senderId, receiverIdMsg, content, timestamp))
                    }
                    adapter.notifyDataSetChanged()

                    if (firstLoad || isAtBottom) {
                        recyclerView.scrollToPosition(messageList.size - 1)
                        firstLoad = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { error ->
            error.printStackTrace()
        })

        queue.add(request)
    }

    private fun sendMessage(senderId: Int, receiverId: Int, message: String) {
        val url = "http://10.0.2.2/marketplace/send_message.php"

        val queue = Volley.newRequestQueue(this)

        val postRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getString("status") == "success") {
                        fetchMessages()
                        recyclerView.scrollToPosition(messageList.size - 1)

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                error.printStackTrace()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["sender_id"] = senderId.toString()
                params["receiver_id"] = receiverId.toString()
                params["message"] = message
                return params
            }
        }

        queue.add(postRequest)
    }
}
