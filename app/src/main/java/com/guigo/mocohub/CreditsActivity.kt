package com.guigo.mocohub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.guigo.mocohub.databinding.ActivityCreditsBinding

class CreditsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreditsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreditsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarCredits.setNavigationOnClickListener { finish() }
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.5.5" }
        binding.textAppVersion.text = "mo.co hub v$versionName"
        binding.btnCellStringEvents.setOnClickListener { open(AppLinks.CELLSTRING_EVENTS) }
        binding.btnCellString.setOnClickListener { open(AppLinks.CELLSTRING_HOME) }
        binding.btnXSource.setOnClickListener { open(AppLinks.X_PROFILE_URL) }
        binding.btnCellStringPlayer.setOnClickListener {
            val tag = getSharedPreferences("moco_hub_profile", MODE_PRIVATE)
                .getString("profile_game_id", "")
                .orEmpty()
                .trim()
                .removePrefix("#")
                .uppercase()
            open(if (tag.isNotBlank()) AppLinks.CELLSTRING_PLAYER_BASE + tag else AppLinks.CELLSTRING_HOME)
        }
        binding.btnMoco.setOnClickListener { open("https://moco.supercell.com/") }
        binding.btnGithub.setOnClickListener { open(AppLinks.GITHUB_URL) }
    }
    private fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        else android.widget.Toast.makeText(this, "Nenhum aplicativo disponível para abrir este link", android.widget.Toast.LENGTH_SHORT).show()
    }
}
