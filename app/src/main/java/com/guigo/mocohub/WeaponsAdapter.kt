package com.guigo.mocohub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.guigo.mocohub.databinding.ItemWeaponBinding
import com.guigo.mocohub.model.Weapon

class WeaponsAdapter : RecyclerView.Adapter<WeaponsAdapter.WeaponViewHolder>() {

    private var items: List<Weapon> = emptyList()

    fun submitList(newItems: List<Weapon>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeaponViewHolder {
        val binding = ItemWeaponBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WeaponViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeaponViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class WeaponViewHolder(private val binding: ItemWeaponBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(weapon: Weapon) {
            binding.textWeaponName.text = weapon.name
            binding.textWeaponSkills.text = weapon.skills.joinToString("\n") { "• $it" }

            if (!weapon.imageAsset.isNullOrBlank()) {
                try {
                    val context = binding.root.context
                    context.assets.open("weapons/${weapon.imageAsset}").use { stream ->
                        val drawable = android.graphics.drawable.Drawable.createFromStream(
                            stream, weapon.imageAsset
                        )
                        binding.imageWeapon.setImageDrawable(drawable)
                    }
                } catch (e: Exception) {
                    binding.imageWeapon.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                binding.imageWeapon.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}
