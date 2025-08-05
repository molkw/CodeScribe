// src/main/kotlin/com/example/codescribe/QAManager.kt
// CREATE this new file in your plugin

package com.example.codescribe

import com.intellij.openapi.diagnostic.Logger

/**
 * Manages Q&A functionality with hybrid local/AI processing
 */
class QAManager {
    private val aiService = AIDocumentationService()
    private val logger = Logger.getInstance(QAManager::class.java)

    /**
     * Process a question using hybrid approach (local + AI)
     */
    fun processQuestion(question: String, documentationResult: DocumentationResult?): String {
        if (documentationResult == null) {
            return "❌ No documentation available. Please run analysis first."
        }

        val cleanQuestion = question.trim().lowercase()

        return if (isSimpleQuery(cleanQuestion)) {
            logger.info("🔍 Processing simple query locally: $question")
            handleLocalQuery(cleanQuestion, documentationResult)
        } else {
            logger.info("🤖 Sending complex query to AI: $question")
            handleAIQuery(question, documentationResult)
        }
    }

    /**
     * Determine if this is a simple query that can be handled locally
     */
    private fun isSimpleQuery(question: String): Boolean {
        val simplePatterns = listOf(
                "how many controllers",
                "how many services",
                "how many repositories",
                "how many entities",
                "how many classes",
                "list controllers",
                "list services",
                "list repositories",
                "list entities",
                "show controllers",
                "show services",
                "show repositories",
                "show entities",
                "count controllers",
                "count services",
                "count repositories",
                "count entities",
                "what controllers",
                "what services",
                "what repositories",
                "what entities"
        )

        return simplePatterns.any { pattern ->
            question.contains(pattern) ||
                    question.matches(".*\\b${pattern.replace(" ", "\\s+")}\\b.*".toRegex())
        }
    }

    /**
     * Handle simple queries locally for instant responses
     */
    private fun handleLocalQuery(question: String, documentationResult: DocumentationResult): String {
        val overview = documentationResult.overview
        val classDetails = documentationResult.classDetails

        return when {
            // Count queries
            question.contains("how many controllers") || question.contains("count controllers") -> {
                extractCount(overview, "Controllers") ?: "Controllers information not found in analysis"
            }
            question.contains("how many services") || question.contains("count services") -> {
                extractCount(overview, "Services") ?: "Services information not found in analysis"
            }
            question.contains("how many repositories") || question.contains("count repositories") -> {
                extractCount(overview, "Repositories") ?: "Repositories information not found in analysis"
            }
            question.contains("how many entities") || question.contains("count entities") -> {
                extractCount(overview, "Entities") ?: "Entities information not found in analysis"
            }
            question.contains("how many classes") || question.contains("count classes") -> {
                extractCount(overview, "Classes Analyzed") ?: extractCount(overview, "Total Classes") ?: "Class count not found in analysis"
            }

            // List/Show queries
            question.contains("list controllers") || question.contains("show controllers") || question.contains("what controllers") -> {
                extractControllerInfo(classDetails, overview)
            }
            question.contains("list services") || question.contains("show services") || question.contains("what services") -> {
                extractServiceInfo(classDetails, overview)
            }
            question.contains("list repositories") || question.contains("show repositories") || question.contains("what repositories") -> {
                extractRepositoryInfo(classDetails, overview)
            }
            question.contains("list entities") || question.contains("show entities") || question.contains("what entities") -> {
                extractEntityInfo(classDetails, overview)
            }

            else -> {
                "❓ Simple query not recognized. Try questions like:\n" +
                        "• 'How many controllers do I have?'\n" +
                        "• 'List all services'\n" +
                        "• 'Show repositories'\n" +
                        "• 'Count entities'"
            }
        }
    }

    /**
     * Handle complex queries by sending to AI backend
     */
    private fun handleAIQuery(question: String, documentationResult: DocumentationResult): String {
        // Check if AI service is available
        if (!aiService.isServiceAvailable()) {
            return """
                ❌ AI Backend unavailable
                
                Try simple queries like:
                • "how many controllers"
                • "list services" 
                • "show repositories"
                • "count entities"
            """.trimIndent()
        }

        // Select relevant context based on question keywords
        val context = selectRelevantContext(question, documentationResult)

        return try {
            aiService.askQuestion(question, context)
        } catch (e: Exception) {
            logger.warn("Failed to get AI response for question: $question", e)
            "❌ Failed to get AI response: ${e.message}"
        }
    }

    /**
     * Select relevant documentation context based on question keywords
     */
    private fun selectRelevantContext(question: String, documentationResult: DocumentationResult): String {
        val questionLower = question.lowercase()
        val context = StringBuilder()

        // Always include overview
        context.append("PROJECT OVERVIEW:\n${documentationResult.overview}\n\n")

        // Add specific sections based on question keywords
        when {
            questionLower.containsAny("controller", "endpoint", "rest", "api") -> {
                context.append("COMPONENTS:\n${extractRelevantSection(documentationResult.classDetails, "controller")}\n\n")
            }
            questionLower.containsAny("service", "business", "logic") -> {
                context.append("COMPONENTS:\n${extractRelevantSection(documentationResult.classDetails, "service")}\n\n")
            }
            questionLower.containsAny("repository", "data", "database", "dao") -> {
                context.append("COMPONENTS:\n${extractRelevantSection(documentationResult.classDetails, "repository")}\n\n")
            }
            questionLower.containsAny("entity", "model", "domain") -> {
                context.append("COMPONENTS:\n${extractRelevantSection(documentationResult.classDetails, "entity")}\n\n")
            }
            questionLower.containsAny("architecture", "pattern", "design") -> {
                context.append("ARCHITECTURE:\n${documentationResult.patterns}\n\n")
            }
            questionLower.containsAny("relationship", "dependency", "coupling") -> {
                context.append("RELATIONSHIPS:\n${documentationResult.relationships}\n\n")
            }
            questionLower.containsAny("improve", "suggest", "recommendation", "issue") -> {
                context.append("INSIGHTS:\n${documentationResult.insights}\n\n")
            }
            else -> {
                // For general questions, include key sections
                context.append("ARCHITECTURE:\n${documentationResult.patterns}\n\n")
                context.append("KEY INSIGHTS:\n${documentationResult.insights}\n\n")
            }
        }

        return context.toString()
    }

    // Helper methods for extracting information
    private fun extractCount(text: String, component: String): String? {
        val patterns = listOf(
                "$component: (\\d+)".toRegex(),
                "• $component: (\\d+)".toRegex(),
                "- $component: (\\d+)".toRegex(),
                "$component Analyzed: (\\d+)".toRegex()
        )

        patterns.forEach { pattern ->
            pattern.find(text)?.let { match ->
                val count = match.groupValues[1]
                return "You have $count ${component.lowercase()}"
            }
        }
        return null
    }

    private fun extractControllerInfo(classDetails: String, overview: String): String {
        val count = extractCount(overview, "Controllers")?.substringAfter("have ")?.substringBefore(" ") ?: "0"
        return if (count == "0") {
            "No controllers found in your project"
        } else {
            "You have $count controllers handling REST endpoints and HTTP requests.\n\n" +
                    "Controllers manage the web layer of your application, handling incoming requests and returning responses."
        }
    }

    private fun extractServiceInfo(classDetails: String, overview: String): String {
        val count = extractCount(overview, "Services")?.substringAfter("have ")?.substringBefore(" ") ?: "0"
        return if (count == "0") {
            "No services found in your project"
        } else {
            "You have $count services containing business logic.\n\n" +
                    "Services encapsulate your application's core business functionality and coordinate between controllers and repositories."
        }
    }

    private fun extractRepositoryInfo(classDetails: String, overview: String): String {
        val count = extractCount(overview, "Repositories")?.substringAfter("have ")?.substringBefore(" ") ?: "0"
        return if (count == "0") {
            "No repositories found in your project"
        } else {
            "You have $count repositories handling data access.\n\n" +
                    "Repositories abstract database operations and provide a clean interface for data access."
        }
    }

    private fun extractEntityInfo(classDetails: String, overview: String): String {
        val count = extractCount(overview, "Entities")?.substringAfter("have ")?.substringBefore(" ") ?: "0"
        return if (count == "0") {
            "No entities found in your project"
        } else {
            "You have $count entities representing your domain models.\n\n" +
                    "Entities define the structure of your data and map to database tables."
        }
    }

    private fun extractRelevantSection(text: String, keyword: String): String {
        val lines = text.lines()
        val relevantLines = lines.filter { line ->
            line.lowercase().contains(keyword.lowercase())!!
        }
        return if (relevantLines.isNotEmpty()) {
            relevantLines.take(5).joinToString("\n")
        } else {
            "No specific $keyword information found"
        }
    }

    // Extension function to check if string contains any of the keywords
    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }
}