package com.shayaankhalid.marketplace


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchProductAdapter(private val products: List<ModelSearchProduct>,private val onItemClick: (ModelSearchProduct) -> Unit) :
    RecyclerView.Adapter<SearchProductAdapter.SearchProductViewHolder>() {

    inner class SearchProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.productImage)
        val title: TextView = itemView.findViewById(R.id.productTitle)
        val price: TextView = itemView.findViewById(R.id.productPrice)

        init {
            itemView.setOnClickListener {
                onItemClick(products[adapterPosition])
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_product, parent, false)
        return SearchProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchProductViewHolder, position: Int) {
        val product = products[position]
        holder.image.setImageResource(product.imageResId)
        holder.title.text = product.title
        holder.price.text = product.price
    }

    override fun getItemCount() = products.size
}

