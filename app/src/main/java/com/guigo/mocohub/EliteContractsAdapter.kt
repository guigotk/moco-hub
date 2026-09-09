package com.guigo.mocohub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.guigo.mocohub.databinding.ItemEliteContractBinding
import com.guigo.mocohub.model.EliteContractEntry

class EliteContractsAdapter : RecyclerView.Adapter<EliteContractsAdapter.VH>() {
    private var items = emptyList<EliteContractEntry>()
    fun submitList(v: List<EliteContractEntry>) { items = v; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemEliteContractBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    class VH(private val b: ItemEliteContractBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: EliteContractEntry) { b.textEliteRank.text = "#${e.rank}"; b.textEliteName.text = e.name; b.textEliteValue.text = e.value }
    }
}
