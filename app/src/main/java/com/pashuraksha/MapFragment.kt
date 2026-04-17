package com.pashuraksha

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pashuraksha.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.cos
import kotlin.math.sin

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var sirModel: SIRModel? = null
    private var outbreakLocation: GeoPoint? = null
    private var simulationHandler = Handler(Looper.getMainLooper())
    private var simulationRunnable: Runnable? = null
    private var simulationSpeed = 1 // 1x, 2x, 5x
    private var isSimulationRunning = false
    private var currentSimulationDay = 0
    private var affectedFarmsCount = 0

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE // Required for OSMDroid tile cache
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle permissions
        requestPermissionsIfNecessary(REQUIRED_PERMISSIONS)

        // Setup OSMDroid configuration
        val ctx = requireContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(12.0)
        map.controller.setCenter(GeoPoint(19.0760, 72.8777)) // Default to Mumbai

        // My Location Overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        // Map tap listener for reporting disease cases
        map.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(event: android.view.MotionEvent, mapView: MapView): Boolean {
                outbreakLocation = mapView.getProjection().fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                Toast.makeText(ctx, "Outbreak reported at: ${outbreakLocation?.latitude}, ${outbreakLocation?.longitude}", Toast.LENGTH_SHORT).show()
                startSimulation()
                return true
            }
        })

        setupSimulationControls()
        setupDataOverlays()
    }

    private fun setupSimulationControls() {
        binding.playPauseButton.setOnClickListener { toggleSimulation() }

        binding.speedSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                simulationSpeed = progress
                binding.speedTextView.text = "${simulationSpeed}x"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val diseaseTypes = arrayOf("FMD", "LSD", "Mastitis")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, diseaseTypes)
        binding.diseaseSpinner.adapter = adapter
        binding.diseaseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle disease selection, potentially adjust SIR model parameters
                Toast.makeText(requireContext(), "Selected: ${diseaseTypes[position]}", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDataOverlays() {
        // These will be implemented as custom overlays or markers on the map
        binding.windDirectionToggle.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Wind Direction: $isChecked", Toast.LENGTH_SHORT).show()
            // TODO: Add/remove wind direction overlay
        }
        binding.cattleDensityToggle.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Cattle Density: $isChecked", Toast.LENGTH_SHORT).show()
            // TODO: Add/remove cattle density heatmap overlay
        }
        binding.vaccinationCoverageToggle.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Vaccination Coverage: $isChecked", Toast.LENGTH_SHORT).show()
            // TODO: Add/remove vaccination coverage layer
        }
        binding.vetClinicToggle.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Vet Clinics: $isChecked", Toast.LENGTH_SHORT).show()
            // TODO: Add/remove vet clinic markers
        }
    }

    private fun startSimulation() {
        if (outbreakLocation == null) {
            Toast.makeText(requireContext(), "Please tap on the map to report an outbreak location first.", Toast.LENGTH_LONG).show()
            return
        }
        // Initialize SIR model with dummy values for now
        sirModel = SIRModel(population = 1000, initialInfected = 1, beta = 0.3, gamma = 0.1)
        currentSimulationDay = 0
        affectedFarmsCount = 0
        binding.dayCounterTextView.text = "Day 0 - 0 farms affected"
        isSimulationRunning = true
        binding.playPauseButton.text = "Pause"
        runSimulationStep()
    }

    private fun toggleSimulation() {
        isSimulationRunning = !isSimulationRunning
        if (isSimulationRunning) {
            binding.playPauseButton.text = "Pause"
            runSimulationStep()
        } else {
            binding.playPauseButton.text = "Play"
            simulationRunnable?.let { simulationHandler.removeCallbacks(it) }
        }
    }

    private fun runSimulationStep() {
        simulationRunnable = Runnable { 
            if (isSimulationRunning) {
                sirModel?.step()
                currentSimulationDay = sirModel?.getCurrentDay() ?: 0
                affectedFarmsCount = sirModel?.getCurrentState()?.infected ?: 0 // Simplified for now
                binding.dayCounterTextView.text = "Day $currentSimulationDay - $affectedFarmsCount farms affected"

                // Update map overlay for spread visualization
                map.overlays.add(SpreadOverlay(outbreakLocation!!, sirModel?.getSpreadRadius(currentSimulationDay, 50.0) ?: 0.0))
                map.invalidate()

                simulationHandler.postDelayed(simulationRunnable!!, (1000L / simulationSpeed))
            }
        }
        simulationHandler.postDelayed(simulationRunnable!!, (1000L / simulationSpeed))
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        myLocationOverlay.disableMyLocation()
        myLocationOverlay.disableFollowLocation()
        simulationRunnable?.let { simulationHandler.removeCallbacks(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        for (i in grantResults.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permissions[i])
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        } else {
            // Permissions granted, proceed with map setup if not already done
            if (!::map.isInitialized) {
                // This might happen if permission was granted after onViewCreated
                // Re-initialize map if needed, or ensure startCamera() is called
            }
        }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    // Custom Overlay for drawing spread radius
    inner class SpreadOverlay(private val center: GeoPoint, private var radiusKm: Double) : Overlay() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.FILL
            alpha = 70 // Semi-transparent
        }

        fun updateRadius(newRadiusKm: Double) {
            radiusKm = newRadiusKm
            map.invalidate()
        }

        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            if (shadow) return

            val projection = mapView.projection
            val pixelCenter = projection.toPixels(center, null)

            // Convert km to pixels (approximate, depends on zoom level)
            val groundResolution = org.osmdroid.util.TileSystem.GroundResolution(
                center.latitude, mapView.zoomLevelDouble
            )
            val radiusPixels = (radiusKm * 1000 / groundResolution).toFloat()

            canvas.drawCircle(pixelCenter.x.toFloat(), pixelCenter.y.toFloat(), radiusPixels, paint)
        }
    }
}
