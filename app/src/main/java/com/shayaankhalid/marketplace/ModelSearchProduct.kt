package com.shayaankhalid.marketplace

data class ModelSearchProduct(
    val title: String,
    val price: String,
    val imageBase64: String,
    val description: String,
    val category: String,
    val p_id: Int
)