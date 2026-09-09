package com.guigo.mocohub

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guigo.mocohub.data.EventsRepository
import com.guigo.mocohub.databinding.FragmentEventsBinding
import com.guigo.mocohub.model.GameEvent
import com.guigo.mocohub.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EventsFragment : Fragment() {
    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private val adapter = EventsAdapter { openDetail(it) }
    private var allEvents: List<GameEvent> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEvents.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { loadEvents() }
        binding.chipAll.setOnClickListener { applyFilter("all") }
        binding.chipChaos.setOnClickListener { applyFilter("chaos") }
        binding.chipOvercharged.setOnClickListener { applyFilter("overcharged") }
        when (Prefs.getEventFilter(requireContext())) {
            "chaos" -> binding.chipChaos.isChecked = true
            "overcharged" -> binding.chipOvercharged.isChecked = true
            else -> binding.chipAll.isChecked = true
        }
        loadEvents()
    }

    private fun loadEvents() {
        binding.swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { EventsRepository.fetchUpcomingEvents(requireContext()) }
            if (_binding == null) return@launch
            allEvents = result.events
            applyFilter(Prefs.getEventFilter(requireContext()))
            renderNextCard(result.events.minByOrNull { it.etaMillis })
            renderTimeline(result.events.take(6))
            renderHistory(EventsRepository.loadHistory(requireContext()))
            binding.offlineBanner.visibility = if (result.fromCache) View.VISIBLE else View.GONE
            if (result.fromCache && result.updatedAt > 0) {
                val mins = TimeUnit.MILLISECONDS.toMinutes((System.currentTimeMillis() - result.updatedAt).coerceAtLeast(0))
                binding.offlineBanner.text = getString(R.string.offline_cache_age, mins)
            }
            binding.swipeRefresh.isRefreshing = false
            binding.textEmpty.visibility = if (result.events.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun applyFilter(filter: String) {
        Prefs.setEventFilter(requireContext(), filter)
        val filtered = if (filter == "all") allEvents else allEvents.filter { it.typeKey == filter }
        adapter.submitList(filtered)
    }

    private fun renderNextCard(event: GameEvent?) {
        if (event == null) { binding.cardNextEvent.visibility = View.GONE; return }
        binding.cardNextEvent.visibility = View.VISIBLE
        binding.textNextEventName.text = event.name
        binding.textNextEventTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.etaMillis))
        val mins = ((event.etaMillis - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
        binding.textNextEventCountdown.text = getString(R.string.in_minutes, mins)
        binding.imageNextEvent.setImageResource(if (event.typeKey == "overcharged") R.drawable.ic_event_overcharged else R.drawable.ic_event_chaos)
        binding.cardNextEvent.setOnClickListener { openDetail(event) }
    }

    private fun renderTimeline(events: List<GameEvent>) {
        binding.timelineContainer.removeAllViews()
        events.forEachIndexed { index, event ->
            val t = TextView(requireContext()).apply {
                text = if (index == 0) "AGORA  •  ${event.timeLabel}\n${event.name}" else "${event.timeLabel}\n${event.name}"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                setPadding(20, 14, 20, 14)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_timeline_pill)
                setOnClickListener { openDetail(event) }
            }
            val lp = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = 12 }
            binding.timelineContainer.addView(t, lp)
        }
    }

    private fun renderHistory(history: List<GameEvent>) {
        binding.textHistoryValue.text = if (history.isEmpty()) getString(R.string.no_history) else history.joinToString("\n") { "${it.timeLabel}  •  ${it.name}" }
    }

    private fun openDetail(event: GameEvent) {
        startActivity(Intent(requireContext(), EventDetailActivity::class.java).apply {
            putExtra("name", event.name); putExtra("time", event.timeLabel); putExtra("eta", event.etaMillis); putExtra("type", event.typeKey)
        })
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
