package com.shayaankhalid.marketplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val messages: List<ChatMessageModel>,
    private val currentUserId: Int
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.textMessage.text = message.content

        val layoutParams = holder.textMessage.layoutParams as ViewGroup.MarginLayoutParams
        val params = holder.textMessage.layoutParams as RelativeLayout.LayoutParams

        if (message.senderId == currentUserId) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
            params.removeRule(RelativeLayout.ALIGN_PARENT_START)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START)
            params.removeRule(RelativeLayout.ALIGN_PARENT_END)
        }

        holder.textMessage.layoutParams = layoutParams
    }

    override fun getItemCount(): Int = messages.size
}
