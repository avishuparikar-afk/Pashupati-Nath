package com.pashuraksha

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.pashuraksha.databinding.FragmentSettingsBinding
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languages = arrayOf("English", "Hindi", "Marathi", "Telugu", "Punjabi")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.languageSpinner.adapter = adapter

        // Set current language selection
        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            resources.configuration.locale
        }
        val currentLang = when (currentLocale.language) {
            "hi" -> "Hindi"
            "mr" -> "Marathi"
            "te" -> "Telugu"
            "pa" -> "Punjabi"
            else -> "English"
        }
        binding.languageSpinner.setSelection(languages.indexOf(currentLang))

        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                val locale = when (selectedLanguage) {
                    "Hindi" -> Locale("hi")
                    "Marathi" -> Locale("mr")
                    "Telugu" -> Locale("te")
                    "Punjabi" -> Locale("pa")
                    else -> Locale("en")
                }
                setLocale(requireContext(), locale)
                activity?.recreate() // Recreate activity to apply language change
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setLocale(context: Context, locale: Locale) {
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
