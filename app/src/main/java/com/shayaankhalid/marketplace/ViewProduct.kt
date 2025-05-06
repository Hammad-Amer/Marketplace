package com.shayaankhalid.marketplace

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ViewProduct : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_product)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val title = findViewById<TextView>(R.id.productTitle)
        val image = findViewById<ImageView>(R.id.productImage)
        val desc = findViewById<TextView>(R.id.productDescription)
        val price = findViewById<TextView>(R.id.productPrice)
        val buyBtn = findViewById<Button>(R.id.buyButton)

        val productName = intent.getStringExtra("title") ?: "N/A"
        val productDesc = intent.getStringExtra("description") ?: "N/A"
        val productPrice = intent.getStringExtra("price") ?: "N/A"
        val productImageBase64 = intent.getStringExtra("imageBase64")
        val p_id = intent.getIntExtra("p_id", -1)

        title.text = productName
        desc.text = productDesc
        price.text = productPrice

        if (!productImageBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(productImageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                image.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                image.setImageResource(R.drawable.dell_xps) // fallback if decoding fails
            }
        } else {
            image.setImageResource(R.drawable.dell_xps) // fallback if no image provided
        }

        backButton.setOnClickListener {
            finish()
        }

        buyBtn.setOnClickListener {
            Toast.makeText(this, "Purchase started!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, Buy::class.java).apply {
                putExtra("title", productName)
                putExtra("description", productDesc)
                putExtra("price", productPrice)
                putExtra("imageBase64", productImageBase64)
                putExtra("p_id", p_id)
            }
            startActivity(intent)
        }
    }
}
