package com.guigo.mocohub

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.guigo.mocohub.databinding.ItemEventBinding
import com.guigo.mocohub.model.GameEvent
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapter(private val onClick: (GameEvent) -> Unit) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {
    private var items: List<GameEvent> = emptyList()
    fun submitList(newItems: List<GameEvent>) { items = newItems; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EventViewHolder(ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClick)
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    class EventViewHolder(private val b: ItemEventBinding, private val onClick: (GameEvent) -> Unit) : RecyclerView.ViewHolder(b.root) {
        private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        fun bind(event: GameEvent) {
            b.textEventName.text = event.name
            b.textEventTime.text = sdf.format(Date(event.etaMillis))
            b.imageEventIcon.setImageResource(if (event.typeKey == "overcharged") R.drawable.ic_event_overcharged else R.drawable.ic_event_chaos)
            val minutes = ((event.etaMillis - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
            b.textEventCountdown.text = if (minutes <= 0) b.root.context.getString(R.string.live_now) else b.root.context.getString(R.string.in_minutes, minutes)
            val color = when {
                minutes <= 5 -> R.color.urgency_red
                minutes <= 15 -> R.color.urgency_amber
                else -> R.color.text_secondary
            }
            b.textEventCountdown.setTextColor(ContextCompat.getColor(b.root.context, color))
            b.root.setOnClickListener { onClick(event) }
        }
    }
}
