package com.guigo.mocohub

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.guigo.mocohub.databinding.ItemNewsBinding
import com.guigo.mocohub.model.NewsItem

class NewsAdapter : RecyclerView.Adapter<NewsAdapter.VH>() {
    private var items = emptyList<NewsItem>()
    fun submitList(newItems: List<NewsItem>) { items = newItems; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(private val b: ItemNewsBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: NewsItem) {
            b.textNewsSource.text = item.source
            b.textNewsTitle.text = item.title
            b.textNewsSummary.text = item.summary
            b.textNewsSummary.visibility = if (item.summary.isBlank()) View.GONE else View.VISIBLE
            b.textNewsMeta.text = listOf(item.published.takeIf { it.isNotBlank() }, "Toque para ler").filterNotNull().joinToString(" • ")
            if (item.imageUrl.isNotBlank()) {
                b.imageNews.visibility = View.VISIBLE
                b.imageNews.load(item.imageUrl) { crossfade(true) }
            } else b.imageNews.visibility = View.GONE
            b.root.setOnClickListener { b.root.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
        }
    }
}
