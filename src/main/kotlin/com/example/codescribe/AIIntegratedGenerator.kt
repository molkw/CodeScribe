// src/main/kotlin/com/example/codescribe/AIIntegratedGenerator.kt
// Updated version with better content distribution logic

package com.example.codescribe

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class AIIntegratedGenerator(private val project: Project) {
    private val originalGenerator = DocumentationGenerator(project)
    private val aiService = AIDocumentationService()
    private val projectConverter = ProjectToJsonConverter(project)
    private val logger = Logger.getInstance(AIIntegratedGenerator::class.java)

    /**
     * Enhanced documentation with proper content structure
     */
    fun generateDocumentation(): DocumentationResult {
        logger.info("🚀 Starting enhanced AI-integrated documentation generation")

        val aiStatus = aiService.getServiceStatus()
        val isAIAvailable = aiService.isServiceAvailable()

        logger.info("🔍 AI Status: $aiStatus")

        val classes = projectConverter.convertProjectToClassInfo()

        if (classes.isEmpty()) {
            return DocumentationResult.empty(project.name ?: "Unknown Project")
        }

        return if (isAIAvailable) {
            try {
                generateEnhancedDocumentation(classes)
            } catch (e: Exception) {
                logger.warn("❌ AI analysis failed: ${e.message}", e)
                generateStaticDocumentation(classes, "AI analysis failed: ${e.message}")
            }
        } else {
            generateStaticDocumentation(classes, aiStatus)
        }
    }

    /**
     * Generate AI-enhanced documentation with proper structure
     */
    private fun generateEnhancedDocumentation(classes: List<ClassInfo>): DocumentationResult {
        logger.info("🤖 Starting AI-enhanced analysis with ${classes.size} classes")

        val aiResponse = aiService.generateAIDocumentation(
                projectName = project.name ?: "Spring Boot Project",
                classes = classes
        )

        return if (aiResponse != null) {
            logger.info("✅ AI analysis successful!")
            DocumentationResult.fromAIResponse(aiResponse, classes, project.name ?: "Unknown")
        } else {
            logger.warn("❌ AI analysis returned null response")
            generateStaticDocumentation(classes, "AI backend returned no response")
        }
    }

    /**
     * Generate static documentation fallback
     */
    private fun generateStaticDocumentation(classes: List<ClassInfo>, reason: String): DocumentationResult {
        logger.info("📊 Generating static documentation fallback")

        return DocumentationResult.fromStaticAnalysis(classes, project.name ?: "Unknown", reason)
    }

    fun getProjectSummary(): String {
        val classes = projectConverter.convertProjectToClassInfo()
        return if (classes.isNotEmpty()) {
            "📊 ${classes.size} classes ready for analysis"
        } else {
            "📭 No Java classes found"
        }
    }

    fun testAIConnection(): Boolean {
        return aiService.testConnection()
    }
}

/**
 * Structured documentation result that separates content properly
 */
data class DocumentationResult(
        val overview: String,
        val classDetails: String,
        val patterns: String,
        val relationships: String,
        val insights: String,
        val isAIEnhanced: Boolean
) {
    companion object {
        fun fromAIResponse(
                aiResponse: AIDocumentationResponse,
                classes: List<ClassInfo>,
                projectName: String
        ): DocumentationResult {

            val stats = ProjectStats.from(classes)
            val timestamp = java.time.LocalDateTime.now()

            // OVERVIEW - Project summary + AI overview
            val overview = """
🤖 AI-ENHANCED ANALYSIS COMPLETE!
Project: $projectName
Analysis Date: $timestamp
Classes Analyzed: ${classes.size}

${aiResponse.documentation}

📊 QUICK STATS
• Controllers: ${stats.controllers} (REST endpoints)
• Services: ${stats.services} (Business logic)  
• Repositories: ${stats.repositories} (Data access)
• Entities: ${stats.entities} (Data models)
• Other Classes: ${stats.others}
• Total Methods: ${stats.totalMethods}
• Avg Complexity: ${"%.1f".format(stats.avgComplexity)}

${determineArchitectureType(stats)}
            """.trimIndent()

            // CLASS DETAILS - Focus on component breakdown
            val classDetails = """
🎯 COMPONENT ANALYSIS
${stats.componentBreakdown}

📈 CODE METRICS
• Total Classes: ${classes.size}
• Total Methods: ${stats.totalMethods}
• Total Fields: ${stats.totalFields}
• Average Methods per Class: ${"%.1f".format(stats.avgMethodsPerClass)}
• Average Method Complexity: ${"%.1f".format(stats.avgComplexity)}

🔍 CLASS DISTRIBUTION BY PACKAGE
${getPackageDistribution(classes)}

💡 COMPLEXITY ANALYSIS
• Low Complexity (1-2): ${stats.lowComplexityMethods} methods
• Medium Complexity (3-5): ${stats.mediumComplexityMethods} methods  
• High Complexity (6+): ${stats.highComplexityMethods} methods
            """.trimIndent()

            // PATTERNS - Architecture + detected patterns
            val patterns = """
🏗️ ARCHITECTURAL PATTERNS
${aiResponse.architecturalPatterns.joinToString("\n") { "✅ $it" }}

🔍 DESIGN PATTERNS DETECTED
${if (stats.controllers > 0) "• Controller Pattern - REST endpoint management" else ""}
${if (stats.services > 0) "• Service Layer Pattern - Business logic encapsulation" else ""}
${if (stats.repositories > 0) "• Repository Pattern - Data access abstraction" else ""}
${if (stats.entities > 0) "• Domain Model Pattern - Entity representation" else ""}
${if (hasDependencyInjection(classes)) "• Dependency Injection - IoC container usage" else ""}

🎯 ARCHITECTURE ASSESSMENT
${determineArchitectureQuality(stats)}
            """.trimIndent()

            // RELATIONSHIPS - Component interactions
            val relationships = """
🔗 COMPONENT RELATIONSHIPS

📊 DEPENDENCY FLOW
Controllers (${stats.controllers}) → Services (${stats.services}) → Repositories (${stats.repositories})

🔄 INTERACTION PATTERNS
• Web Layer: ${stats.controllers} controllers handle HTTP requests
• Business Layer: ${stats.services} services process business logic
• Data Layer: ${stats.repositories} repositories manage data access
• Model Layer: ${stats.entities} entities represent domain objects

🏗️ LAYERING QUALITY
${analyzeLayers(stats)}

📈 COUPLING ANALYSIS
• Controller-Service coupling: ${if (stats.services > 0) "Normal" else "Missing"}
• Service-Repository coupling: ${if (stats.repositories > 0) "Normal" else "Potential issue"}
• Entity usage: ${if (stats.entities > 0) "Well-modeled" else "Consider adding"}
            """.trimIndent()

            // INSIGHTS - AI insights + practical recommendations
            val insights = """
💡 AI INSIGHTS
${aiResponse.insights.joinToString("\n") { "• $it" }}

🎯 AI RECOMMENDATIONS  
${aiResponse.suggestions.joinToString("\n") { "• $it" }}

🔧 TECHNICAL RECOMMENDATIONS
${generateTechnicalRecommendations(stats)}

⚡ QUICK WINS
${generateQuickWins(stats)}

📋 NEXT STEPS
• Review high-complexity methods for refactoring opportunities
• Ensure proper test coverage for critical business logic
• Consider adding API documentation with Swagger/OpenAPI
• Implement proper error handling and logging
            """.trimIndent()

            return DocumentationResult(
                    overview = overview,
                    classDetails = classDetails,
                    patterns = patterns,
                    relationships = relationships,
                    insights = insights,
                    isAIEnhanced = true
            )
        }

        fun fromStaticAnalysis(
                classes: List<ClassInfo>,
                projectName: String,
                reason: String
        ): DocumentationResult {

            val stats = ProjectStats.from(classes)
            val timestamp = java.time.LocalDateTime.now()

            val overview = """
📊 STATIC ANALYSIS COMPLETE
Project: $projectName  
Analysis Date: $timestamp
Classes Analyzed: ${classes.size}

⚠️ AI Analysis Unavailable
Reason: $reason

📊 PROJECT OVERVIEW
Spring Boot application with ${classes.size} classes following standard conventions.

${stats.componentBreakdown}

${determineArchitectureType(stats)}
            """.trimIndent()

            return DocumentationResult(
                    overview = overview,
                    classDetails = "🔧 Enable AI backend for detailed class analysis\n\n${stats.componentBreakdown}",
                    patterns = "🔧 Enable AI backend for pattern detection\n\nBasic patterns: Spring Boot MVC",
                    relationships = "🔧 Enable AI backend for relationship analysis\n\nStandard Spring Boot layering detected",
                    insights = "🔧 Enable AI backend for detailed insights\n\nBasic structure analysis shows well-organized Spring Boot application",
                    isAIEnhanced = false
            )
        }

        fun empty(projectName: String): DocumentationResult {
            return DocumentationResult(
                    overview = "📭 No Java classes found in $projectName",
                    classDetails = "No classes to analyze",
                    patterns = "No patterns detected",
                    relationships = "No relationships found",
                    insights = "Ensure project contains Java source files",
                    isAIEnhanced = false
            )
        }

        // Helper functions for better analysis
        private fun determineArchitectureType(stats: ProjectStats): String {
            return when {
                stats.controllers > 0 && stats.services > 0 && stats.repositories > 0 ->
                    "✅ **Full MVC Architecture** - Complete separation of concerns"
                stats.controllers > 0 && stats.services > 0 ->
                    "✅ **Web Application** - Good controller/service separation"
                stats.controllers > 0 ->
                    "⚡ **REST API** - Controller-focused architecture"
                stats.services > 0 ->
                    "🔧 **Service Application** - Business logic focused"
                else ->
                    "📝 **Basic Application** - Simple structure"
            }
        }

        private fun determineArchitectureQuality(stats: ProjectStats): String {
            val score = when {
                stats.controllers > 0 && stats.services > 0 && stats.repositories > 0 -> 9
                stats.controllers > 0 && stats.services > 0 -> 7
                stats.controllers > 0 || stats.services > 0 -> 5
                else -> 3
            }

            return when {
                score >= 8 -> "🟢 Excellent - Well-structured layered architecture"
                score >= 6 -> "🟡 Good - Decent separation of concerns"
                score >= 4 -> "🟡 Fair - Basic structure present"
                else -> "🔴 Poor - Consider improving architecture"
            }
        }

        private fun analyzeLayers(stats: ProjectStats): String {
            val layers = mutableListOf<String>()
            if (stats.controllers > 0) layers.add("Web Layer ✅")
            if (stats.services > 0) layers.add("Business Layer ✅")
            if (stats.repositories > 0) layers.add("Data Layer ✅")
            else layers.add("Data Layer ⚠️ (Missing repositories)")
            if (stats.entities > 0) layers.add("Model Layer ✅")

            return layers.joinToString(" → ")
        }

        private fun getPackageDistribution(classes: List<ClassInfo>): String {
            val packageCount = classes.groupBy { it.packageName }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)

            return packageCount.joinToString("\n") { (pkg, count) ->
                "• ${pkg.ifEmpty { "(default)" }}: $count classes"
            }
        }

        private fun hasDependencyInjection(classes: List<ClassInfo>): Boolean {
            return classes.any { classInfo ->
                classInfo.annotations.any { it.contains("Component") || it.contains("Service") ||
                        it.contains("Repository") || it.contains("Controller") }
            }
        }

        private fun generateTechnicalRecommendations(stats: ProjectStats): String {
            val recommendations = mutableListOf<String>()

            if (stats.repositories == 0) {
                recommendations.add("• Consider adding Repository layer for better data access abstraction")
            }
            if (stats.avgComplexity > 3.0) {
                recommendations.add("• Review high-complexity methods for refactoring opportunities")
            }
            if (stats.controllers > 0 && stats.avgMethodsPerClass > 10) {
                recommendations.add("• Large classes detected - consider breaking into smaller components")
            }

            return recommendations.ifEmpty { listOf("• Code structure looks good!") }.joinToString("\n")
        }

        private fun generateQuickWins(stats: ProjectStats): String {
            val wins = mutableListOf<String>()

            if (stats.services > 0) wins.add("• Good service layer separation")
            if (stats.entities > 0) wins.add("• Well-defined domain models")
            if (stats.avgComplexity < 2.0) wins.add("• Low complexity methods - good maintainability")

            return wins.ifEmpty { listOf("• Focus on improving architecture") }.joinToString("\n")
        }
    }
}

/**
 * Helper class for project statistics
 */
data class ProjectStats(
        val controllers: Int,
        val services: Int,
        val repositories: Int,
        val entities: Int,
        val others: Int,
        val totalMethods: Int,
        val totalFields: Int,
        val avgMethodsPerClass: Double,
        val avgComplexity: Double,
        val lowComplexityMethods: Int,
        val mediumComplexityMethods: Int,
        val highComplexityMethods: Int,
        val componentBreakdown: String
) {
    companion object {
        fun from(classes: List<ClassInfo>): ProjectStats {
            val controllers = classes.count { it.annotations.any { ann -> ann.contains("Controller") } }
            val services = classes.count { it.annotations.any { ann -> ann.contains("Service") } }

            // Enhanced repository detection
            val repositories = classes.count { classInfo ->
                // Check for @Repository annotation
                classInfo.annotations.any { ann -> ann.contains("Repository") } ||
                        // Check for class/interface names ending with "Repository"
                        classInfo.name.endsWith("Repository", ignoreCase = true) ||
                        // Check for Spring Data JPA repository inheritance patterns
                        classInfo.annotations.any { ann -> ann.contains("JpaRepository") || ann.contains("CrudRepository") }
            }

            val entities = classes.count { it.annotations.any { ann -> ann.contains("Entity") } }
            val others = classes.size - controllers - services - repositories - entities

            val totalMethods = classes.sumOf { it.methods.size }
            val totalFields = classes.sumOf { it.fields.size }

            val avgMethodsPerClass = if (classes.isNotEmpty()) totalMethods.toDouble() / classes.size else 0.0
            val avgComplexity = if (totalMethods > 0) {
                classes.sumOf { cls -> cls.methods.sumOf { it.complexity } }.toDouble() / totalMethods
            } else 0.0

            val allMethods = classes.flatMap { it.methods }
            val lowComplexityMethods = allMethods.count { it.complexity <= 2 }
            val mediumComplexityMethods = allMethods.count { it.complexity in 3..5 }
            val highComplexityMethods = allMethods.count { it.complexity >= 6 }

            val componentBreakdown = """
**Component Breakdown:**
• Controllers: $controllers (REST endpoints)
• Services: $services (Business logic)
• Repositories: $repositories (Data access)
• Entities: $entities (Data models)
• Other Classes: $others (Utilities, configs, etc.)
            """.trimIndent()

            return ProjectStats(
                    controllers, services, repositories, entities, others,
                    totalMethods, totalFields, avgMethodsPerClass, avgComplexity,
                    lowComplexityMethods, mediumComplexityMethods, highComplexityMethods,
                    componentBreakdown
            )
        }
    }
}