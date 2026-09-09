package com.guigo.mocohub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.guigo.mocohub.databinding.ActivityEventDetailBinding
import com.guigo.mocohub.util.Prefs
import com.guigo.mocohub.data.EventsRepository
import com.guigo.mocohub.work.EventAlertScheduler

class EventDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEventDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra("name").orEmpty()
        val type = intent.getStringExtra("type") ?: "other"
        val time = intent.getStringExtra("time").orEmpty()
        binding.toolbarDetail.setNavigationOnClickListener { finish() }
        binding.textDetailTitle.text = name
        binding.textDetailTime.text = time
        binding.imageDetailEvent.setImageResource(if (type == "overcharged") R.drawable.ic_event_overcharged else R.drawable.ic_event_chaos)
        binding.textDetailDescription.text = when (type) {
            "overcharged" -> getString(R.string.overcharged_description)
            "chaos" -> getString(R.string.chaos_description)
            else -> getString(R.string.generic_event_description)
        }

        val selected = Prefs.getLeadTimesForEvent(this, type)
        binding.check15.isChecked = "15" in selected
        binding.check10.isChecked = "10" in selected
        binding.check5.isChecked = "5" in selected
        binding.checkStart.isChecked = "0" in selected
        binding.btnSaveEventAlerts.setOnClickListener {
            val set = mutableSetOf<String>()
            if (binding.check15.isChecked) set += "15"
            if (binding.check10.isChecked) set += "10"
            if (binding.check5.isChecked) set += "5"
            if (binding.checkStart.isChecked) set += "0"
            Prefs.setLeadTimesForEvent(this, type, set)
            EventAlertScheduler.scheduleUpcoming(this, EventsRepository.loadCache(this))
            binding.textSaved.visibility = android.view.View.VISIBLE
        }
    }
}
