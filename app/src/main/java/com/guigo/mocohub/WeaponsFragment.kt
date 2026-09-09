package com.guigo.mocohub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.guigo.mocohub.data.WeaponsRepository
import com.guigo.mocohub.databinding.FragmentWeaponsBinding

class WeaponsFragment : Fragment() {

    private var _binding: FragmentWeaponsBinding? = null
    private val binding get() = _binding!!
    private val adapter = WeaponsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeaponsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerWeapons.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerWeapons.adapter = adapter
        adapter.submitList(WeaponsRepository.loadWeapons(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
