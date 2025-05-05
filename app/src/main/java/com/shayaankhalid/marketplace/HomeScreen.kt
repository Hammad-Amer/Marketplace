package com.shayaankhalid.marketplace

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject

class Homescreen : AppCompatActivity() {

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        setupCategoryList()
        setupProfileImage()
        setupSearchBar()
        setupBottomNavigation()

        productRecyclerView = findViewById(R.id.productRecyclerView)
        productRecyclerView.layoutManager = GridLayoutManager(this, 2)

        loadOtherProducts()
    }

    private fun setupCategoryList() {
        val categories = listOf(
            Category(R.drawable.dell_xps, "Laptops"),
            Category(R.drawable.s24_ultra, "Mobiles"),
            Category(R.drawable.s24_ultra, "Tablets"),
            Category(R.drawable.apple_watch, "Watches"),
            Category(R.drawable.sony_camera, "Cameras")
        )

        val categoryRecyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        categoryAdapter = CategoryAdapter(categories) { category ->
            Toast.makeText(this, "Clicked: ${category.name}", Toast.LENGTH_SHORT).show()
        }

        categoryRecyclerView.adapter = categoryAdapter
    }

    private fun setupProfileImage() {
        val profileImage = findViewById<ImageView>(R.id.editprofile)
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

    private fun setupSearchBar() {
        val search = findViewById<EditText>(R.id.searchbar)
        search.setOnClickListener {
            startActivity(Intent(this, Search::class.java))
        }
    }

    private fun loadOtherProducts() {
        val TAG = "Homescreen"
        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        val url = "http://10.0.2.2/marketplace/get_other_products.php"

        val request = object : StringRequest(Method.POST, url, { response ->
            Log.d(TAG, "Server response: $response")

            try {
                val json = JSONObject(response)
                Log.d(TAG, "Parsed JSON object successfully")

                if (json.getBoolean("success")) {
                    val posts = json.getJSONArray("posts")
                    val productList = mutableListOf<Product>()

                    for (i in 0 until posts.length()) {
                        val post = posts.getJSONObject(i)
                        val product = Product(
                            title = post.getString("title"),
                            description = post.getString("description"),
                            price = post.getString("price"),
                            imageBase64 = post.getString("image")
                        )
                        Log.d(TAG, "Parsed product: $product")
                        productList.add(product)
                    }

                    val productAdapter = ProductAdapter(productList) { product ->
                        val intent = Intent(this, ViewProduct::class.java).apply {
                            putExtra("title", product.title)
                            putExtra("description", product.description)
                            putExtra("price", product.price)
                            putExtra("imageBase64", product.imageBase64)
                        }
                        startActivity(intent)
                    }

                    productRecyclerView.adapter = productAdapter
                    Log.d(TAG, "Product adapter set with ${productList.size} products")

                } else {
                    Log.e(TAG, "Server response indicated failure")
                    Toast.makeText(this, "Failed to load products", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error parsing response: ${e.message}", e)
                Toast.makeText(this, "Error parsing server response", Toast.LENGTH_SHORT).show()
            }

        }, { error ->
            Log.e(TAG, "Volley error: ${error.message}", error)
            Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                return mapOf("user_id" to userId.toString())
            }
        }

        Volley.newRequestQueue(this).add(request)
        Log.d(TAG, "Volley request sent")
    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_search -> {
                    startActivity(Intent(this, Search::class.java))
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddProducts::class.java))
                    true
                }
                R.id.nav_my_products -> {
                    startActivity(Intent(this, MyProducts::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
