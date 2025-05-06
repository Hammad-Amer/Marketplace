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

class Buy : AppCompatActivity() {

    private var isCouponApplied = false
    private val TAG = "BuyActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy)

        val productName = intent.getStringExtra("title") ?: "N/A"
        val productDesc = intent.getStringExtra("description") ?: "N/A"
        val productPrice = intent.getStringExtra("price") ?: "N/A"
        val productImageBase64 = intent.getStringExtra("imageBase64")
        val p_id = intent.getIntExtra("p_id", -1)

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
}
