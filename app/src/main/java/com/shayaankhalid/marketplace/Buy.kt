package com.shayaankhalid.marketplace

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Buy : AppCompatActivity() {

    private var originalAmount = 7000
    private var isCouponApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy)

        val orderAmountText = findViewById<TextView>(R.id.orderAmount)
        val orderTotalText = findViewById<TextView>(R.id.orderTotal)
        val selectCouponButton = findViewById<Button>(R.id.btnSelectCoupon)
        val couponText = findViewById<TextView>(R.id.changetext)

        orderAmountText.text = String.format("%,d.00 PKR", originalAmount)
        orderTotalText.text = String.format("%,d.00 PKR", originalAmount)

        selectCouponButton.setOnClickListener {
            if (!isCouponApplied) {
                val discountedAmount = originalAmount - 500
                orderTotalText.text = String.format("%,d.00 PKR", discountedAmount)
                isCouponApplied = true
                selectCouponButton.text = "Applied"
                couponText.text = "Coupon Applied"
            }
        }

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        val confirmButton = findViewById<Button>(R.id.btnProceedPayment)
        confirmButton.setOnClickListener {
           finish()
        }

    }
}
