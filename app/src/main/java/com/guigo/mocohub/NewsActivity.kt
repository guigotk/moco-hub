package com.guigo.mocohub

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.guigo.mocohub.data.NewsRepository
import com.guigo.mocohub.databinding.ActivityNewsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewsBinding
    private val adapter = NewsAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarNews.setNavigationOnClickListener { finish() }
        binding.listAllNews.layoutManager = LinearLayoutManager(this)
        binding.listAllNews.adapter = adapter
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { NewsRepository.fetch() }
            adapter.submitList(items)
            binding.progressNews.visibility = View.GONE
        }
    }
}
