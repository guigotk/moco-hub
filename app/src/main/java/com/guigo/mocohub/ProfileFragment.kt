package com.guigo.mocohub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.guigo.mocohub.data.PlayerStatsRepository
import com.guigo.mocohub.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            binding.profileImage.setImageURI(uri)
            prefs().edit().putString("profile_image", uri.toString()).apply()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val p = prefs()
        binding.editName.setText(p.getString("profile_name", ""))
        binding.editAge.setText(p.getString("profile_age", ""))
        binding.editBio.setText(p.getString("profile_bio", ""))
        binding.editGameId.setText(p.getString("profile_game_id", ""))
        p.getString("profile_image", null)?.let { runCatching { binding.profileImage.setImageURI(Uri.parse(it)) } }

        // Keep the profile screen anchored at the top when it opens. Text fields can
        // otherwise steal initial focus and make ScrollView restore a position mid-card.
        binding.profileScroll.isFocusableInTouchMode = true
        binding.profileScroll.requestFocus()
        binding.profileScroll.post { binding.profileScroll.scrollTo(0, 0) }

        binding.profileImage.setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        binding.btnChangePhoto.setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnLoadStats.setOnClickListener { loadStats() }
        binding.btnRatingInfo.setOnClickListener { showRatingInfo() }
        binding.btnRetryStats.setOnClickListener { loadStats() }
        binding.editGameId.doAfterTextChanged {
            binding.gameIdLayout.error = null
            updateFirstUseState()
        }
        updateFirstUseState()
        if (binding.editGameId.text?.isNotBlank() == true) loadStats()
    }

    private fun prefs() = requireContext().getSharedPreferences("moco_hub_profile", Context.MODE_PRIVATE)

    private fun saveProfile() {
        val tag = cleanTag(binding.editGameId.text?.toString().orEmpty())
        if (tag.isNotBlank() && !isValidTag(tag)) {
            binding.gameIdLayout.error = "Use de 6 a 16 letras ou números"
            binding.editGameId.requestFocus()
            return
        }
        binding.gameIdLayout.error = null
        binding.editGameId.setText(tag)
        val bio = binding.editBio.text?.toString().orEmpty().take(45)
        binding.editBio.setText(bio)
        prefs().edit()
            .putString("profile_name", binding.editName.text?.toString().orEmpty())
            .putString("profile_age", binding.editAge.text?.toString().orEmpty())
            .putString("profile_bio", bio)
            .putString("profile_game_id", tag)
            .apply()
        Toast.makeText(requireContext(), "Perfil salvo", Toast.LENGTH_SHORT).show()
        updateFirstUseState()
        if (tag.isNotBlank()) loadStats()
    }

    private fun displayValue(value: String): String = value.takeUnless { it.isBlank() || it == "—" } ?: "N/D"
    private fun displayTop(value: Int?): String = value?.let { "Top $it%" } ?: ""
    private fun cleanTag(value: String) = value.trim().removePrefix("#").replace(" ", "").uppercase()
    private fun isValidTag(tag: String) = tag.matches(Regex("[A-Z0-9]{6,16}"))

    private fun updateFirstUseState() {
        val hasTag = cleanTag(binding.editGameId.text?.toString().orEmpty()).isNotBlank()
        binding.cardFirstUse.visibility = if (hasTag) View.GONE else View.VISIBLE
        binding.statsSectionHeader.visibility = if (hasTag) View.VISIBLE else View.GONE
        binding.btnLoadStats.isEnabled = hasTag
        if (!hasTag) {
            binding.cardPlayerStats.visibility = View.GONE
            binding.textStatsError.visibility = View.GONE
            binding.btnRetryStats.visibility = View.GONE
            binding.progressPlayerStats.visibility = View.GONE
        }
    }

    private fun showStatsError(message: String) {
        binding.textStatsError.text = message
        binding.textStatsError.visibility = View.VISIBLE
        binding.btnRetryStats.visibility = View.VISIBLE
    }

    private fun loadStats() {
        val tag = cleanTag(binding.editGameId.text?.toString().orEmpty())
        if (tag.isBlank()) {
            updateFirstUseState()
            Toast.makeText(requireContext(), "Informe o Hunter ID", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isValidTag(tag)) {
            binding.gameIdLayout.error = "Use de 6 a 16 letras ou números"
            binding.editGameId.requestFocus()
            return
        }
        binding.gameIdLayout.error = null
        binding.editGameId.setText(tag)
        binding.progressPlayerStats.visibility = View.VISIBLE
        binding.cardPlayerStats.visibility = View.GONE
        binding.textStatsError.visibility = View.GONE
        binding.btnRetryStats.visibility = View.GONE

        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { PlayerStatsRepository.fetch(appContext, tag) } }
            if (_binding == null) return@launch
            binding.progressPlayerStats.visibility = View.GONE
            result.onSuccess { pair ->
                var stats = pair.first
                val cached = pair.second
                if (stats == null) {
                    showStatsError("Não foi possível consultar o Hunter agora. Confira o ID ou tente novamente em alguns instantes.")
                } else {
                    binding.cardPlayerStats.visibility = View.VISIBLE
                    binding.textStatsName.text = stats.name
                    binding.textStatsTitle.text = stats.title.ifBlank { "Hunter" }
                    binding.textStatsTag.text = "#${stats.tag}"

                    binding.textCareerLevel.text = displayValue(stats.careerLevel)
                    binding.textCareerTop.text = displayTop(stats.careerTopPercent)
                    binding.textCollectorLevel.text = displayValue(stats.collectorLevel)
                    binding.textCollectorTop.text = displayTop(stats.collectorTopPercent)
                    binding.textEliteMerits.text = displayValue(stats.eliteMerits)
                    binding.textEliteMeritsTop.text = displayTop(stats.eliteMeritsTopPercent)
                    binding.textCurrentLevel.text = displayValue(stats.currentLevel)

                    binding.textRating.text = displayValue(stats.rating)
                    binding.textRatingTier.text = stats.rating.toIntOrNull()?.let(::ratingTier).orEmpty()
                    fun bindScore(value: Int?, bar: android.widget.ProgressBar, label: android.widget.TextView) {
                        bar.progress = value ?: 0
                        label.text = value?.toString() ?: "N/D"
                        bar.alpha = if (value == null) 0.28f else 1f
                    }
                    bindScore(stats.eliteScore, binding.progressEliteScore, binding.textEliteScore)
                    bindScore(stats.grindScore, binding.progressGrindScore, binding.textGrindScore)
                    bindScore(stats.progressionScore, binding.progressProgressionScore, binding.textProgressionScore)
                    bindScore(stats.collectionScore, binding.progressCollectionScore, binding.textCollectionScore)

                    val missingCore = listOf(stats.rating, stats.currentLevel).count { it.isBlank() || it == "—" }
                    val missingPercentiles = listOf(stats.careerTopPercent, stats.collectorTopPercent, stats.eliteMeritsTopPercent).count { it == null }
                    binding.textStatsAvailability.visibility = if (missingCore > 0 || missingPercentiles > 0) View.VISIBLE else View.GONE
                    binding.textStatsAvailability.text = when {
                        cached -> "Dados em cache • alguns campos podem estar desatualizados"
                        else -> "Alguns campos podem não ser fornecidos pela fonte nesta consulta"
                    }
                    binding.textStatsUpdated.text = if (cached) "Dados salvos • ${stats.lastUpdated}" else "Atualizado ${stats.lastUpdated}"
                }
            }.onFailure {
                showStatsError("Não foi possível consultar o perfil agora. Verifique sua conexão e tente novamente.")
            }
        }
    }

    private fun ratingTier(rating: Int): String = when (rating) {
        in 0..19 -> "Recruit"
        in 20..39 -> "Rookie Hunter"
        in 40..59 -> "Junior Hunter"
        in 60..84 -> "Advanced Hunter"
        else -> "Veteran Hunter"
    }

    private fun showRatingInfo() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Como o Rating funciona")
            .setMessage(
                "O Rating é calculado pelo mo.co hub usando os quatro indicadores do Hunter.\n\n" +
                    "Elite • 30%\nMéritos Elite da temporada atual.\n\n" +
                    "Grind • 30%\nProgresso e esforço da temporada atual.\n\n" +
                    "Progression • 20%\nProgressão de longo prazo da conta.\n\n" +
                    "Collection • 20%\nEvolução da coleção.\n\n" +
                    "Faixas\nRecruit 0–19\nRookie Hunter 20–39\nJunior Hunter 40–59\nAdvanced Hunter 60–84\nVeteran Hunter 85–100\n\n" +
                    "O Rating só é calculado quando os quatro indicadores estão disponíveis."
            )
            .setPositiveButton("Fechar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
