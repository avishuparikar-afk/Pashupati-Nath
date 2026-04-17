package com.pashuraksha

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

class ImmunityViewModel : ViewModel() {

    private val _immunityGapPercentage = MutableLiveData<Float?>()
    val immunityGapPercentage: LiveData<Float?> = _immunityGapPercentage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun calculateImmunityGap(villageName: String, district: String, totalCattle: Int, cattleType: String) {
        _errorMessage.value = null // Clear previous errors
        viewModelScope.launch {
            try {
                // Simulate API call or local dataset calculation
                val vaccinatedCount = simulateVaccinatedCount(villageName, district, totalCattle, cattleType)
                val gapPercentage = ((totalCattle - vaccinatedCount).toFloat() / totalCattle) * 100
                _immunityGapPercentage.value = gapPercentage
            } catch (e: Exception) {
                _errorMessage.value = "Error calculating immunity gap: ${e.message}"
                _immunityGapPercentage.value = null
            }
        }
    }

    private suspend fun simulateVaccinatedCount(villageName: String, district: String, totalCattle: Int, cattleType: String): Int = withContext(Dispatchers.IO) {
        // This is a placeholder for actual API call (e.g., eGoPalan API)
        // For now, we'll return a simulated value based on some factors
        val random = Random()
        val baseVaccinated = (totalCattle * 0.6).toInt() // 60% base vaccination
        val variance = totalCattle / 5 // +/- 20% variance

        var vaccinated = baseVaccinated + random.nextInt(variance * 2) - variance

        // Adjust based on cattle type (example logic)
        vaccinated = when (cattleType) {
            "Cow" -> (vaccinated * 1.1).toInt().coerceAtMost(totalCattle)
            "Buffalo" -> (vaccinated * 0.9).toInt().coerceAtMost(totalCattle)
            else -> vaccinated.coerceAtMost(totalCattle)
        }

        // Adjust based on village/district (example logic)
        if (district == "Pune") vaccinated = (vaccinated * 1.2).toInt().coerceAtMost(totalCattle)
        if (villageName.contains("highrisk", ignoreCase = true)) vaccinated = (vaccinated * 0.7).toInt().coerceAtMost(totalCattle)

        vaccinated.coerceAtLeast(0)
    }
}
