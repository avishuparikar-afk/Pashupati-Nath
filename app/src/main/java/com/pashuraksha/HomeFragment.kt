package com.pashuraksha

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pashuraksha.data.OfflineDataRepository
import com.pashuraksha.databinding.FragmentHomeBinding

/**
 * Redesigned home — premium rural-tech aesthetic.
 * Hero card: Scan Animal. Quick actions grid. Pashupatinath featured row.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Preload CSV datasets on first landing
        OfflineDataRepository.ensureLoaded(requireContext())

        // Observe ViewModel data (only fields that still exist in new layout)
        homeViewModel.greeting.observe(viewLifecycleOwner) {
            binding.greetingTextView.text = it
        }
        homeViewModel.villageName.observe(viewLifecycleOwner) {
            binding.villageNameTextView.text = it
        }
        homeViewModel.weather.observe(viewLifecycleOwner) {
            binding.weatherTextView.text = "☀️  ${it ?: "25°C"}"
        }

        // Hero + quick-action click routing
        binding.btnStartScan.setOnClickListener {
            safeNavigate(R.id.scanFragment)
        }
        binding.btnImmunityCalculator.setOnClickListener {
            safeNavigate(R.id.immunityFragment)
        }
        binding.btnOutbreakMap.setOnClickListener {
            safeNavigate(R.id.mapFragment)
        }
        binding.btnHealthReport.setOnClickListener {
            safeNavigate(R.id.reportFragment)
        }

        // Chatbot — deep link to disease detection (chat UI added via MainActivity intent)
        binding.btnChatbot.setOnClickListener {
            val intent = Intent(requireContext(), ChatActivity::class.java)
            startActivity(intent)
        }

        // Pashupatinath Mode - the wow moment
        binding.btnPashupatinathMode.setOnClickListener {
            val intent = Intent(requireContext(), CosmicEnergyActivity::class.java)
            startActivity(intent)
        }
    }

    private fun safeNavigate(destId: Int) {
        try {
            findNavController().navigate(destId)
        } catch (_: Throwable) { /* silently ignore if not in graph yet */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
