package com.pashuraksha

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import android.util.Log
import retrofit2.http.Part
import java.io.ByteArrayOutputStream

data class DiagnosisResult(
    val disease: String,
    val confidence: Int,
    @SerializedName("symptoms_visible") val symptomsVisible: String,
    val recommendations: String,
    @SerializedName("urgency_level") val urgencyLevel: String
)

interface GeminiApiService {
    @Multipart
        @POST("v1/models/gemini-pro-vision:generateContent")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part,
        @Part("prompt") prompt: String
    ): GeminiApiResponse

    companion object {
        const val API_KEY = "sk-GZwWUzEZeAatZWcfauigB8" // Placeholder, will be replaced by environment variable
    }
}

data class GeminiApiResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: Content
)

data class Content(
    val parts: List<ContentPart>
)

data class ContentPart(
    val text: String
)

class DiseaseDetectionViewModel : ViewModel() {

    private val _diagnosisResult = MutableLiveData<DiagnosisResult?>()
    val diagnosisResult: LiveData<DiagnosisResult?> = _diagnosisResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val geminiApiService: GeminiApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/") // Gemini API base URL
            .client(okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("x-goog-api-key", GeminiApiService.API_KEY)
                val request = requestBuilder.build()
                chain.proceed(request)
            }.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        geminiApiService = retrofit.create(GeminiApiService::class.java)
    }

    fun analyzeImageWithGemini(imageBitmap: Bitmap) {
        _errorMessage.value = null
        _diagnosisResult.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val imageBytes = outputStream.toByteArray()

                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", "image.jpg", requestBody)

                val prompt = "You are a veterinary AI. Analyze this image of a cow/buffalo. Detect signs of: Lumpy Skin Disease, FMD, Mastitis. Return JSON: {\"disease\": \"<disease_name>\", \"confidence\": <confidence_percentage>, \"symptoms_visible\": \"<symptoms>\", \"recommendations\": \"<recommendations>\", \"urgency_level\": \"<urgency>\"}"

                val response = geminiApiService.analyzeImage(imagePart, prompt)

                val jsonResponse = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                jsonResponse?.let {
                    val diagnosis = Gson().fromJson(it, DiagnosisResult::class.java)
                    _diagnosisResult.postValue(diagnosis)
                } ?: run {
                    _errorMessage.postValue("Failed to parse Gemini API response.")
                }

            } catch (e: Exception) {
                _errorMessage.postValue("Error analyzing image: ${e.message}")
            }
        }
    }
}
