package com.guigo.mocohub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.guigo.mocohub.data.EventsRepository
import com.guigo.mocohub.data.NewsRepository
import com.guigo.mocohub.data.SeasonRepository
import com.guigo.mocohub.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val newsAdapter = NewsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnOpenEvents.setOnClickListener { (activity as? MainActivity)?.selectEventsTab() }
        binding.cardEliteContracts.setOnClickListener { startActivity(Intent(requireContext(), EliteContractsActivity::class.java)) }
        binding.cardQuickProfile.setOnClickListener { (activity as? MainActivity)?.selectProfileTab() }
        binding.cardPartnerX.setOnClickListener { open(AppLinks.PARTNER_X_URL) }
        binding.cardPartnerInstagram.setOnClickListener { open(AppLinks.PARTNER_INSTAGRAM_URL) }
        binding.cardPartnerDiscordBr.setOnClickListener { open(AppLinks.DISCORD_BR_PT_URL) }
        binding.cardPartnerQuantum.setOnClickListener { open(AppLinks.DISCORD_QUANTUM_URL) }
        binding.btnAllNews.setOnClickListener { startActivity(Intent(requireContext(), NewsActivity::class.java)) }

        binding.listNews.layoutManager = LinearLayoutManager(requireContext())
        binding.listNews.adapter = newsAdapter
        binding.listNews.isNestedScrollingEnabled = false

        loadNextEvent()
        loadNews()
        updateSeason()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) { updateSeason(); delay(60_000) }
            }
        }
    }

    private fun open(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun loadNews() {
        binding.progressHome.visibility = View.VISIBLE
        binding.textNewsStatus.text = "Atualizando notícia em destaque…"
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { NewsRepository.fetch() }
            if (_binding == null) return@launch
            newsAdapter.submitList(items.take(1))
            binding.progressHome.visibility = View.GONE
            binding.textNewsStatus.text = if (items.isNotEmpty()) "Fonte: CellString • toque no card para abrir a matéria" else "Notícias temporariamente indisponíveis."
        }
    }

    private fun updateSeason() {
        if (_binding == null) return
        val season = SeasonRepository.current()
        binding.textSeasonTitle.text = season.title
        binding.textSeasonCountdown.text = season.subtitle
        binding.progressSeason.progress = season.progressPercent
    }

    private fun loadNextEvent() {
        binding.progressHome.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { EventsRepository.fetchUpcomingEvents(requireContext()) }
            if (_binding == null) return@launch
            val next = result.events.filter { it.etaMillis > System.currentTimeMillis() }.minByOrNull { it.etaMillis }
            next?.let {
                binding.textNextEventName.text = it.name
                val mins = TimeUnit.MILLISECONDS.toMinutes((it.etaMillis - System.currentTimeMillis()).coerceAtLeast(0))
                binding.textNextEventEta.text = "Começa em $mins min"
                binding.imageNextEvent.setImageResource(if (it.typeKey == "overcharged") R.drawable.ic_event_overcharged else R.drawable.ic_event_chaos)
            } ?: run {
                binding.textNextEventName.text = "Sem evento carregado"
                binding.textNextEventEta.text = "Abra Eventos para atualizar"
            }
            binding.progressHome.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        binding.listNews.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
