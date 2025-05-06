package com.shayaankhalid.marketplace

data class Product(
    val title: String,
    val description: String,
    val price: String,
    val imageBase64: String,
    val p_id: Int
)