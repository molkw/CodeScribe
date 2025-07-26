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

    // Backend URLs - primary (localhost) and fallback (forwarded)
    private val primaryBaseUrl = "http://localhost:8000/api/v1"
    private val fallbackBaseUrl = "https://sk0nn89v-8000.uks1.devtunnels.ms/api/v1"

    // Track which backend is currently working
    private var activeBaseUrl: String? = null
    private var lastHealthCheckTime = 0L
    private val healthCheckCacheMs = 30000L // Cache health check for 30 seconds

    /**
     * Determine which backend URL to use by checking availability
     */
    private fun getAvailableBaseUrl(): String? {
        val currentTime = System.currentTimeMillis()

        // If we have a cached active URL and it's still fresh, use it
        if (activeBaseUrl != null && (currentTime - lastHealthCheckTime) < healthCheckCacheMs) {
            return activeBaseUrl
        }

        // Test primary URL first
        logger.debug("🔍 Testing primary backend: $primaryBaseUrl")
        if (testBackendConnection(primaryBaseUrl)) {
            logger.info("✅ Using local backend: $primaryBaseUrl")
            activeBaseUrl = primaryBaseUrl
            lastHealthCheckTime = currentTime
            return primaryBaseUrl
        }

        // If primary fails, test fallback URL
        logger.debug("🔍 Primary backend unavailable, testing fallback: $fallbackBaseUrl")
        if (testBackendConnection(fallbackBaseUrl)) {
            logger.info("✅ Using forwarded backend: $fallbackBaseUrl")
            activeBaseUrl = fallbackBaseUrl
            lastHealthCheckTime = currentTime
            return fallbackBaseUrl
        }

        // Both backends are unavailable
        logger.warn("❌ Both backends are unavailable")
        activeBaseUrl = null
        lastHealthCheckTime = currentTime
        return null
    }

    /**
     * Test if a specific backend URL is available
     */
    private fun testBackendConnection(baseUrl: String): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            response.statusCode() == 200
        } catch (e: Exception) {
            logger.debug("❌ Backend $baseUrl unavailable: ${e.message}")
            false
        }
    }

    /**
     * Send code analysis request to AI backend with automatic fallback
     */
    fun generateAIDocumentation(
            projectName: String,
            classes: List<ClassInfo>
    ): AIDocumentationResponse? {
        val baseUrl = getAvailableBaseUrl()

        if (baseUrl == null) {
            logger.warn("❌ No backend available for AI documentation generation")
            return null
        }

        return try {
            logger.info("🚀 Sending ${classes.size} classes to AI backend: $baseUrl")

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
                    // If this backend failed, invalidate the cache to force re-check next time
                    activeBaseUrl = null
                    lastHealthCheckTime = 0L
                    null
                }
            }
        } catch (e: Exception) {
            logger.warn("❌ Failed to connect to AI backend ($baseUrl): ${e.message}", e)
            // If this backend failed, invalidate the cache to force re-check next time
            activeBaseUrl = null
            lastHealthCheckTime = 0L
            null
        }
    }

    /**
     * Check if AI backend is available (tries both URLs)
     */
    fun isServiceAvailable(): Boolean {
        return getAvailableBaseUrl() != null
    }

    /**
     * Get detailed service status for UI display
     */
    fun getServiceStatus(): String {
        val baseUrl = getAvailableBaseUrl()

        return when (baseUrl) {
            null -> "❌ AI Backend: Offline (both local and remote)"
            primaryBaseUrl -> getDetailedStatus(baseUrl, "Local")
            fallbackBaseUrl -> getDetailedStatus(baseUrl, "Remote")
            else -> "❓ AI Backend: Unknown state"
        }
    }

    /**
     * Get detailed status from a specific backend
     */
    private fun getDetailedStatus(baseUrl: String, location: String): String {
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
                        health.codeOptimized -> "🤖 AI Backend ($location): Online with qwen2.5-coder"
                        health.ollamaAvailable -> "⚡ AI Backend ($location): Online (basic model)"
                        else -> "⚠️ AI Backend ($location): Online but Ollama unavailable"
                    }
                }
                else -> "❌ AI Backend ($location): Error (${response.statusCode()})"
            }
        } catch (e: Exception) {
            "❌ AI Backend ($location): Connection failed"
        }
    }

    /**
     * Test connection with simple ping (tries both URLs)
     */
    fun testConnection(): Boolean {
        return isServiceAvailable()
    }

    /**
     * Force refresh of backend availability (useful for manual retry)
     */
    fun refreshBackendStatus() {
        logger.info("🔄 Forcing backend status refresh...")
        activeBaseUrl = null
        lastHealthCheckTime = 0L
        getAvailableBaseUrl()
    }

    /**
     * Get which backend is currently active (for debugging/UI)
     */
    fun getActiveBackend(): String? {
        return when (activeBaseUrl) {
            primaryBaseUrl -> "Local (localhost:8000)"
            fallbackBaseUrl -> "Remote (devtunnels.ms)"
            else -> null
        }
    }
}