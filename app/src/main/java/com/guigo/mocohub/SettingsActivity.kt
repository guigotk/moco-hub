package com.guigo.mocohub

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.guigo.mocohub.databinding.ActivitySettingsBinding
import com.guigo.mocohub.util.Prefs

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarSettings.setNavigationOnClickListener { finish() }

        when (Prefs.getThemeMode(this)) {
            "light" -> binding.radioLight.isChecked = true
            "dark" -> binding.radioDark.isChecked = true
            else -> binding.radioSystem.isChecked = true
        }

        val language = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        when {
            language.startsWith("en") -> binding.radioEnglish.isChecked = true
            language.startsWith("es") -> binding.radioSpanish.isChecked = true
            else -> binding.radioPortuguese.isChecked = true
        }

        binding.btnApplyPreferences.setOnClickListener {
            val theme = when (binding.radioTheme.checkedRadioButtonId) {
                R.id.radioLight -> "light"
                R.id.radioDark -> "dark"
                else -> "system"
            }
            Prefs.setThemeMode(this, theme)
            MocoHubApp.applyTheme(theme)

            val tag = when (binding.radioLanguage.checkedRadioButtonId) {
                R.id.radioEnglish -> "en"
                R.id.radioSpanish -> "es"
                else -> "pt-BR"
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_SHORT).show()
        }

        binding.rowSupport.root.setOnClickListener { openSupportEmail() }
        binding.rowX.root.setOnClickListener { openConfiguredUrl(AppLinks.PARTNER_X_URL) }
        binding.rowInstagram.root.setOnClickListener { openConfiguredUrl(AppLinks.PARTNER_INSTAGRAM_URL) }
        binding.rowDiscordBr.root.setOnClickListener { openConfiguredUrl(AppLinks.DISCORD_BR_PT_URL) }
        binding.rowDiscordQuantum.root.setOnClickListener { openConfiguredUrl(AppLinks.DISCORD_QUANTUM_URL) }
        binding.rowPaypal.root.setOnClickListener {
            if (AppLinks.PAYPAL_URL.startsWith("http")) openUrl(AppLinks.PAYPAL_URL)
            else Toast.makeText(this, R.string.paypal_unavailable, Toast.LENGTH_SHORT).show()
        }
        binding.rowPix.root.setOnClickListener { copyPix() }
        binding.rowCredits.root.setOnClickListener { startActivity(Intent(this, CreditsActivity::class.java)) }
    }

    private fun openSupportEmail() {
        if (!AppLinks.SUPPORT_EMAIL.contains("@")) {
            Toast.makeText(this, R.string.configure_email, Toast.LENGTH_SHORT).show(); return
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${AppLinks.SUPPORT_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, "Suporte — mo.co hub")
        }
        runCatching { startActivity(intent) }.onFailure { Toast.makeText(this, R.string.no_email_app, Toast.LENGTH_SHORT).show() }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        else Toast.makeText(this, "Nenhum aplicativo disponível para abrir este link", Toast.LENGTH_SHORT).show()
    }

    private fun openConfiguredUrl(url: String) {
        if (!url.startsWith("http")) {
            Toast.makeText(this, R.string.configure_partner_link, Toast.LENGTH_SHORT).show()
            return
        }
        openUrl(url)
    }

    private fun copyPix() {
        if (AppLinks.PIX_KEY.startsWith("SUA_")) {
            Toast.makeText(this, R.string.configure_pix, Toast.LENGTH_SHORT).show(); return
        }
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("PIX", AppLinks.PIX_KEY))
        Toast.makeText(this, R.string.pix_copied, Toast.LENGTH_SHORT).show()
    }
}
