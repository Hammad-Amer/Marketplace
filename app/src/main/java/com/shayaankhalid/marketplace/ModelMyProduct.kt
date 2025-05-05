package com.shayaankhalid.marketplace

data class ModelMyProduct(
    val id: Int,               // ← server product ID
    val title: String,
    val price: String,
    val imageBase64: String
)
