package com.pashuraksha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.pashuraksha.databinding.FragmentImmunityBinding

class ImmunityFragment : Fragment() {

    private var _binding: FragmentImmunityBinding? = null
    private val binding get() = _binding!!
    private val immunityViewModel: ImmunityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImmunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup District Dropdown
        val districts = arrayOf("Pune", "Nashik", "Nagpur", "Mumbai", "Aurangabad") // Example districts
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, districts)
        binding.districtAutoCompleteTextView.setAdapter(adapter)

        binding.calculateButton.setOnClickListener {
            calculateImmunityGap()
        }

        observeViewModel()
    }

    private fun calculateImmunityGap() {
        val villageName = binding.villageNameEditText.text.toString()
        val district = binding.districtAutoCompleteTextView.text.toString()
        val totalCattleStr = binding.totalCattleEditText.text.toString()

        if (villageName.isEmpty() || district.isEmpty() || totalCattleStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val totalCattle = totalCattleStr.toIntOrNull()
        if (totalCattle == null || totalCattle <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid total cattle count", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCattleType = when (binding.cattleTypeRadioGroup.checkedRadioButtonId) {
            R.id.radioCow -> "Cow"
            R.id.radioBuffalo -> "Buffalo"
            R.id.radioMixed -> "Mixed"
            else -> {
                Toast.makeText(requireContext(), "Please select a cattle type", Toast.LENGTH_SHORT).show()
                return
            }
        }

        immunityViewModel.calculateImmunityGap(villageName, district, totalCattle, selectedCattleType)
    }

    private fun observeViewModel() {
        immunityViewModel.immunityGapPercentage.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.resultLayout.visibility = View.VISIBLE
                updatePieChart(it)
                updateRiskAssessment(it)
            } else {
                binding.resultLayout.visibility = View.GONE
            }
        }

        immunityViewModel.errorMessage.observe(viewLifecycleOwner) {
            it?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updatePieChart(gapPercentage: Float) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(gapPercentage, "Gap"))
        entries.add(PieEntry(100f - gapPercentage, "Immune"))

        val dataSet = PieDataSet(entries, "Immunity Gap")
        dataSet.colors = listOf(resources.getColor(R.color.sacred_orange, null), resources.getColor(R.color.bioluminescent_green, null))
        dataSet.valueTextColor = resources.getColor(R.color.white, null)
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)
        binding.immunityPieChart.data = data
        binding.immunityPieChart.description.isEnabled = false
        binding.immunityPieChart.setCenterText("${gapPercentage.toInt()}%")
        binding.immunityPieChart.setCenterTextColor(resources.getColor(R.color.white, null))
        binding.immunityPieChart.setCenterTextSize(24f)
        binding.immunityPieChart.legend.isEnabled = false
        binding.immunityPieChart.setUsePercentValues(false)
        binding.immunityPieChart.invalidate()
    }

    private fun updateRiskAssessment(gapPercentage: Float) {
        val riskLevel: String
        val outbreakProbability: String
        val fmdRisk: String
        val lsdRisk: String
        val mastitisRisk: String

        when {
            gapPercentage > 60 -> {
                riskLevel = "HIGH 🔴"
                outbreakProbability = "Outbreak probability in next 21 days: 78%"
                fmdRisk = "• FMD Risk: HIGH 🔴"
                lsdRisk = "• LSD Risk: MEDIUM 🟡"
                mastitisRisk = "• Mastitis Risk: LOW 🟢"
            }
            gapPercentage > 30 -> {
                riskLevel = "MEDIUM 🟡"
                outbreakProbability = "Outbreak probability in next 21 days: 45%"
                fmdRisk = "• FMD Risk: MEDIUM 🟡"
                lsdRisk = "• LSD Risk: LOW 🟢"
                mastitisRisk = "• Mastitis Risk: LOW 🟢"
            }
            else -> {
                riskLevel = "LOW 🟢"
                outbreakProbability = "Outbreak probability in next 21 days: 15%"
                fmdRisk = "• FMD Risk: LOW 🟢"
                lsdRisk = "• LSD Risk: LOW 🟢"
                mastitisRisk = "• Mastitis Risk: LOW 🟢"
            }
        }

        binding.riskLevelTextView.text = "Risk Level: $riskLevel"
        binding.outbreakProbabilityTextView.text = outbreakProbability
        binding.fmdRiskTextView.text = fmdRisk
        binding.lsdRiskTextView.text = lsdRisk
        binding.mastitisRiskTextView.text = mastitisRisk

        // TODO: Populate action recommendations dynamically
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
