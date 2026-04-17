package com.pashuraksha

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel : ViewModel() {

    private val _greeting = MutableLiveData<String>().apply {
        value = "नमस्ते किसान 🙏"
    }
    val greeting: LiveData<String> = _greeting

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> = _currentTime

    private val _currentDate = MutableLiveData<String>()
    val currentDate: LiveData<String> = _currentDate

    private val _villageName = MutableLiveData<String>().apply {
        value = "Sample Village"
    }
    val villageName: LiveData<String> = _villageName

    private val _weather = MutableLiveData<String>().apply {
        value = "25°C, Sunny"
    }
    val weather: LiveData<String> = _weather

    init {
        updateDateTime()
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        _currentTime.value = timeFormat.format(calendar.time)
        _currentDate.value = dateFormat.format(calendar.time)
    }

    // TODO: Implement actual live clock and date updates, and OpenWeatherMap API integration
}
