package com.shayaankhalid.marketplace

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Button
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap

class Search : AppCompatActivity() {

    private lateinit var productAdapter: SearchProductAdapter
    private lateinit var searchBox: EditText
    private lateinit var productRecyclerView: RecyclerView

    private var allProducts: MutableList<ModelSearchProduct> = mutableListOf()
    private var sortDescending = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        productRecyclerView = findViewById(R.id.productRecyclerView)
        searchBox = findViewById(R.id.searchbox)

        val sortButton = findViewById<Button>(R.id.sortBtn)
        sortButton.setOnClickListener {
            sortProducts()
        }

        val filterButton = findViewById<Button>(R.id.filterBtn)
        filterButton.setOnClickListener {
            showFilterOptions()
        }

        productAdapter = SearchProductAdapter(allProducts) { product ->
            val intent = Intent(this, ViewProduct::class.java).apply {
                putExtra("title", product.title)
                putExtra("description", product.description)
                putExtra("price", product.price)
                putExtra("imageBase64", product.imageBase64)
                putExtra("p_id", product.p_id)
                putExtra("u_id", product.u_id)
                putExtra("category", product.category)
            }
            startActivity(intent)
        }

        productRecyclerView.layoutManager = GridLayoutManager(this, 2)
        productRecyclerView.adapter = productAdapter



        setupBottomNav()
        loadProductsFromServer()
        setupSearchBox()
        setupProfileImage()
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

    private fun sortProducts() {
        sortDescending = !sortDescending

        allProducts.sortWith(compareBy { product ->
            product.price.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
        })

        if (sortDescending) {
            allProducts.reverse()
        }

        productAdapter.updateData(allProducts)

        Toast.makeText(
            this,
            if (sortDescending) "Sorted: High to Low" else "Sorted: Low to High",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showFilterOptions() {
        val options = arrayOf("Show All", "Less than 10,000", "10,000 to 100,000", "Greater than 100,000")

        AlertDialog.Builder(this)
            .setTitle("Filter by Price")
            .setItems(options) { _, which ->
                val filteredList = when (which) {
                    0 -> allProducts // Show all products
                    1 -> allProducts.filter { it.price.toDouble() < 10000 }
                    2 -> allProducts.filter { it.price.toDouble() in 10000.0..100000.0 }
                    3 -> allProducts.filter { it.price.toDouble() > 100000 }
                    else -> allProducts
                }

                applySearchAndFilter(filteredList)
            }
            .show()
    }


    private fun setupSearchBox() {
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()

                if (query.isEmpty()) {
                    applySearchAndFilter(emptyList())
                    return
                }

                val queryWords = query.split(" ")

                val filtered = allProducts.filter { product ->
                    val titleWords = product.title.lowercase().split(" ")
                    val categoryWords = product.category.lowercase().split(" ")
                    val allWords = titleWords + categoryWords

                    queryWords.all { queryWord ->
                        allWords.any { it.startsWith(queryWord) }
                    }
                }

                applySearchAndFilter(filtered)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun applySearchAndFilter(filteredList: List<ModelSearchProduct>) {
        val query = searchBox.text.toString().trim().lowercase()

        if (query.isNotEmpty()) {
            val queryWords = query.split(" ")

            val searchFiltered = filteredList.filter { product ->
                val titleWords = product.title.lowercase().split(" ")
                val categoryWords = product.category.lowercase().split(" ")
                val allWords = titleWords + categoryWords

                queryWords.all { queryWord ->
                    allWords.any { it.startsWith(queryWord) }
                }
            }

            productAdapter.updateData(searchFiltered)
        } else {
            productAdapter.updateData(filteredList)
        }
    }

    private fun loadProductsFromServer() {
        val sharedPref = getSharedPreferences("Marketplace", MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        val url = "http://10.0.2.2/marketplace/get_other_products.php"

        val request = object : StringRequest(Method.POST, url, { response ->
            val json = JSONObject(response)
            if (json.getBoolean("success")) {
                val posts = json.getJSONArray("posts")
                allProducts.clear()
                for (i in 0 until posts.length()) {
                    val post = posts.getJSONObject(i)
                    allProducts.add(
                        ModelSearchProduct(
                            title = post.getString("title"),
                            price = post.getString("price"),
                            imageBase64 = post.getString("image"),
                            description = post.getString("description"),
                            category = post.getString("category"),
                            p_id = post.getInt("id"),
                            u_id = post.getInt("user_id")
                        )
                    )
                }
               // productAdapter.updateData(allProducts)
            } else {
                Toast.makeText(this, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        }, {
            Toast.makeText(this, "Network error: ${it.message}", Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): Map<String, String> {
                return mapOf("user_id" to userId.toString())
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun setupBottomNav() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_search
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Homescreen::class.java))
                    finishAffinity()
                    true
                }
                R.id.nav_search -> true
                R.id.nav_add -> {
                    startActivity(Intent(this, AddProducts::class.java))
                    true
                }
                R.id.nav_my_products -> {
                    startActivity(Intent(this, MyProducts::class.java))
                    finish()
                    true
                }
                R.id.nav_message -> {
                    startActivity(Intent(this, Messages::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    private fun decodeBase64ToBitmap(base64Str: String): Bitmap {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}
