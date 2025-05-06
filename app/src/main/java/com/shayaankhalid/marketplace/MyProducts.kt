package com.shayaankhalid.marketplace

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_products)

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
            fetchMyProducts()

            swipeRefreshLayout.postDelayed(
                {
                    swipeRefreshLayout.isRefreshing = false
                }, 2000)
        }

    }

    private fun deleteProductFromServer(productId: Int, position: Int) {
        val url = "http://10.0.2.2/marketplace/delete_product.php"
        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                val json = JSONObject(response)
                if (json.getBoolean("success")) {
                    Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show()
                    adapter.removeItem(position)
                } else {
                    val message = json.optString("message", "Failed to delete")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    private fun fetchMyProducts() {
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

                        for (i in 0 until productsArray.length()) {
                            val productObj = productsArray.getJSONObject(i)
                            val id = productObj.getInt("id")  // assume your PHP returns this
                            val name = productObj.getString("name")
                            val price = productObj.getString("price")
                            val imageBase64 = productObj.getString("image")

                            productList.add(
                                ModelMyProduct(
                                    id = id,
                                    title = name,
                                    price = "$price PKR",
                                    imageBase64 = imageBase64
                                )
                            )
                        }

                        adapter.notifyDataSetChanged()
                    } else {
                        val message = json.optString("message", "Failed to load products")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("MyProducts", "Parsing error", e)
                    Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("MyProducts", "Volley error: ${error.message}", error)
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return mapOf("user_id" to userId.toString())
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }
}
