package com.guigo.mocohub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.guigo.mocohub.databinding.ActivityMainBinding
import com.guigo.mocohub.work.WorkScheduler

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val notifPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        if (savedInstanceState == null) showFragment(HomeFragment())
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { showFragment(HomeFragment()); true }
                R.id.nav_events -> { showFragment(EventsFragment()); true }
                R.id.nav_profile -> { showFragment(ProfileFragment()); true }
                else -> false
            }
        }
        if (intent?.getBooleanExtra("open_events", false) == true) {
            binding.bottomNav.selectedItemId = R.id.nav_events
        }
        askNotificationPermissionIfNeeded()
        WorkScheduler.schedulePeriodicCheck(this)
        WorkScheduler.runCheckNow(this)
    }

    fun selectEventsTab() { binding.bottomNav.selectedItemId = R.id.nav_events }
    fun selectProfileTab() { binding.bottomNav.selectedItemId = R.id.nav_profile }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
