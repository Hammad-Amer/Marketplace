package com.shayaankhalid.marketplace

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
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
import androidx.appcompat.app.AlertDialog

class Homescreen : AppCompatActivity() {

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private var productList: MutableList<Product> = mutableListOf()
    private var currentFilteredProducts: List<Product> = productList

    private var sortDescending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        setupCategoryList()
        setupProfileImage()
        setupSearchBar()
        setupBottomNavigation()

        productRecyclerView = findViewById(R.id.productRecyclerView)
        productRecyclerView.layoutManager = GridLayoutManager(this, 2)

        val sortButton = findViewById<Button>(R.id.sortBtn)
        sortButton.setOnClickListener {
            sortProducts()
        }

        val filterButton = findViewById<Button>(R.id.filterBtn)
        filterButton.setOnClickListener {
            val options = arrayOf("Show All", "Less than 10,000", "10,000 to 100,000", "Greater than 100,000")

            AlertDialog.Builder(this)
                .setTitle("Filter by Price")
                .setItems(options) { _, which ->
                    val filteredList = when (which) {
                        0 -> currentFilteredProducts
                        1 -> currentFilteredProducts.filter { it.price.toDouble() < 10000 }
                        2 -> currentFilteredProducts.filter { it.price.toDouble() in 10000.0..100000.0 }
                        3 -> currentFilteredProducts.filter { it.price.toDouble() > 100000 }
                        else -> currentFilteredProducts
                    }

                    productAdapter = ProductAdapter(filteredList) { product ->
                        val intent = Intent(this, ViewProduct::class.java).apply {
                            putExtra("title", product.title)
                            putExtra("description", product.description)
                            putExtra("price", product.price)
                            putExtra("imageBase64", product.imageBase64)
                            putExtra("p_id", product.p_id)
                            putExtra("category", product.category)
                        }
                        startActivity(intent)
                    }

                    productRecyclerView.adapter = productAdapter
                }
                .show()
        }

        loadOtherProducts()

        val swipeRefreshLayout = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)

        swipeRefreshLayout.setOnRefreshListener {
            loadOtherProducts()

            swipeRefreshLayout.postDelayed(
                {
                swipeRefreshLayout.isRefreshing = false
            }, 2000)
        }

    }

    private fun setupCategoryList() {
        val categories = listOf(
            Category(R.drawable.apple_logo, "All"),
            Category(R.drawable.dell_xps, "Laptops"),
            Category(R.drawable.s24_ultra, "Mobiles"),
            Category(R.drawable.s24_ultra, "Tablets"),
            Category(R.drawable.apple_watch, "Watches"),
            Category(R.drawable.sony_camera, "Cameras"),
            Category(R.drawable.s24_ultra, "Headphones"),
            Category(R.drawable.s24_ultra, "Accessories"),
            Category(R.drawable.s24_ultra, "Gaming Consoles"),
            Category(R.drawable.s24_ultra, "Printers"),
            Category(R.drawable.s24_ultra, "Peripherals"),
            Category(R.drawable.s24_ultra, "Others")
        )

        val categoryRecyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        categoryAdapter = CategoryAdapter(categories) { category ->
            val filteredProducts = if (category.name == "All") {
                productList // No filtering, show all
            } else {
                productList.filter { it.category.equals(category.name, ignoreCase = true) }
            }

            // Update the product adapter with the filtered list
            productAdapter = ProductAdapter(filteredProducts) { product ->
                val intent = Intent(this, ViewProduct::class.java).apply {
                    putExtra("title", product.title)
                    putExtra("description", product.description)
                    putExtra("price", product.price)
                    putExtra("imageBase64", product.imageBase64)
                    putExtra("p_id", product.p_id)
                    putExtra("category", product.category)
                }
                startActivity(intent)
            }

            productRecyclerView.adapter = productAdapter
            Toast.makeText(this, "Filtered by: ${category.name}", Toast.LENGTH_SHORT).show()

            currentFilteredProducts = filteredProducts
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
                if (json.getBoolean("success")) {
                    val posts = json.getJSONArray("posts")
                    productList.clear()

                    for (i in 0 until posts.length()) {
                        val post = posts.getJSONObject(i)
                        val product = Product(
                            title = post.getString("title"),
                            description = post.getString("description"),
                            price = post.getString("price"),
                            imageBase64 = post.getString("image"),
                            p_id = post.getInt("id"),
                            category = post.getString("category")
                        )
                        productList.add(product)
                    }

                    productAdapter = ProductAdapter(productList) { product ->
                        val intent = Intent(this, ViewProduct::class.java).apply {
                            putExtra("title", product.title)
                            putExtra("description", product.description)
                            putExtra("price", product.price)
                            putExtra("imageBase64", product.imageBase64)
                            putExtra("p_id", product.p_id)
                            putExtra("category", product.category)
                        }
                        startActivity(intent)
                    }

                    productRecyclerView.adapter = productAdapter
                    Log.d(TAG, "Product adapter set with ${productList.size} products")

                } else {
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
    }

    private fun sortProducts() {
        sortDescending = !sortDescending

        currentFilteredProducts = currentFilteredProducts.sortedBy {
            it.price.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
        }

        if (sortDescending) {
            currentFilteredProducts = currentFilteredProducts.reversed()
        }

        productAdapter = ProductAdapter(currentFilteredProducts) { product ->
            val intent = Intent(this, ViewProduct::class.java).apply {
                putExtra("title", product.title)
                putExtra("description", product.description)
                putExtra("price", product.price)
                putExtra("imageBase64", product.imageBase64)
                putExtra("p_id", product.p_id)
                putExtra("category", product.category)
            }
            startActivity(intent)
        }

        productRecyclerView.adapter = productAdapter
        Toast.makeText(
            this,
            if (sortDescending) "Sorted: High to Low" else "Sorted: Low to High",
            Toast.LENGTH_SHORT
        ).show()
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
