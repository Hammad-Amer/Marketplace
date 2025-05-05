package com.shayaankhalid.marketplace

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyProductsAdapter(
    private val productList: MutableList<ModelMyProduct>,
    private val onDeleteClick: (productId: Int, position: Int) -> Unit
) : RecyclerView.Adapter<MyProductsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productTitle: TextView = itemView.findViewById(R.id.productTitle)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        // Decode base64 string to bitmap
        try {
            val imageBytes = Base64.decode(product.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            holder.productImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            holder.productImage.setImageResource(R.drawable.empty_user)
        }

        holder.productTitle.text = product.title
        holder.productPrice.text = product.price

        holder.deleteIcon.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                onDeleteClick(product.id, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < productList.size) {
            productList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, productList.size)
        }
    }
}
