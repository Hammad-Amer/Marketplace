package com.shayaankhalid.marketplace

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback

class Buy : AppCompatActivity() {

    private var isCouponApplied = false
    private val TAG = "BuyActivity"
    private var productowner: Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy)

        val productName = intent.getStringExtra("title") ?: "N/A"
        val productDesc = intent.getStringExtra("description") ?: "N/A"
        val productPrice = intent.getStringExtra("price") ?: "N/A"
        val productImageBase64 = intent.getStringExtra("imageBase64")
        val p_id = intent.getIntExtra("p_id", -1)


        fetchUserIdByProductId(p_id) { ownerId ->
            if (ownerId != null) {
                productowner = ownerId
                Log.d(TAG, "Product $p_id is owned by user $productowner")
            } else {
                Log.e(TAG, "Failed to fetch owner for product $p_id")
            }
        }

        val originalAmount = productPrice.filter { it.isDigit() }.toIntOrNull() ?: 0

        val orderAmountText = findViewById<TextView>(R.id.orderAmount)
        val orderTotalText = findViewById<TextView>(R.id.orderTotal)
        val selectCouponButton = findViewById<Button>(R.id.btnSelectCoupon)
        val couponText = findViewById<TextView>(R.id.changetext)
        val productTitleView = findViewById<TextView>(R.id.productTitle)
        val productImageView = findViewById<ImageView>(R.id.productImage)

        productTitleView.text = productName

        if (!productImageBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(productImageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                productImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                productImageView.setImageResource(R.drawable.dell_xps)
            }
        } else {
            productImageView.setImageResource(R.drawable.dell_xps)
        }

        orderAmountText.text = String.format("%,d.00 PKR", originalAmount)
        orderTotalText.text = String.format("%,d.00 PKR", originalAmount)

        selectCouponButton.setOnClickListener {
            if (!isCouponApplied) {
                var discountedAmount = originalAmount
                var discountApplied = ""

                if (originalAmount > 10_000) {
                    val discount = (originalAmount * 0.02).toInt()
                    discountedAmount -= discount
                    discountApplied = "2% Discount Applied"
                    Log.d(TAG, "Applying 2% discount: -$discount PKR")
                } else {
                    discountedAmount -= 500
                    discountApplied = "500 PKR Discount Applied"
                    Log.d(TAG, "Applying 500 PKR fixed discount")
                }

                if (discountedAmount < 0) discountedAmount = 0

                orderTotalText.text = String.format("%,d.00 PKR", discountedAmount)
                couponText.text = discountApplied
                isCouponApplied = true
                selectCouponButton.text = "Applied"

            }
        }

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val confirmButton = findViewById<Button>(R.id.btnProceedPayment)
        confirmButton.setOnClickListener {
            val url = "http://10.0.2.2/marketplace/delete_product.php"
            val queue: RequestQueue = Volley.newRequestQueue(this)

            val stringRequest = object : StringRequest(
                Request.Method.POST, url,
                Response.Listener { response ->
                    try {
                        val jsonResponse = JSONObject(response)
                        val success = jsonResponse.getBoolean("success")
                        val message = jsonResponse.getString("message")
                        if (success) {
                            Toast.makeText(this, "Order placed", Toast.LENGTH_SHORT).show()
                            val soldMessage = "Your product “$productName” has been sold!"
                            sendNotificationToReceiver(soldMessage)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed: $message", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Unexpected server response", Toast.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["product_id"] = p_id.toString()
                    return params
                }
            }

            queue.add(stringRequest)
        }
    }


    fun fetchUserIdByProductId(
        productId: Int,
        callback: (userId: Int?) -> Unit
    ) {
        val url = "http://10.0.2.2/marketplace/get_user_by_product.php?product_id=$productId"
        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET,
            url,
            { response ->
                try {
                    val obj = JSONObject(response)
                    if (obj.getString("status") == "success") {
                        callback(obj.getInt("user_id"))
                    } else {
                        callback(null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            },
            { error ->
                error.printStackTrace()
                callback(null)
            }
        )

        queue.add(request)
    }

    private fun sendNotificationToReceiver(messageText: String) {
        Log.d("FCM-DEBUG", "▶︎ sendNotificationToReceiver() START")
        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val senderId = sharedPref.getInt("user_id", 0)
        val senderName = "Marketplace"
        val queue = Volley.newRequestQueue(this)

        val url = "http://10.0.2.2/marketplace/get_fcm_token.php?user_id=$productowner"
        Log.d("FCM-DEBUG", "Fetching receiver token from: $url")
        val request = StringRequest(Request.Method.GET, url, { response ->
            Log.d("FCM-DEBUG", "Got response: $response")
            try {
                val jsonObject = JSONObject(response)

                // use the correct key names:
                if (jsonObject.optBoolean("success", false)) {
                    val receiverToken = jsonObject.optString("fcm_token")

                    Log.d("FCM-DEBUG", "Parsed receiverToken = $receiverToken")

                    val notification = Notification(
                        message = NotificationData(
                            token = receiverToken,
                            data = hashMapOf(
                                "title" to senderName,
                                "body"  to messageText
                            )
                        )
                    )

                    NotificationApi.create()
                        .sendNotification(notification)
                        .enqueue(object : Callback<Notification> {
                            override fun onResponse(
                                call: Call<Notification>,
                                response: retrofit2.Response<Notification>
                            ) {
                                Log.d("FCM-DEBUG", "✅ Notification sent!  HTTP ${response.code()}")
                            }

                            override fun onFailure(call: Call<Notification>, t: Throwable) {
                                Log.e("FCM-DEBUG", "🚨 Retrofit failure: ${t.message}")
                            }
                        })
                } else {
                    Log.e("FCM-DEBUG", "Response success flag was false")
                }
            } catch (e: Exception) {
                Log.e("FCM-DEBUG", "JSON parse error", e)
            }
        }, { error ->
            Log.e("FCM-DEBUG", "Volley error fetching token", error)
        })


        queue.add(request)
    }
}
