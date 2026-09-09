package com.guigo.mocohub

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guigo.mocohub.data.EliteContractsRepository
import com.guigo.mocohub.databinding.ActivityEliteContractsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EliteContractsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEliteContractsBinding
    private val adapter = EliteContractsAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEliteContractsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarElite.setNavigationOnClickListener { finish() }
        binding.listElite.layoutManager = LinearLayoutManager(this)
        binding.listElite.adapter = adapter
        binding.swipeElite.setOnRefreshListener { load() }
        load()
    }
    private fun load() {
        binding.progressElite.visibility = View.VISIBLE
        lifecycleScope.launch {
            val (rows, cached) = withContext(Dispatchers.IO) { EliteContractsRepository.fetch(this@EliteContractsActivity) }
            binding.progressElite.visibility = View.GONE
            binding.swipeElite.isRefreshing = false
            adapter.submitList(rows)
            binding.textEliteStatus.text = when {
                rows.isEmpty() -> "Não foi possível carregar o leaderboard agora."
                cached -> "Sem conexão • exibindo a última lista salva"
                else -> "Atualizado pelo CellString • ${rows.size} Hunters"
            }
        }
    }
}
