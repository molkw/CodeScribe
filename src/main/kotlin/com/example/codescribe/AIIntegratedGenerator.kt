// src/main/kotlin/com/example/codescribe/AIIntegratedGenerator.kt
// CREATE this new file - this will be your main generator

package com.example.codescribe

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class AIIntegratedGenerator(private val project: Project) {
    private val originalGenerator = DocumentationGenerator(project)
    private val aiService = AIDocumentationService()
    private val projectConverter = ProjectToJsonConverter(project)
    private val logger = Logger.getInstance(AIIntegratedGenerator::class.java)

    /**
     * Generate documentation using AI backend + static analysis
     */
    fun generateDocumentation(): String {
        logger.info("🚀 Starting AI-integrated documentation generation")

        // Check AI service availability
        val aiStatus = aiService.getServiceStatus()
        val isAIAvailable = aiService.isServiceAvailable()

        logger.info("🔍 AI Status: $aiStatus")

        if (!isAIAvailable) {
            logger.info("⚠️ AI service unavailable, falling back to static analysis")
            return generateFallbackDocumentation(aiStatus)
        }

        return try {
            generateAIEnhancedDocumentation()
        } catch (e: Exception) {
            logger.warn("❌ AI analysis failed: ${e.message}", e)
            generateFallbackDocumentation("AI analysis failed: ${e.message}")
        }
    }

    /**
     * Generate AI-enhanced documentation
     */
    private fun generateAIEnhancedDocumentation(): String {
        logger.info("🤖 Starting AI-enhanced analysis")

        // Convert project to JSON format
        val classes = projectConverter.convertProjectToClassInfo()

        if (classes.isEmpty()) {
            logger.warn("📭 No classes found for analysis")
            return generateEmptyProjectResponse()
        }

        logger.info("📊 Sending ${classes.size} classes to AI backend")

        // Get AI analysis
        val aiResponse = aiService.generateAIDocumentation(
                projectName = project.name ?: "Spring Boot Project",
                classes = classes
        )

        return if (aiResponse != null) {
            logger.info("✅ AI analysis successful!")
            buildAIEnhancedDocument(aiResponse, classes)
        } else {
            logger.warn("❌ AI analysis returned null response")
            generateFallbackDocumentation("AI backend returned no response")
        }
    }

    /**
     * Build the final AI-enhanced documentation
     */
    private fun buildAIEnhancedDocument(
            aiResponse: AIDocumentationResponse,
            classes: List<ClassInfo>
    ): String {
        val builder = StringBuilder()

        // Header
        builder.append("# 🤖 AI-Enhanced Project Analysis: ${project.name}\n\n")
        builder.append("Generated using qwen2.5-coder AI model + Advanced Static Analysis\n")
        builder.append("Analysis Date: ${java.time.LocalDateTime.now()}\n")
        builder.append("Classes Analyzed: ${classes.size}\n\n")

        // AI Analysis Section
        builder.append("## 🤖 AI Analysis\n")
        builder.append(aiResponse.documentation)
        builder.append("\n\n")

        // AI Insights
        if (aiResponse.insights.isNotEmpty()) {
            builder.append("## 💡 Key Insights\n")
            aiResponse.insights.forEach { insight ->
                builder.append("- $insight\n")
            }
            builder.append("\n")
        }

        // AI Suggestions
        if (aiResponse.suggestions.isNotEmpty()) {
            builder.append("## 🎯 Recommendations\n")
            aiResponse.suggestions.forEach { suggestion ->
                builder.append("- $suggestion\n")
            }
            builder.append("\n")
        }

        // Architectural Patterns
        if (aiResponse.architecturalPatterns.isNotEmpty()) {
            builder.append("## 🏗️ Architecture Patterns\n")
            aiResponse.architecturalPatterns.forEach { pattern ->
                builder.append("- $pattern\n")
            }
            builder.append("\n")
        }

        // Project Statistics
        builder.append("## 📊 Project Statistics\n")
        builder.append(generateProjectStatistics(classes))
        builder.append("\n")

        // Optional: Include detailed static analysis
        if (shouldIncludeDetailedAnalysis(classes)) {
            builder.append("---\n\n")
            builder.append("## 📋 Detailed Static Analysis\n")
            builder.append("*Comprehensive class-by-class analysis using pattern detection*\n\n")
            builder.append(originalGenerator.generateProjectDocumentation())
        }

        return builder.toString()
    }

    /**
     * Generate fallback documentation when AI is unavailable
     */
    private fun generateFallbackDocumentation(reason: String): String {
        val builder = StringBuilder()

        builder.append("# 📊 Project Analysis: ${project.name}\n\n")
        builder.append("Analysis Date: ${java.time.LocalDateTime.now()}\n\n")

        builder.append("## ⚠️ AI Analysis Unavailable\n")
        builder.append("Reason: $reason\n\n")
        builder.append("Using advanced static analysis instead.\n\n")

        // Project overview
        val classes = projectConverter.convertProjectToClassInfo()
        if (classes.isNotEmpty()) {
            builder.append("## 📊 Project Overview\n")
            builder.append(generateProjectStatistics(classes))
            builder.append("\n")
        }

        builder.append("## 🔧 To Enable AI Analysis\n")
        builder.append("1. Start AI backend: `python main.py` in codescribe-backend folder\n")
        builder.append("2. Verify Ollama is running: `ollama serve`\n")
        builder.append("3. Check model: `ollama list` should show qwen2.5-coder:7b-instruct\n")
        builder.append("4. Test backend: `curl http://localhost:8000/api/v1/health`\n\n")

        // Include detailed static analysis
        builder.append("---\n\n")
        builder.append("## 📋 Static Analysis\n")
        builder.append(originalGenerator.generateProjectDocumentation())

        return builder.toString()
    }

    /**
     * Generate empty project response
     */
    private fun generateEmptyProjectResponse(): String {
        return """
            # 📭 Empty Project Analysis: ${project.name}
            
            ## No Java Classes Found
            
            This could mean:
            - No Java source files in the project
            - Project not fully loaded/indexed
            - Source files in unexpected locations
            
            ## Suggestions
            - Ensure project is a Java/Spring Boot project
            - Check that source files are in `src/main/java`
            - Wait for IntelliJ indexing to complete
            - Try refreshing the project structure
        """.trimIndent()
    }

    /**
     * Generate project statistics summary
     */
    private fun generateProjectStatistics(classes: List<ClassInfo>): String {
        val controllers = classes.count { it.annotations.any { ann -> ann.contains("Controller") } }
        val services = classes.count { it.annotations.any { ann -> ann.contains("Service") } }
        val repositories = classes.count { it.annotations.any { ann -> ann.contains("Repository") } }
        val entities = classes.count { it.annotations.any { ann -> ann.contains("Entity") } }

        val totalMethods = classes.sumOf { it.methods.size }
        val totalFields = classes.sumOf { it.fields.size }

        val avgMethodsPerClass = if (classes.isNotEmpty()) totalMethods.toDouble() / classes.size else 0.0
        val avgComplexity = if (totalMethods > 0) {
            classes.sumOf { cls -> cls.methods.sumOf { it.complexity } }.toDouble() / totalMethods
        } else 0.0

        return """
            **Component Breakdown:**
            - Controllers: $controllers (REST endpoints)
            - Services: $services (Business logic)
            - Repositories: $repositories (Data access)
            - Entities: $entities (Data models)
            - Other Classes: ${classes.size - controllers - services - repositories - entities}
            
            **Code Metrics:**
            - Total Classes: ${classes.size}
            - Total Methods: $totalMethods
            - Total Fields: $totalFields
            - Avg Methods/Class: ${"%.1f".format(avgMethodsPerClass)}
            - Avg Method Complexity: ${"%.1f".format(avgComplexity)}
            
            **Architecture Assessment:**
            ${determineArchitectureType(controllers, services, repositories)}
        """.trimIndent()
    }

    /**
     * Determine architecture type based on component analysis
     */
    private fun determineArchitectureType(controllers: Int, services: Int, repositories: Int): String {
        return when {
            controllers > 0 && services > 0 && repositories > 0 ->
                "✅ **Full MVC Architecture** - Well-structured with clear separation of concerns"
            controllers > 0 && services > 0 ->
                "✅ **Web Application** - Good controller/service separation"
            controllers > 0 ->
                "⚡ **REST API** - Controller-focused architecture"
            services > 0 ->
                "🔧 **Service Application** - Business logic focused"
            else ->
                "📝 **Basic Application** - Simple structure"
        }
    }

    /**
     * Decide whether to include detailed static analysis
     */
    private fun shouldIncludeDetailedAnalysis(classes: List<ClassInfo>): Boolean {
        // Include detailed analysis for smaller projects or when requested
        return classes.size <= 20 // Don't overwhelm for large projects
    }

    /**
     * Get quick project summary for UI display
     */
    fun getProjectSummary(): String {
        val classes = projectConverter.convertProjectToClassInfo()
        return if (classes.isNotEmpty()) {
            "📊 ${classes.size} classes ready for AI analysis"
        } else {
            "📭 No Java classes found"
        }
    }

    /**
     * Test AI connectivity
     */
    fun testAIConnection(): Boolean {
        return aiService.testConnection()
    }
}