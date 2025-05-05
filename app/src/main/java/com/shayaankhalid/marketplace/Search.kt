package com.shayaankhalid.marketplace

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class Search : AppCompatActivity() {

    private lateinit var productAdapter: SearchProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        //  val categoryRecyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        val productRecyclerView = findViewById<RecyclerView>(R.id.productRecyclerView)

//        // Sample data
//        val categories = listOf(
//            Category(R.drawable.ic_laptop, "Laptops"),
//            Category(R.drawable.ic_mobile, "Mobiles"),
//            Category(R.drawable.ic_tablet, "Tablets"),
//            Category(R.drawable.ic_watch, "Watches"),
//            Category(R.drawable.ic_camera, "Cameras")
//        )

        val products = listOf(
            ModelSearchProduct("Dell XPS 13",  "300,000 PKR", R.drawable.dell_xps),
            ModelSearchProduct("Samsung Galaxy S24 Ultra",  "399,999 PKR", R.drawable.s24_ultra),
            ModelSearchProduct("Sony Alpha a6400",  "280,000 PKR", R.drawable.sony_camera),
            ModelSearchProduct("Apple Watch Series 9",  "130,000 PKR", R.drawable.apple_watch)
        )

//        categoryAdapter = CategoryAdapter(categories)
//        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        categoryRecyclerView.adapter = categoryAdapter



        productAdapter = SearchProductAdapter(products) { product ->
            val intent = Intent(this, ViewProduct::class.java)
            intent.putExtra("title", "dell xps 13")
            intent.putExtra("description", "Great for professionals")
            intent.putExtra("price", "300,000 PKR")
            intent.putExtra("image", R.drawable.dell_xps)
            startActivity(intent)
        }
        productRecyclerView.layoutManager = GridLayoutManager(this, 2)
        productRecyclerView.adapter = productAdapter



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
//                R.id.nav_profile -> { startActivity(Intent(this, UserProfile::class.java)); true }
                R.id.nav_my_products -> {
                    startActivity(Intent(this, MyProducts::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

    }

}