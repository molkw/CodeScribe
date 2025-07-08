// src/main/kotlin/com/example/codescribe/AIDocumentationService.kt
// REPLACE your existing AIDocumentationService.kt with this

package com.example.codescribe

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration

// Data classes that exactly match your FastAPI backend
data class CodeAnalysisRequest(
        @SerializedName("project_name")
        val projectName: String,
        @SerializedName("classes")
        val classes: List<ClassInfo>,
        @SerializedName("analysis_type")
        val analysisType: String = "full"
)

data class ClassInfo(
        @SerializedName("name")
        val name: String,
        @SerializedName("package_name")
        val packageName: String,
        @SerializedName("source_code")
        val sourceCode: String,
        @SerializedName("annotations")
        val annotations: List<String>,
        @SerializedName("methods")
        val methods: List<MethodInfo>,
        @SerializedName("fields")
        val fields: List<FieldInfo>
)

data class MethodInfo(
        @SerializedName("name")
        val name: String,
        @SerializedName("signature")
        val signature: String,
        @SerializedName("annotations")
        val annotations: List<String>,
        @SerializedName("complexity")
        val complexity: Int
)

data class FieldInfo(
        @SerializedName("name")
        val name: String,
        @SerializedName("type")
        val type: String,
        @SerializedName("annotations")
        val annotations: List<String>
)

data class AIDocumentationResponse(
        @SerializedName("documentation")
        val documentation: String,
        @SerializedName("insights")
        val insights: List<String>,
        @SerializedName("suggestions")
        val suggestions: List<String>,
        @SerializedName("architectural_patterns")
        val architecturalPatterns: List<String>
)

data class HealthResponse(
        @SerializedName("status")
        val status: String,
        @SerializedName("ollama_available")
        val ollamaAvailable: Boolean,
        @SerializedName("active_model")
        val activeModel: String?,
        @SerializedName("code_optimized")
        val codeOptimized: Boolean
)

class AIDocumentationService {
    private val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    private val gson = Gson()
    private val logger = Logger.getInstance(AIDocumentationService::class.java)

    // Backend URL - change if your backend runs on different port
    private val baseUrl = "http://localhost:8000/api/v1"

    /**
     * Send code analysis request to AI backend
     */
    fun generateAIDocumentation(
            projectName: String,
            classes: List<ClassInfo>
    ): AIDocumentationResponse? {
        return try {
            logger.info("🚀 Sending ${classes.size} classes to AI backend for analysis")

            val request = CodeAnalysisRequest(
                    projectName = projectName,
                    classes = classes,
                    analysisType = "full"
            )

            val requestJson = gson.toJson(request)
            logger.debug("📊 Request payload size: ${requestJson.length} characters")

            val httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/analyze-code"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(3)) // Give AI time to analyze
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build()

            val response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            )

            logger.info("📡 AI backend responded with status: ${response.statusCode()}")

            when (response.statusCode()) {
                200 -> {
                    val aiResponse = gson.fromJson(response.body(), AIDocumentationResponse::class.java)
                    logger.info("✅ AI analysis successful!")
                    aiResponse
                }
                else -> {
                    logger.warn("❌ AI backend error: ${response.statusCode()} - ${response.body()}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.warn("❌ Failed to connect to AI backend: ${e.message}", e)
            null
        }
    }

    /**
     * Check if AI backend is available and healthy
     */
    fun isServiceAvailable(): Boolean {
        return try {
            logger.debug("🔍 Checking AI service availability...")

            val request = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val isAvailable = response.statusCode() == 200

            logger.debug("📊 AI service available: $isAvailable")
            isAvailable
        } catch (e: Exception) {
            logger.debug("❌ AI service check failed: ${e.message}")
            false
        }
    }

    /**
     * Get detailed service status for UI display
     */
    fun getServiceStatus(): String {
        return try {
            val request = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            when (response.statusCode()) {
                200 -> {
                    val health = gson.fromJson(response.body(), HealthResponse::class.java)
                    when {
                        health.codeOptimized -> "🤖 AI Backend: Online with qwen2.5-coder"
                        health.ollamaAvailable -> "⚡ AI Backend: Online (basic model)"
                        else -> "⚠️ AI Backend: Online but Ollama unavailable"
                    }
                }
                else -> "❌ AI Backend: Error (${response.statusCode()})"
            }
        } catch (e: Exception) {
            "❌ AI Backend: Offline"
        }
    }

    /**
     * Test connection with simple ping
     */
    fun testConnection(): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }
}