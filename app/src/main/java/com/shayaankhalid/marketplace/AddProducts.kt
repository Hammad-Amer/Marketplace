package com.shayaankhalid.marketplace

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class AddProducts : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputDescription: EditText
    private lateinit var inputPrice: EditText
    private lateinit var categorySpinner: Spinner // 🔁 Spinner instead of EditText
    private lateinit var buttonAdd: Button
    private lateinit var buttonChooseImage: Button
    private lateinit var imagePreview: ImageView

    private var base64Image: String? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var selectedCategory: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_products)

        inputName = findViewById(R.id.input_name)
        inputDescription = findViewById(R.id.input_description)
        inputPrice = findViewById(R.id.input_price)
        categorySpinner = findViewById(R.id.spinner_category) // 🔁 find Spinner
        buttonAdd = findViewById(R.id.button_add)
        buttonChooseImage = findViewById(R.id.button_choose_image)
        imagePreview = findViewById(R.id.image_preview)

        findViewById<ImageView>(R.id.back_arrow).setOnClickListener {
            finish()
        }

        setupSpinner() // 🔁 set up category dropdown

        buttonChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        buttonAdd.setOnClickListener {
            submitProduct()
        }
    }

    private fun setupSpinner() {
        val categories = listOf(
            "Select a category",
            "Laptops",
            "Mobiles",
            "Tablets",
            "Watches",
            "Cameras",
            "Headphones",
            "Accessories",
            "Gaming Consoles",
            "Printers",
            "Peripherals",
            "Others"
        )
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            categories
        ) {
            override fun isEnabled(position: Int): Boolean = position != 0

            override fun getDropDownView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(if (position == 0) Color.GRAY else Color.BLACK)
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun submitProduct() {
        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        val name = inputName.text.toString().trim()
        selectedCategory = categorySpinner.selectedItem.toString()
        val description = inputDescription.text.toString().trim()
        val priceText = inputPrice.text.toString().trim()
        val price = priceText.toDoubleOrNull()

        Log.d("AddProduct", "Collected inputs: userId=$userId, name=$name, category=$selectedCategory, description=$description, priceText=$priceText, price=$price, hasImage=${base64Image != null}")

        if (name.isEmpty() || selectedCategory == "Select a category" || description.isEmpty() || priceText.isEmpty() || price == null || base64Image == null) {
            Toast.makeText(this, "Please fill all fields correctly and choose an image", Toast.LENGTH_SHORT).show()
            Log.d("AddProduct", "Validation failed: some fields are empty or invalid")
            return
        }

        val url = "http://10.0.2.2/marketplace/add_product.php"
        Log.d("AddProduct", "Sending POST request to $url")

        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                Log.d("AddProduct", "Raw response: $response")
                try {
                    val json = JSONObject(response)
                    Log.d("AddProduct", "Parsed JSON: $json")

                    if (json.getBoolean("success")) {
                        Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val message = json.optString("message", "Unknown error")
                        Toast.makeText(this, "Failed: $message", Toast.LENGTH_SHORT).show()
                        Log.d("AddProduct", "Server reported failure: $message")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to parse server response", Toast.LENGTH_SHORT).show()
                    Log.e("AddProduct", "JSON parsing error", e)
                    Log.e("AddProduct", "Problematic response: $response")
                }
            },
            { error ->
                Log.e("AddProduct", "Volley error: ${error.message}", error)
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {

            override fun getParams(): Map<String, String> {
                val params = mutableMapOf<String, String>()
                params["user_id"] = userId.toString()
                params["name"] = name
                params["description"] = description
                params["price"] = price.toString()
                params["category"] = selectedCategory
                params["image"] = base64Image ?: ""
                Log.d("AddProduct", "POST params: $params")
                return params
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            imagePreview.setImageBitmap(bitmap)

            base64Image = encodeImage(bitmap)
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
}
