package com.shayaankhalid.marketplace

data class ChatMessageModel(
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: Long
)