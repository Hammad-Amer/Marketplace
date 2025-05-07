package com.shayaankhalid.marketplace

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject

class MyProducts : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyProductsAdapter
    private lateinit var productList: MutableList<ModelMyProduct>
    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_products)
        dbHelper = DBHelper(this)

        recyclerView = findViewById(R.id.recycler_view_products)
        recyclerView.layoutManager = LinearLayoutManager(this)

        productList = mutableListOf()
        adapter = MyProductsAdapter(productList) { productId, position ->
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete this product?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteProductFromServer(productId, position)
                }
                .setNegativeButton("No", null)
                .show()
        }
        recyclerView.adapter = adapter

        fetchMyProducts()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_my_products
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
                R.id.nav_my_products -> true
                R.id.nav_message -> {
                    startActivity(Intent(this, Messages::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }

        val logoutButton = findViewById<Button>(R.id.logout)
        logoutButton.setOnClickListener {
            val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
        }

        val swipeRefreshLayout = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)

        swipeRefreshLayout.setOnRefreshListener {
            if (isNetworkAvailable()) {
                dbHelper.clearMyProducts()
                loadFromServer()
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            swipeRefreshLayout.isRefreshing = false
        }

    }


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun fetchMyProducts() {
        if (isNetworkAvailable()) {
            syncPendingProducts(this)
            loadFromServer()
        } else {
            loadFromCache()
            Toast.makeText(this, "Offline mode: Showing cached products", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteProductFromServer(productId: Int, position: Int) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Cannot delete while offline", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://10.0.2.2/marketplace/delete_product.php"
        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        // Remove from both server and local cache
                        dbHelper.deleteProduct(productId)
                        adapter.removeItem(position)
                        Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show()

                    } else {
                        val message = json.optString("message", "Failed to delete")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("product_id" to productId.toString())
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

//    private fun fetchMyProducts() {
//        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
//        val userId = sharedPref.getInt("user_id", -1)
//
//        if (userId == -1) {
//            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val url = "http://10.0.2.2/marketplace/get_my_products.php"
//        val stringRequest = object : StringRequest(Method.POST, url,
//            { response ->
//                Log.d("MyProducts", "Response: $response")
//                try {
//                    val json = JSONObject(response)
//                    if (json.getBoolean("success")) {
//                        val productsArray = json.getJSONArray("products")
//                        productList.clear()
//
//                        for (i in 0 until productsArray.length()) {
//                            val productObj = productsArray.getJSONObject(i)
//                            val id = productObj.getInt("id")  // assume your PHP returns this
//                            val name = productObj.getString("name")
//                            val price = productObj.getString("price")
//                            val imageBase64 = productObj.getString("image")
//
//                            productList.add(
//                                ModelMyProduct(
//                                    id = id,
//                                    title = name,
//                                    price = "$price PKR",
//                                    imageBase64 = imageBase64
//                                )
//                            )
//                        }
//
//                        adapter.notifyDataSetChanged()
//                    } else {
//                        val message = json.optString("message", "Failed to load products")
//                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//                    }
//                } catch (e: Exception) {
//                    Log.e("MyProducts", "Parsing error", e)
//                    Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show()
//                }
//            },
//            { error ->
//                Log.e("MyProducts", "Volley error: ${error.message}", error)
//                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
//            }
//        ) {
//            override fun getParams(): Map<String, String> {
//                return mapOf("user_id" to userId.toString())
//            }
//        }
//
//        Volley.newRequestQueue(this).add(stringRequest)
//    }

    private fun loadFromCache() {
        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val cachedProducts = dbHelper.getUserProducts(userId)
        productList.clear()

        cachedProducts.forEach { product ->
            productList.add(
                ModelMyProduct(
                    id = product.p_id,
                    title = product.title,
                    price = "${product.price} PKR",
                    imageBase64 = product.imageBase64
                )
            )
        }

        adapter.notifyDataSetChanged()
    }

    private fun loadFromServer() {
        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://10.0.2.2/marketplace/get_my_products.php"
        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                Log.d("MyProducts", "Response: $response")
                try {
                    val json = JSONObject(response)
                    if (json.getBoolean("success")) {
                        val productsArray = json.getJSONArray("products")
                        productList.clear()
                        val productsToCache = mutableListOf<Product>()

                        for (i in 0 until productsArray.length()) {
                            val productObj = productsArray.getJSONObject(i)
                            val id = productObj.getInt("id")
                            val name = productObj.getString("name")
                            val price = productObj.getString("price")
                            val imageBase64 = productObj.getString("image")
                            val description = productObj.optString("description", "")
                            val category = productObj.optString("category", "")

                            productList.add(
                                ModelMyProduct(
                                    id = id,
                                    title = name,
                                    price = "$price PKR",
                                    imageBase64 = imageBase64
                                )
                            )

                            // Prepare product for caching
                            productsToCache.add(
                                Product(
                                    p_id = id,
                                    u_id = userId,
                                    title = name,
                                    description = description,
                                    price = price,
                                    imageBase64 = imageBase64,
                                    category = category
                                )
                            )
                        }

                        // Update cache
                        //dbHelper.clearMyProducts()
                        dbHelper.addOrUpdateMyProducts(productsToCache)

                        adapter.notifyDataSetChanged()
                    } else {
                        loadFromCache() // Fallback to cache if server fails
                        val message = json.optString("message", "Failed to load products")
                        Toast.makeText(this, "$message (showing cached)", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("MyProducts", "Parsing error", e)
                    loadFromCache() // Fallback to cache on parsing error
                    Toast.makeText(this, "Error parsing data (showing cached)", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("MyProducts", "Volley error: ${error.message}", error)
                loadFromCache() // Fallback to cache on network error
                Toast.makeText(this, "Network error (showing cached)", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("user_id" to userId.toString())
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }


    private fun syncPendingProducts(context: Context) {
        val dbHelper = DBHelper(context)
        val pendingProducts = dbHelper.getPendingProducts()

        for (product in pendingProducts) {
            val request = object : StringRequest(Method.POST, "http://10.0.2.2/marketplace/add_product.php",
                { response ->
                    try {
                        val json = JSONObject(response)
                        if (json.getBoolean("success")) {
                            dbHelper.deletePendingProduct(product.p_id)
                            Log.d("Sync", "Product synced and removed locally: ${product.title}")
                        } else {
                            Log.e("Sync", "Failed to sync product: ${json.getString("message")}")
                        }
                    } catch (e: Exception) {
                        Log.e("Sync", "Parse error: ${e.message}")
                    }
                },
                { error ->
                    Log.e("Sync", "Volley error: ${error.message}")
                }) {

                override fun getParams(): Map<String, String> {
                    return mapOf(
                        "user_id" to product.u_id.toString(),
                        "name" to product.title,
                        "description" to product.description,
                        "price" to product.price,
                        "category" to product.category,
                        "image" to product.imageBase64
                    )
                }
            }

            Volley.newRequestQueue(context).add(request)
        }
    }
}
