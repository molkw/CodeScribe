// src/main/kotlin/com/example/codescribe/QAManager.kt
// Extended version with Project Mode functionality

package com.example.codescribe

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.nio.charset.StandardCharsets

/**
 * Enhanced Q&A Manager with both Documentation and Project modes
 */
class QAManager(private val project: Project) {
    private val aiService = AIDocumentationService()
    private val logger = Logger.getInstance(QAManager::class.java)

    // Progress callback for UI updates
    var progressCallback: ((String) -> Unit)? = null

    /**
     * Process question in either Documentation or Project mode
     */
    fun processQuestion(
            question: String,
            mode: QueryMode,
            documentationResult: DocumentationResult? = null
    ): String {
        return when (mode) {
            QueryMode.DOCUMENTATION -> processDocumentationQuestion(question, documentationResult)
            QueryMode.PROJECT -> processProjectQuestion(question)
        }
    }

    /**
     * Documentation mode - existing functionality (fast)
     */
    private fun processDocumentationQuestion(question: String, documentationResult: DocumentationResult?): String {
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
     * Project mode - new functionality (detailed)
     */
    private fun processProjectQuestion(question: String): String {
        logger.info("🔍 Processing project question: $question")

        try {
            // Step 1: Extract keywords
            progressCallback?.invoke("📝 Extracting keywords from question...")
            val keywords = extractKeywords(question)
            logger.info("Keywords extracted: $keywords")

            // Step 2: Scan project files
            progressCallback?.invoke("📁 Scanning project files...")
            val relevantFiles = findRelevantFiles(keywords)

            if (relevantFiles.isEmpty()) {
                return "❌ No relevant files found for keywords: ${keywords.joinToString(", ")}\n\n" +
                        "Try asking about specific classes, entities, or configuration topics."
            }

            progressCallback?.invoke("📋 Found ${relevantFiles.size} relevant files...")
            logger.info("Found ${relevantFiles.size} relevant files")

            // Step 3: Read file contents
            progressCallback?.invoke("📖 Reading file contents...")
            val projectContext = buildProjectContext(relevantFiles)

            // Step 4: Send to AI
            progressCallback?.invoke("🤖 Asking AI for detailed analysis...")
            return aiService.askProjectQuestion(question, projectContext)

        } catch (e: Exception) {
            logger.warn("Failed to process project question: $question", e)
            return "❌ Error processing project question: ${e.message}"
        }
    }

    /**
     * Extract keywords using hybrid approach (word splitting + entity extraction)
     */
    private fun extractKeywords(question: String): List<String> {
        val keywords = mutableSetOf<String>()

        // Simple word splitting approach
        val words = question.lowercase()
                .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
                .split("\\s+".toRegex())
                .filter { it.length > 2 && !isStopWord(it) }

        keywords.addAll(words)

        // Entity/noun extraction approach
        val entities = extractEntitiesFromQuestion(question)
        keywords.addAll(entities)

        // Add technical keywords
        val technicalKeywords = extractTechnicalKeywords(question)
        keywords.addAll(technicalKeywords)

        return keywords.toList()
    }

    /**
     * Find files relevant to the extracted keywords
     */
    private fun findRelevantFiles(keywords: List<String>): List<VirtualFile> {
        val baseDir = project.baseDir ?: return emptyList()
        val allFiles = mutableListOf<VirtualFile>()
        val relevantFiles = mutableSetOf<VirtualFile>()

        // Collect all relevant file types
        collectAllProjectFiles(baseDir, allFiles)

        // Score files based on keyword relevance
        allFiles.forEach { file ->
            val relevanceScore = calculateFileRelevance(file, keywords)
            if (relevanceScore > 0) {
                relevantFiles.add(file)
                logger.debug("File ${file.name} scored $relevanceScore for keywords: $keywords")
            }
        }

        return relevantFiles.toList()
    }

    /**
     * Calculate how relevant a file is to the given keywords
     */
    private fun calculateFileRelevance(file: VirtualFile, keywords: List<String>): Int {
        var score = 0
        val fileName = file.name.lowercase()
        val filePath = file.path.lowercase()

        keywords.forEach { keyword ->
            // File name matches
            if (fileName.contains(keyword)) score += 10

            // File path matches
            if (filePath.contains(keyword)) score += 5

            // Content matches (for small config files)
            if (isConfigFile(file)) {
                try {
                    val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8).lowercase()
                    if (content.contains(keyword)) score += 15
                } catch (e: Exception) {
                    logger.debug("Could not read config file ${file.name}: ${e.message}")
                }
            }

            // Java class content matches (basic check)
            if (file.name.endsWith(".java")) {
                try {
                    val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8).lowercase()
                    // Quick scan for keyword in class/method names
                    if (content.contains("class $keyword") ||
                            content.contains("$keyword(") ||
                            content.contains("$keyword ")) {
                        score += 20
                    }
                } catch (e: Exception) {
                    logger.debug("Could not scan Java file ${file.name}: ${e.message}")
                }
            }
        }

        return score
    }

    /**
     * Build context from relevant files to send to AI
     */
    private fun buildProjectContext(files: List<VirtualFile>): String {
        val context = StringBuilder()

        files.forEach { file ->
            try {
                context.append("=== FILE: ${file.path} ===\n")
                val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8)
                context.append(content)
                context.append("\n\n")
            } catch (e: Exception) {
                logger.warn("Could not read file ${file.path}: ${e.message}")
                context.append("ERROR: Could not read file content\n\n")
            }
        }

        return context.toString()
    }

    /**
     * Collect all relevant project files
     */
    private fun collectAllProjectFiles(dir: VirtualFile, files: MutableList<VirtualFile>) {
        try {
            if (dir.isDirectory) {
                // Skip irrelevant directories
                val dirName = dir.name
                if (dirName in listOf("target", "build", ".git", ".idea", "node_modules", "out")) {
                    return
                }

                dir.children?.forEach { child ->
                    collectAllProjectFiles(child, files)
                }
            } else {
                // Include relevant file types
                val fileName = dir.name.lowercase()
                if (fileName.endsWith(".java") ||
                        fileName.endsWith(".properties") ||
                        fileName.endsWith(".yml") ||
                        fileName.endsWith(".yaml") ||
                        fileName == "pom.xml" ||
                        fileName.endsWith(".xml")) {
                    files.add(dir)
                }
            }
        } catch (e: Exception) {
            logger.debug("Error processing directory ${dir.path}: ${e.message}")
        }
    }

    // Helper methods
    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf(
                "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
                "what", "how", "why", "when", "where", "who", "which", "that", "this", "these",
                "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does", "did"
        )
        return word in stopWords
    }

    private fun extractEntitiesFromQuestion(question: String): List<String> {
        val entities = mutableListOf<String>()

        // Look for common Spring Boot entities
        val springPatterns = listOf(
                "controller", "service", "repository", "entity", "component",
                "configuration", "bean", "datasource", "database", "connection",
                "student", "user", "order", "product", "customer" // Common domain entities
        )

        springPatterns.forEach { pattern ->
            if (question.lowercase().contains(pattern)) {
                entities.add(pattern)
            }
        }

        return entities
    }

    private fun extractTechnicalKeywords(question: String): List<String> {
        val technical = mutableListOf<String>()
        val questionLower = question.lowercase()

        // Database keywords
        if (questionLower.containsAny("database", "db", "mysql", "postgresql", "h2", "sql")) {
            technical.addAll(listOf("datasource", "jpa", "hibernate", "database"))
        }

        // Web keywords
        if (questionLower.containsAny("endpoint", "api", "rest", "controller", "mapping")) {
            technical.addAll(listOf("controller", "mapping", "rest"))
        }

        // Data keywords
        if (questionLower.containsAny("create", "save", "update", "delete", "crud")) {
            technical.addAll(listOf("service", "repository", "crud"))
        }

        return technical
    }

    private fun isConfigFile(file: VirtualFile): Boolean {
        val fileName = file.name.lowercase()
        return fileName.endsWith(".properties") ||
                fileName.endsWith(".yml") ||
                fileName.endsWith(".yaml") ||
                fileName == "pom.xml"
    }

    // Extension function
    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it, ignoreCase = true) }
    }

    // ========== EXISTING DOCUMENTATION MODE METHODS ==========

    private fun isSimpleQuery(question: String): Boolean {
        val simplePatterns = listOf(
                "how many controllers", "how many services", "how many repositories", "how many entities", "how many classes",
                "list controllers", "list services", "list repositories", "list entities",
                "show controllers", "show services", "show repositories", "show entities",
                "count controllers", "count services", "count repositories", "count entities",
                "what controllers", "what services", "what repositories", "what entities"
        )

        return simplePatterns.any { pattern ->
            question.contains(pattern) ||
                    question.matches(".*\\b${pattern.replace(" ", "\\s+")}\\b.*".toRegex())
        }
    }

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

    private fun handleAIQuery(question: String, documentationResult: DocumentationResult): String {
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

        val context = selectRelevantContext(question, documentationResult)

        return try {
            aiService.askQuestion(question, context)
        } catch (e: Exception) {
            logger.warn("Failed to get AI response for question: $question", e)
            "❌ Failed to get AI response: ${e.message}"
        }
    }

    private fun selectRelevantContext(question: String, documentationResult: DocumentationResult): String {
        val questionLower = question.lowercase()
        val context = StringBuilder()

        context.append("PROJECT OVERVIEW:\n${documentationResult.overview}\n\n")

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
                context.append("ARCHITECTURE:\n${documentationResult.patterns}\n\n")
                context.append("KEY INSIGHTS:\n${documentationResult.insights}\n\n")
            }
        }

        return context.toString()
    }

    // Helper methods for documentation mode
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
            line.lowercase().contains(keyword.lowercase())
        }
        return if (relevantLines.isNotEmpty()) {
            relevantLines.take(5).joinToString("\n")
        } else {
            "No specific $keyword information found"
        }
    }
}

/**
 * Query modes for the Q&A system
 */
enum class QueryMode {
    DOCUMENTATION,  // Fast mode using documentation
    PROJECT        // Detailed mode using project source code
}