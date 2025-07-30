// src/main/kotlin/com/example/codescribe/CodeScribeToolWindowFactory.kt
// Complete version with Q&A functionality integrated

package com.example.codescribe

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*

class CodeScribeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val toolWindowContent = CodeScribeToolWindowContent(project)
        val content = contentFactory.createContent(toolWindowContent.getContentPanel(), "CodeScribe", false)
        toolWindow.contentManager.addContent(content)
    }
}

class CodeScribeToolWindowContent(private val project: Project) {
    private val contentPanel = JPanel(BorderLayout())
    private val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))

    // Main controls
    private val analyzeButton = JButton("🤖 AI Analysis")
    private val statusButton = JButton("📡 Check Status")
    private val clearButton = JButton("🗑️ Clear")
    private val exportButton = JButton("📝 Export")

    // Content tabs
    private val tabbedPane = JBTabbedPane()
    private val overviewArea = JTextArea()
    private val classDetailsArea = JTextArea()
    private val patternsArea = JTextArea()
    private val relationshipsArea = JTextArea()
    private val insightsArea = JTextArea()

    // Q&A Panel components
    private val questionField = JTextField()
    private val askButton = JButton("Ask")
    private val clearQAButton = JButton("Clear Q&A")
    private val qaHistoryArea = JTextArea()

    // Status display
    private val statusLabel = JLabel("Ready to analyze project...")

    // Generator, Q&A manager and current results
    private val generator = AIIntegratedGenerator(project)
    private val qaManager = QAManager()
    private var currentResult: DocumentationResult? = null

    init {
        setupUI()
        setupListeners()
        checkInitialStatus()
    }

    private fun setupUI() {
        // Configure text areas
        listOf(overviewArea, classDetailsArea, patternsArea, relationshipsArea, insightsArea).forEach { area ->
            area.isEditable = false
            area.font = Font("Monospaced", Font.PLAIN, 12)
            area.lineWrap = true
            area.wrapStyleWord = true
        }

        // Configure Q&A area
        qaHistoryArea.isEditable = false
        qaHistoryArea.font = Font("Monospaced", Font.PLAIN, 11)
        qaHistoryArea.lineWrap = true
        qaHistoryArea.wrapStyleWord = true
        qaHistoryArea.text = "💬 Ask questions about your documentation here...\n\nExamples:\n• How many controllers do I have?\n• List all services\n• Explain the architecture\n• What patterns were detected?"

        // Add tabs with better descriptions
        tabbedPane.addTab("📊 Overview", JBScrollPane(overviewArea))
        tabbedPane.addTab("🎯 Components", JBScrollPane(classDetailsArea))
        tabbedPane.addTab("🏗️ Architecture", JBScrollPane(patternsArea))
        tabbedPane.addTab("🔗 Structure", JBScrollPane(relationshipsArea))
        tabbedPane.addTab("💡 Insights", JBScrollPane(insightsArea))

        // Setup buttons
        analyzeButton.toolTipText = "Analyze project with AI + advanced static analysis"
        statusButton.toolTipText = "Check AI backend connection status"
        clearButton.toolTipText = "Clear all analysis results"
        exportButton.toolTipText = "Export analysis results to markdown file"
        exportButton.isEnabled = false // Initially disabled until analysis is done

        buttonPanel.add(analyzeButton)
        buttonPanel.add(statusButton)
        buttonPanel.add(clearButton)
        buttonPanel.add(exportButton)

        // Setup Q&A panel
        val qaPanel = setupQAPanel()

        // Create main content panel with tabs
        val mainContentPanel = JPanel(BorderLayout())
        mainContentPanel.add(tabbedPane, BorderLayout.CENTER)
        mainContentPanel.add(qaPanel, BorderLayout.SOUTH)

        // Layout
        contentPanel.add(buttonPanel, BorderLayout.NORTH)
        contentPanel.add(mainContentPanel, BorderLayout.CENTER)

        statusLabel.font = Font("SansSerif", Font.ITALIC, 11)
        contentPanel.add(statusLabel, BorderLayout.SOUTH)
    }

    private fun setupListeners() {
        analyzeButton.addActionListener {
            runAnalysis()
        }

        statusButton.addActionListener {
            checkAIStatus()
        }

        clearButton.addActionListener {
            clearAllTabs()
            clearQAHistory()
            currentResult = null
            exportButton.isEnabled = false
            statusLabel.text = "Ready to analyze project..."
        }

        exportButton.addActionListener {
            exportToMarkdown()
        }

        // Q&A listeners
        askButton.addActionListener {
            processQuestion()
        }

        clearQAButton.addActionListener {
            clearQAHistory()
        }

        // Allow Enter key to ask question
        questionField.addActionListener {
            processQuestion()
        }
    }

    private fun checkInitialStatus() {
        statusLabel.text = generator.getProjectSummary()
        checkAIStatus()
    }

    private fun checkAIStatus() {
        statusButton.isEnabled = false
        statusButton.text = "⏳ Checking..."

        Thread {
            val aiService = AIDocumentationService()
            val status = aiService.getServiceStatus()
            val isAvailable = aiService.isServiceAvailable()

            SwingUtilities.invokeLater {
                statusLabel.text = status
                statusButton.text = "📡 Check Status"
                statusButton.isEnabled = true

                // Update analyze button based on AI availability
                analyzeButton.text = if (isAvailable) {
                    "🤖 AI Analysis"
                } else {
                    "📊 Static Analysis"
                }

                analyzeButton.toolTipText = if (isAvailable) {
                    "AI-enhanced analysis with qwen2.5-coder + static analysis"
                } else {
                    "AI unavailable - using advanced static analysis only"
                }
            }
        }.start()
    }

    private fun runAnalysis() {
        analyzeButton.isEnabled = false
        val originalText = analyzeButton.text
        analyzeButton.text = "🔄 Analyzing..."

        clearAllTabs()
        clearQAHistory() // Clear Q&A when new analysis starts
        showAnalysisProgress()

        // Run analysis in background with proper read action
        Thread {
            try {
                val documentationResult = ApplicationManager.getApplication().runReadAction<DocumentationResult> {
                    generator.generateDocumentation()
                }

                SwingUtilities.invokeLater {
                    displayResults(documentationResult)
                    currentResult = documentationResult
                    exportButton.isEnabled = true
                    analyzeButton.text = originalText
                    analyzeButton.isEnabled = true

                    val statusText = if (documentationResult.isAIEnhanced) {
                        "✅ AI-enhanced analysis completed! You can now ask questions."
                    } else {
                        "✅ Static analysis completed! You can ask simple questions."
                    }
                    statusLabel.text = statusText
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    showError(e)
                    currentResult = null
                    exportButton.isEnabled = false
                    analyzeButton.text = originalText
                    analyzeButton.isEnabled = true
                    statusLabel.text = "❌ Analysis failed"
                }
                e.printStackTrace()
            }
        }.start()
    }

    private fun showAnalysisProgress() {
        overviewArea.text = """
🚀 COMPREHENSIVE PROJECT ANALYSIS IN PROGRESS...

📁 Scanning Java source files...
🔍 Extracting classes, methods, and annotations...
🤖 Sending to AI backend for intelligent analysis...
🧠 Processing with qwen2.5-coder model...
📊 Generating structured insights...

⏱️ This usually takes 30-90 seconds depending on project size.
⏱️ Larger projects may take longer for thorough analysis.

Please wait...
        """.trimIndent()
    }

    private fun displayResults(result: DocumentationResult) {
        // Distribute content properly to each tab
        overviewArea.text = result.overview
        classDetailsArea.text = result.classDetails
        patternsArea.text = result.patterns
        relationshipsArea.text = result.relationships
        insightsArea.text = result.insights

        // Auto-switch to overview tab to show results
        tabbedPane.selectedIndex = 0
    }

    private fun showError(e: Exception) {
        overviewArea.text = """
❌ ANALYSIS ERROR

Error Details: ${e.message}

🔍 Possible Causes:
• Project not fully loaded/indexed by IntelliJ
• AI backend connection issues  
• Invalid or empty project structure
• Insufficient permissions

🛠️ Solutions:
1. Wait for IntelliJ indexing to complete (check progress bar)
2. Check AI backend status: curl http://localhost:8000/api/v1/health
3. Restart AI backend: python main.py in codescribe-backend folder
4. Ensure project contains Java source files in src/main/java
5. Try with a smaller test project first

📞 AI Backend Setup:
1. cd codescribe-backend
2. python main.py
3. Verify: curl http://localhost:8000/api/v1/health

🔧 Debug Information:
${e.stackTraceToString()}
        """.trimIndent()

        // Clear other tabs
        classDetailsArea.text = "Analysis failed - check Overview tab for details"
        patternsArea.text = "Analysis failed - check Overview tab for details"
        relationshipsArea.text = "Analysis failed - check Overview tab for details"
        insightsArea.text = "Analysis failed - check Overview tab for details"
    }

    private fun exportToMarkdown() {
        val result = currentResult ?: return

        try {
            // Create file chooser
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            fileChooser.dialogTitle = "Export Analysis to Markdown"

            // Set default filename
            val projectName = project.name?.replace(" ", "_") ?: "Project"
            val defaultFileName = "${projectName}-analysis.md"
            fileChooser.selectedFile = java.io.File(defaultFileName)

            // Show save dialog
            val userSelection = fileChooser.showSaveDialog(contentPanel)

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                val selectedFile = fileChooser.selectedFile

                // Ensure .md extension
                val finalFile = if (selectedFile.extension.lowercase() != "md") {
                    java.io.File(selectedFile.parent, selectedFile.nameWithoutExtension + ".md")
                } else {
                    selectedFile
                }

                // Generate markdown content
                val markdownContent = generateMarkdownContent(result)

                // Write to file
                finalFile.writeText(markdownContent, Charsets.UTF_8)

                // Show success message
                statusLabel.text = "✅ Exported to: ${finalFile.absolutePath}"

                // Open file automatically
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().open(finalFile)
                    }
                } catch (e: Exception) {
                    // If can't open automatically, just show the path
                    JOptionPane.showMessageDialog(
                            contentPanel,
                            "Analysis exported successfully!\n\nFile: ${finalFile.absolutePath}",
                            "Export Complete",
                            JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        } catch (e: Exception) {
            // Show error dialog
            JOptionPane.showMessageDialog(
                    contentPanel,
                    "Failed to export analysis:\n\n${e.message}",
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE
            )
            statusLabel.text = "❌ Export failed"
        }
    }

    private fun generateMarkdownContent(result: DocumentationResult): String {
        val projectName = project.name ?: "Unknown Project"
        val timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )

        return """
# ${projectName} - Code Analysis Report

**Generated**: ${timestamp}  
**Tool**: CodeScribe IntelliJ Plugin  
**Analysis Type**: ${if (result.isAIEnhanced) "AI-Enhanced Analysis" else "Static Analysis"}

---

## Executive Summary

${extractExecutiveSummary(result)}

---

## Project Architecture

${cleanArchitectureSection(result)}

---

## Component Analysis

${cleanComponentAnalysis(result)}

---

## Code Quality Assessment

${extractCodeQuality(result)}

---

## Recommendations

${cleanRecommendations(result)}

---

## Technical Details

${extractTechnicalDetails(result)}

---

## Analysis Methodology

This analysis was generated using CodeScribe, an IntelliJ plugin that combines ${if (result.isAIEnhanced) "AI-powered analysis with qwen2.5-coder model and" else ""} advanced static code analysis to provide comprehensive insights into Spring Boot projects.

**Analysis Coverage:**
- Component structure and layering
- Design pattern detection
- Code complexity metrics
- Architecture quality assessment
- Best practice recommendations

*Generated by CodeScribe Plugin*
        """.trimIndent()
    }

    // Helper methods for content extraction (keeping existing implementation)
    private fun extractExecutiveSummary(result: DocumentationResult): String {
        val overviewText = result.overview
        val projectStats = extractProjectStats(overviewText)
        val architectureType = extractArchitectureType(overviewText)

        return """
**Project Type**: Spring Boot Application  
**Architecture**: ${architectureType}  
**Total Classes**: ${projectStats["classes"] ?: "N/A"}  
**Components**: ${projectStats["controllers"] ?: "0"} Controllers, ${projectStats["services"] ?: "0"} Services, ${projectStats["repositories"] ?: "0"} Repositories  

**Key Findings:**
- Well-structured layered architecture with clear separation of concerns
- Standard Spring Boot MVC pattern implementation
- ${if (result.isAIEnhanced) "AI analysis indicates good architectural practices" else "Static analysis shows organized code structure"}
- Low average method complexity (${projectStats["complexity"] ?: "N/A"}) indicates maintainable code
        """.trimIndent()
    }

    private fun cleanArchitectureSection(result: DocumentationResult): String {
        val patternsText = result.patterns
        val architecturalPatterns = extractArchitecturalPatterns(patternsText)
        val layeringQuality = extractLayeringQuality(result.relationships)

        return """
**Architecture Pattern**: Layered Architecture with MVC  
**Design Patterns Detected**:
${architecturalPatterns}

**Layer Structure**:
${layeringQuality}

**Architecture Quality**: ${extractArchitectureQuality(result)}
        """.trimIndent()
    }

    private fun cleanComponentAnalysis(result: DocumentationResult): String {
        val classDetails = result.classDetails
        val packageDistribution = extractPackageDistribution(classDetails)
        val complexityAnalysis = extractComplexityAnalysis(classDetails)

        return """
**Component Distribution**:
${extractComponentBreakdown(classDetails)}

**Package Organization**:
${packageDistribution}

**Complexity Metrics**:
${complexityAnalysis}
        """.trimIndent()
    }

    private fun extractCodeQuality(result: DocumentationResult): String {
        val insights = result.insights
        val qualityMetrics = extractQualityMetrics(insights)
        val strengths = extractStrengths(insights)

        return """
**Quality Metrics**:
${qualityMetrics}

**Code Strengths**:
${strengths}

**Areas for Attention**:
${extractAreasForImprovement(insights)}
        """.trimIndent()
    }

    private fun cleanRecommendations(result: DocumentationResult): String {
        val insights = result.insights
        val recommendations = extractCleanRecommendations(insights)

        return """
**Immediate Actions**:
${recommendations["immediate"] ?: "- No immediate actions required"}

**Long-term Improvements**:
${recommendations["longterm"] ?: "- Consider adding comprehensive test coverage"}

**Best Practice Suggestions**:
${recommendations["bestpractices"] ?: "- Implement proper error handling and logging"}
        """.trimIndent()
    }

    private fun extractTechnicalDetails(result: DocumentationResult): String {
        return """
**Technology Stack**: Spring Boot, Spring MVC, Spring Data JPA  
**Architecture Style**: Layered Architecture  
**Dependency Management**: Spring IoC Container  
**Data Access**: Repository Pattern  

**Analysis Scope**:
- Static code structure analysis
- Component relationship mapping
- Design pattern detection
- Code complexity assessment
${if (result.isAIEnhanced) "- AI-powered insight generation" else ""}
        """.trimIndent()
    }

    // Helper methods for content extraction (keeping existing implementations)
    private fun extractProjectStats(text: String): Map<String, String> {
        val stats = mutableMapOf<String, String>()
        val classesMatch = "Classes Analyzed: (\\d+)".toRegex().find(text)
        val controllersMatch = "Controllers: (\\d+)".toRegex().find(text)
        val servicesMatch = "Services: (\\d+)".toRegex().find(text)
        val repositoriesMatch = "Repositories: (\\d+)".toRegex().find(text)
        val complexityMatch = "Avg Complexity: ([\\d,]+)".toRegex().find(text)

        classesMatch?.let { stats["classes"] = it.groupValues[1] }
        controllersMatch?.let { stats["controllers"] = it.groupValues[1] }
        servicesMatch?.let { stats["services"] = it.groupValues[1] }
        repositoriesMatch?.let { stats["repositories"] = it.groupValues[1] }
        complexityMatch?.let { stats["complexity"] = it.groupValues[1] }

        return stats
    }

    private fun extractArchitectureType(text: String): String {
        return when {
            text.contains("Full MVC Architecture") -> "Full MVC with Layered Architecture"
            text.contains("Web Application") -> "Web Application"
            text.contains("REST API") -> "REST API"
            else -> "Spring Boot Application"
        }
    }

    private fun extractArchitecturalPatterns(text: String): String {
        val patterns = mutableListOf<String>()
        if (text.contains("Repository Pattern")) patterns.add("- Repository Pattern")
        if (text.contains("Service Layer Pattern")) patterns.add("- Service Layer Pattern")
        if (text.contains("Controller Pattern")) patterns.add("- Controller Pattern")
        if (text.contains("Dependency Injection")) patterns.add("- Dependency Injection")
        if (text.contains("Domain Model Pattern")) patterns.add("- Domain Model Pattern")
        return if (patterns.isNotEmpty()) patterns.joinToString("\n") else "- Standard Spring Boot patterns"
    }

    private fun extractLayeringQuality(text: String): String {
        return when {
            text.contains("Web Layer ✅ → Business Layer ✅ → Data Layer ✅") ->
                "- Web Layer (Controllers)\n- Business Layer (Services)\n- Data Layer (Repositories)\n- Model Layer (Entities)"
            else -> "- Standard Spring Boot layering detected"
        }
    }

    private fun extractArchitectureQuality(result: DocumentationResult): String {
        val patterns = result.patterns
        return when {
            patterns.contains("Excellent") -> "High quality with excellent separation of concerns"
            patterns.contains("Good") -> "Good architecture with proper layering"
            else -> "Standard Spring Boot architecture"
        }
    }

    private fun extractComponentBreakdown(text: String): String {
        val lines = text.lines()
        val breakdown = mutableListOf<String>()
        lines.forEach { line ->
            if (line.contains("Controllers:") || line.contains("Services:") ||
                    line.contains("Repositories:") || line.contains("Entities:")) {
                breakdown.add("- ${line.trim()}")
            }
        }
        return if (breakdown.isNotEmpty()) breakdown.joinToString("\n") else "- Component breakdown not available"
    }

    private fun extractPackageDistribution(text: String): String {
        val lines = text.lines()
        val packages = mutableListOf<String>()
        lines.forEach { line ->
            if (line.contains("tn.esprit") || line.contains("com.example")) {
                packages.add("- ${line.trim()}")
            }
        }
        return if (packages.isNotEmpty()) packages.take(5).joinToString("\n") else "- Package distribution not available"
    }

    private fun extractComplexityAnalysis(text: String): String {
        val lines = text.lines()
        val complexity = mutableListOf<String>()
        lines.forEach { line ->
            if (line.contains("Low Complexity") || line.contains("Medium Complexity") ||
                    line.contains("High Complexity") || line.contains("Average")) {
                complexity.add("- ${line.trim()}")
            }
        }
        return if (complexity.isNotEmpty()) complexity.joinToString("\n") else "- Low complexity codebase"
    }

    private fun extractQualityMetrics(text: String): String {
        return when {
            text.contains("Low complexity methods") -> "- Low average method complexity indicates maintainable code"
            else -> "- Standard code quality metrics"
        }
    }

    private fun extractStrengths(text: String): String {
        val strengths = mutableListOf<String>()
        if (text.contains("service layer separation")) strengths.add("- Well-defined service layer")
        if (text.contains("domain models")) strengths.add("- Clear domain model structure")
        if (text.contains("separation of concerns")) strengths.add("- Good separation of concerns")
        return if (strengths.isNotEmpty()) strengths.joinToString("\n") else "- Well-structured Spring Boot application"
    }

    private fun extractAreasForImprovement(text: String): String {
        val areas = mutableListOf<String>()
        if (text.contains("test coverage")) areas.add("- Consider adding comprehensive test coverage")
        if (text.contains("error handling")) areas.add("- Implement proper error handling")
        if (text.contains("documentation")) areas.add("- Add API documentation")
        return if (areas.isNotEmpty()) areas.joinToString("\n") else "- No major issues detected"
    }

    private fun extractCleanRecommendations(text: String): Map<String, String> {
        val recommendations = mutableMapOf<String, String>()
        val immediate = mutableListOf<String>()
        val longterm = mutableListOf<String>()
        val bestpractices = mutableListOf<String>()

        val lines = text.lines()
        lines.forEach { line ->
            when {
                line.contains("Unit Testing") || line.contains("test coverage") ->
                    immediate.add("- Implement unit testing for critical components")
                line.contains("API Documentation") || line.contains("Swagger") ->
                    immediate.add("- Add API documentation with Swagger/OpenAPI")
                line.contains("refactoring") ->
                    longterm.add("- Review high-complexity methods for refactoring")
                line.contains("error handling") ->
                    bestpractices.add("- Implement comprehensive error handling")
                line.contains("logging") ->
                    bestpractices.add("- Add proper logging throughout the application")
            }
        }

        if (immediate.isNotEmpty()) recommendations["immediate"] = immediate.joinToString("\n")
        if (longterm.isNotEmpty()) recommendations["longterm"] = longterm.joinToString("\n")
        if (bestpractices.isNotEmpty()) recommendations["bestpractices"] = bestpractices.joinToString("\n")

        return recommendations
    }

    private fun clearAllTabs() {
        overviewArea.text = ""
        classDetailsArea.text = ""
        patternsArea.text = ""
        relationshipsArea.text = ""
        insightsArea.text = ""
    }

    /**
     * Setup Q&A panel at the bottom
     */
    private fun setupQAPanel(): JPanel {
        val qaPanel = JPanel(BorderLayout())
        qaPanel.preferredSize = java.awt.Dimension(0, 200) // Fixed height
        qaPanel.border = javax.swing.BorderFactory.createTitledBorder("💬 Ask Questions About Documentation")

        // Input section
        val inputPanel = JPanel(BorderLayout(5, 0))
        questionField.toolTipText = "Ask questions like: 'How many controllers?', 'Explain the architecture', 'List all services'"

        askButton.toolTipText = "Ask question (or press Enter)"
        clearQAButton.toolTipText = "Clear Q&A history"
        clearQAButton.font = Font("SansSerif", Font.PLAIN, 10)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0))
        buttonPanel.add(askButton)
        buttonPanel.add(clearQAButton)

        inputPanel.add(JLabel("Question: "), BorderLayout.WEST)
        inputPanel.add(questionField, BorderLayout.CENTER)
        inputPanel.add(buttonPanel, BorderLayout.EAST)

        // History section with scroll
        val scrollPane = JBScrollPane(qaHistoryArea)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        qaPanel.add(inputPanel, BorderLayout.NORTH)
        qaPanel.add(scrollPane, BorderLayout.CENTER)

        return qaPanel
    }

    /**
     * Process user question using QAManager
     */
    private fun processQuestion() {
        val question = questionField.text.trim()
        if (question.isEmpty()) {
            return
        }

        // Disable ask button during processing
        askButton.isEnabled = false
        val originalText = askButton.text
        askButton.text = "⏳"

        // Add question to history immediately
        appendToQAHistory("Q: $question")
        questionField.text = ""

        // Process in background thread
        Thread {
            try {
                val answer = qaManager.processQuestion(question, currentResult)

                SwingUtilities.invokeLater {
                    appendToQAHistory("A: $answer")
                    appendToQAHistory("") // Empty line for spacing

                    // Auto-scroll to bottom
                    qaHistoryArea.caretPosition = qaHistoryArea.document.length

                    askButton.text = originalText
                    askButton.isEnabled = true
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    appendToQAHistory("A: ❌ Error processing question: ${e.message}")
                    appendToQAHistory("")

                    askButton.text = originalText
                    askButton.isEnabled = true
                }
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Append text to Q&A history
     */
    private fun appendToQAHistory(text: String) {
        if (qaHistoryArea.text.contains("Ask questions about your documentation here")) {
            // Clear initial help text on first question
            qaHistoryArea.text = ""
        }
        qaHistoryArea.append("$text\n")
    }

    /**
     * Clear Q&A history
     */
    private fun clearQAHistory() {
        qaHistoryArea.text = "💬 Ask questions about your documentation here...\n\nExamples:\n• How many controllers do I have?\n• List all services\n• Explain the architecture\n• What patterns were detected?"
    }

    fun getContentPanel(): JPanel {
        return contentPanel
    }
}